package com.aruno.foolquest.widget;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

/** Bounded, read-only request. The connection is always disconnected by the caller. */
final class RevenueHttp {
    static final int CONNECT_MS=10000, READ_MS=10000, BODY_MS=10000, MAX_BYTES=32768;
    static final class HttpFailure extends IOException {
        final int status;
        HttpFailure(int status){super("HTTP response");this.status=status;}
    }
    static final class InvalidResponse extends IOException {}
    static final class Offline extends IOException {}
    static String read(HttpURLConnection con)throws IOException {
        con.setConnectTimeout(CONNECT_MS);con.setReadTimeout(READ_MS);con.setInstanceFollowRedirects(false);
        con.setRequestMethod("GET");con.setRequestProperty("Accept","application/json");con.setRequestProperty("Cache-Control","no-cache");
        int status=con.getResponseCode();if(status!=200)throw new HttpFailure(status);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        long deadline=System.nanoTime()+BODY_MS*1000000L;
        try(InputStream in=con.getInputStream()) {
            byte[] buffer=new byte[2048];int n;
            while((n=in.read(buffer))!=-1) {
                if(System.nanoTime()>deadline)throw new SocketTimeoutException("Response deadline");
                if(bytes.size()+n>MAX_BYTES)throw new InvalidResponse();
                bytes.write(buffer,0,n);
            }
        }
        if(System.nanoTime()>deadline)throw new SocketTimeoutException("Response deadline");
        return bytes.toString("UTF-8");
    }
}
