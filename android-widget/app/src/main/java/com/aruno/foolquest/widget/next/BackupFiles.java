package com.aruno.foolquest.widget.next;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Append-only SAF writes. Existing good documents are never opened for writing. */
final class BackupFiles {
    final Context context;final Uri tree;
    BackupFiles(Context c,Uri t){context=c;tree=t;}
    Uri root(){return DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));}
    Map<String,Uri> documents()throws Exception {
        Map<String,Uri> out=new TreeMap<>();
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        try(Cursor cur=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
            if(cur==null)throw new IOException("保存先の一覧を取得できません");
            while(cur.moveToNext())out.put(cur.getString(1),DocumentsContract.buildDocumentUriUsingTree(tree,cur.getString(0)));
        }return out;
    }
    static String read(Context c,Uri uri)throws Exception {
        try(InputStream in=c.getContentResolver().openInputStream(uri)){
            if(in==null)throw new IOException("バックアップを開けません");
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1){if(out.size()+n>BackupFormat.LIMIT)throw new IOException("バックアップが大きすぎます");out.write(buf,0,n);}
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(out.toByteArray())).toString();
        }
    }
    Uri append(String name,String json)throws Exception {
        BackupFormat.parse(json);
        Uri document=DocumentsContract.createDocument(context.getContentResolver(),root(),"application/octet-stream",name+".pending");
        if(document==null)throw new IOException("保存先にファイルを作成できません");
        try {
            ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(document,"w");
            if(fd==null)throw new IOException("保存先を書き込めません");
            try(ParcelFileDescriptor.AutoCloseOutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(fd)){
                out.write(json.getBytes(StandardCharsets.UTF_8));out.flush();out.getFD().sync();
            }
            if(!json.equals(read(context,document)))throw new IOException("保存後の再読込が一致しません");
            Uri complete=DocumentsContract.renameDocument(context.getContentResolver(),document,name);
            if(complete==null)throw new IOException("保存先が安全なファイル確定に対応していません");
            if(!json.equals(read(context,complete)))throw new IOException("確定後の再読込が一致しません");
            BackupFormat.parse(read(context,complete));return complete;
        }catch(Exception e){
            // A .pending file is never offered as a healthy generation; retain it for diagnosis.
            throw e;
        }
    }
    void prune()throws Exception {
        Map<String,Uri> docs=documents();Map<String,List<String>> groups=new HashMap<>();
        for(String name:docs.keySet())if(name.matches("aruno-result-(change|daily|safety)-[0-9]+-[a-f0-9-]+\\.json")){
            try{String k=BackupFormat.parse(read(context,docs.get(name))).getString("kind");groups.computeIfAbsent(k,x->new ArrayList<>()).add(name);}catch(Exception corrupt){/* corrupt files never count toward good retention */}
        }
        for(List<String> names:groups.values()){
            Collections.sort(names,Collections.reverseOrder());
            for(int i=10;i<names.size();i++)if(!DocumentsContract.deleteDocument(context.getContentResolver(),docs.get(names.get(i))))throw new IOException("古い世代を整理できません");
        }
    }
}
