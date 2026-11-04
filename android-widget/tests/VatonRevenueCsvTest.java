package com.aruno.foolquest.widget.next;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class VatonRevenueCsvTest {
    private static final String HEADER="日付,種別,ブランド名,商品名,交換ポイント,円換算額,チャージポイント\n";
    private static int checks;

    private static void check(boolean condition,String description){
        checks++;
        if(!condition)throw new AssertionError(description);
    }

    private static void reject(String text){
        reject(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void reject(byte[] bytes){
        boolean rejected=false;
        try{VatonRevenueCsv.parseCsv(bytes);}
        catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"invalid import was accepted");
    }

    public static void main(String[] args) throws Exception {
        String fixture=HEADER+"2026/11/01,チャージ,,,0,0,100\n"
            +"2026/11/01,チャージ,,,0,0,200\n"
            +"2026/11/02,交換,例,\"商品, サンプル\",-5000,5000,0\n";
        VatonRevenueCsv.CsvResult utf=VatonRevenueCsv.parseCsv(
            ("\uFEFF注意書き\r\n"+fixture.replace("\n","\r\n")).getBytes(StandardCharsets.UTF_8));
        check(utf.datesComplete&&utf.datesMonth.equals("2026-11")&&utf.count==3,"dated BOM CSV");
        check(utf.total==300&&utf.days.get("2026-11-01")==300&&utf.days.get("2026-11-02")==0
            &&!utf.days.containsKey("2026-11-03"),"charge rows, exchange-only zero, missing day");
        VatonRevenueCsv.CsvResult sjis=VatonRevenueCsv.parseCsv(fixture.getBytes(Charset.forName("MS932")));
        check(sjis.days.equals(utf.days)&&sjis.total==utf.total,"MS932 equivalence");
        VatonRevenueCsv.CsvResult legacy=VatonRevenueCsv.parseCsv("ID,チャージポイント\nA,\"1,000\"\nB,-200\n");
        check(!legacy.datesComplete&&legacy.total==1200,"legacy undated month-only behavior");
        for(String amount:new String[]{"broken","","-1","1.5","1\"0\"","\"1,23\"","9007199254740992"}){
            reject(HEADER+"2026/11/01,チャージ,,,0,0,"+amount+"\n");
        }
        reject(fixture+"2026/11/03,チャージ,,,0,0\n");
        reject(fixture+"2026/11/03,交換,,,0,0,100\n");
        reject(fixture+"2026/11/32,チャージ,,,0,0,100\n");
        reject(fixture+"2026/11/03 bad,チャージ,,,0,0,100\n");
        reject(fixture+"2026/12/01,チャージ,,,0,0,100\n");
        reject(HEADER);
        reject("種別,チャージポイント\nチャージ,200\n");
        reject(new byte[]{(byte)0xef,(byte)0xbb,(byte)0xbf,(byte)0xff});
        if(args.length==1){
            VatonRevenueCsv.CsvResult original=VatonRevenueCsv.parseCsv(Files.readAllBytes(Paths.get(args[0])));
            check(original.datesComplete&&original.count==26&&original.total==2200,"original file aggregate");
            check(original.days.size()==4&&original.days.get("2026-11-01")==200
                &&original.days.get("2026-11-02")==300&&original.days.get("2026-11-03")==1700
                &&original.days.get("2026-11-04")==0,"original file daily amounts");
        }
        System.out.println("PASS "+checks+" Vaton CSV checks");
    }
}
