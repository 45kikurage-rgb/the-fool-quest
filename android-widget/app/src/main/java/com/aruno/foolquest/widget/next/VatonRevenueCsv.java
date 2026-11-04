package com.aruno.foolquest.widget.next;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Parser shared by the existing file-picker entry point and offline tests. */
final class VatonRevenueCsv {
    private VatonRevenueCsv() {}

    static final class CsvResult {
        final long total;
        final int count;
        final java.util.Map<String,Long> days;
        final boolean datesComplete;
        final String datesMonth;
        CsvResult(long total,int count,java.util.Map<String,Long> days,boolean datesComplete,String datesMonth){
            this.total=total;
            this.count=count;
            this.days=java.util.Collections.unmodifiableMap(days);
            this.datesComplete=datesComplete;
            this.datesMonth=datesMonth;
        }
    }
    static String suggestAcquiredDate(CsvResult csv,long modified,long now){
        if(!csv.datesComplete||modified<=0||modified>now)return "";
        String candidate=RevenueMath.date("yyyy-MM-dd",modified);
        for(String day:csv.days.keySet())if(day.compareTo(candidate)>0)return "";
        return candidate;
    }

    static CsvResult parseCsv(byte[] bytes) {
        String decoded;
        try {
            decoded = decode(bytes, StandardCharsets.UTF_8);
        } catch (CharacterCodingException utf8Error) {
            if (bytes.length >= 3 && bytes[0] == (byte) 0xef &&
                    bytes[1] == (byte) 0xbb && bytes[2] == (byte) 0xbf) {
                throw new IllegalArgumentException("UTF-8のCSVを読み取れません");
            }
            try {
                decoded = decode(bytes, Charset.forName("MS932"));
            } catch (CharacterCodingException ms932Error) {
                throw new IllegalArgumentException("CSVの文字コードを読み取れません");
            }
        }
        return parseCsv(decoded);
    }

    private static String decode(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static long plus(long a, long b) {
        if (b > RevenueMath.MAX_YEN - a) throw new IllegalArgumentException("チャージ金額が大きすぎます");
        return a + b;
    }

    static CsvResult parseCsv(String text) {
        if(text.length()>4_000_000)throw new IllegalArgumentException("CSVが大きすぎます");
        List<List<String>> rows=new ArrayList<>();
        List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();
        boolean quoted=false;
        for(int i=0;i<text.length();i++) {
            char ch=text.charAt(i);
            if(i==0&&ch=='\uFEFF')continue;
            if(ch=='"') {
                if(quoted && i+1<text.length()&&text.charAt(i+1)=='"'){
                    cell.append('"');
                    i++;
                }else if(quoted){
                    quoted=false;
                    if(i+1<text.length()&&",\r\n".indexOf(text.charAt(i+1))<0)
                        throw new IllegalArgumentException("CSVの引用符の後に区切りがありません");
                }else{
                    if(cell.length()>0)throw new IllegalArgumentException("CSVの引用符が不正です");
                    quoted=true;
                }
            }else if(!quoted&&(ch==','||ch=='\n'||ch=='\r')){
                row.add(cell.toString());
                cell.setLength(0);
                if(ch!=','){
                    rows.add(row);
                    row=new ArrayList<>();
                    if(ch=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;
                }
            }else cell.append(ch);
        }
        if(quoted)throw new IllegalArgumentException("CSVの引用符が閉じていません");
        if(cell.length()>0||!row.isEmpty()){row.add(cell.toString());rows.add(row);}
        int head=-1,index=-1,dateIndex=-1,typeIndex=-1;
        for(int i=0;i<rows.size();i++){
            List<String> header=rows.get(i);
            for(int j=0;j<header.size();j++){
                if("チャージポイント".equals(header.get(j).trim())){
                    if(index>=0)throw new IllegalArgumentException("CSVのチャージポイント列が重複しています");
                    head=i;
                    index=j;
                }
            }
            if(head>=0){
                for(int j=0;j<header.size();j++){
                    String column=header.get(j).trim();
                    if("日付".equals(column)){
                        if(dateIndex>=0)throw new IllegalArgumentException("CSVの日付列が重複しています");
                        dateIndex=j;
                    }
                    if("種別".equals(column)){
                        if(typeIndex>=0)throw new IllegalArgumentException("CSVの種別列が重複しています");
                        typeIndex=j;
                    }
                }
                if(typeIndex>=0&&dateIndex<0)
                    throw new IllegalArgumentException("「日付」列が見つかりません");
                break;
            }
        }
        if(head<0)throw new IllegalArgumentException("「チャージポイント」列が見つかりません");
        long total=0;int count=0;String month="";
        java.util.Map<String,Long> days=new java.util.TreeMap<>();
        for(int i=head+1;i<rows.size();i++){
            List<String> record=rows.get(i);
            if(record.size()==1&&record.get(0).trim().isEmpty())continue;
            if(record.size()!=rows.get(head).size())
                throw new IllegalArgumentException("CSVの列数が一致しません（"+(i+1)+"行目）");
            String raw=record.get(index).trim();
            String type=typeIndex<0?"":record.get(typeIndex).trim();
            if(typeIndex>=0&&!"チャージ".equals(type)&&!"交換".equals(type))
                throw new IllegalArgumentException("CSVの種別が不正です（"+(i+1)+"行目）");
            if("交換".equals(type)&&raw.isEmpty())raw="0";
            String sign=dateIndex<0?"-?":"";
            if(!raw.matches(sign+"(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)"))
                throw new IllegalArgumentException("チャージ金額が不正です（"+(i+1)+"行目）");
            long amount;
            try{
                amount=Long.parseLong(raw.replace(",",""));
            }catch(NumberFormatException e){
                throw new IllegalArgumentException("チャージ金額が大きすぎます");
            }
            if(amount==Long.MIN_VALUE||("交換".equals(type)&&amount!=0))
                throw new IllegalArgumentException("チャージ金額が不正です（"+(i+1)+"行目）");
            long positive=Math.abs(amount);
            total=plus(total,positive);
            count++;
            if(dateIndex<0)continue;
            String normalized=CalendarDateMath.normalizeCsvDate(record.get(dateIndex));
            if(normalized==null)
                throw new IllegalArgumentException("CSVの日付が不正です（"+(i+1)+"行目）");
            String rowMonth=normalized.substring(0,7);
            if(!month.isEmpty()&&!month.equals(rowMonth))
                throw new IllegalArgumentException("CSVに複数月の日付があります。月別CSVを選択してください");
            month=rowMonth;
            days.put(normalized,plus(days.containsKey(normalized)?days.get(normalized):0L,positive));
        }
        if(count==0)throw new IllegalArgumentException("CSVに明細がありません");
        return new CsvResult(total,count,days,dateIndex>=0,month);
    }
}
