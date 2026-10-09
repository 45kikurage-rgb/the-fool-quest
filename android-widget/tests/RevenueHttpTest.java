package com.aruno.foolquest.widget;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLException;

/** Real loopback timing and deterministic HTTP failures, with no production writes. */
public final class RevenueHttpTest {
    static int checks;
    static void ok(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static final class Response extends HttpURLConnection {
        final int status;final byte[] body;boolean closed;
        Response(int status,byte[] body)throws Exception{super(new URL("https://example.invalid/reader"));this.status=status;this.body=body;}
        public int getResponseCode(){return status;}
        public InputStream getInputStream(){return new ByteArrayInputStream(body){public void close()throws IOException{closed=true;super.close();}};}
        public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}
    }
    public static void main(String[] args)throws Exception {
        Response good=new Response(200,"{\"month\":\"2026-10\",\"coupon_revenue\":123}".getBytes(StandardCharsets.UTF_8));
        ok(RevenueHttp.read(good).contains("123"),"valid payload retained");
        ok(good.getConnectTimeout()==10000&&good.getReadTimeout()==10000,"bounded longer network timeouts");
        ok(!good.getInstanceFollowRedirects(),"redirects cannot change source");
        ok("GET".equals(good.getRequestMethod())&&"application/json".equals(good.getRequestProperty("Accept")),"reader-only request");
        ok(good.closed,"response stream closed");
        for(int status:new int[]{301,401,403,429,500,503}){
            try{RevenueHttp.read(new Response(status,new byte[0]));throw new AssertionError("HTTP accepted");}
            catch(RevenueHttp.HttpFailure e){ok(RevenueFailure.code(e).equals("HTTP_"+status),"HTTP status retained without body");}
        }
        try{RevenueHttp.read(new Response(200,new byte[32769]));throw new AssertionError("oversized accepted");}
        catch(RevenueHttp.InvalidResponse e){ok("INVALID_RESPONSE".equals(RevenueFailure.code(e)),"oversized response rejected");}
        Exception[] errors={new RevenueHttp.Offline(),new SocketTimeoutException("secret"),new UnknownHostException("secret"),new SSLException("secret"),new IOException("secret"),new RuntimeException("secret")};
        String[] codes={"OFFLINE","TIMEOUT","DNS","TLS","NETWORK","UNKNOWN"};
        for(int i=0;i<errors.length;i++){ok(codes[i].equals(RevenueFailure.code(errors[i])),"failure classified");ok(!RevenueFailure.label(codes[i]).contains("secret"),"details sanitized");}
        ok("通信失敗".equals(RevenueFailure.label("HTTP_503 secret")),"arbitrary stored text never exposed");
        // 3.2s before HTTP headers AND 3.2s before body: old 2.5s budgets fail this response.
        try(ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){
            final Throwable[] failure={null};
            Thread responder=new Thread(()->{
                try(Socket socket=server.accept()){
                    socket.setSoTimeout(12000);BufferedReader request=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));
                    String line;while((line=request.readLine())!=null&&!line.isEmpty()){}
                    Thread.sleep(3200);OutputStream out=socket.getOutputStream();
                    out.write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));out.flush();
                    Thread.sleep(3200);out.write("{}".getBytes(StandardCharsets.US_ASCII));out.flush();
                }catch(Throwable e){failure[0]=e;}
            });responder.start();
            HttpURLConnection connection=(HttpURLConnection)new URL("http://127.0.0.1:"+server.getLocalPort()+"/reader").openConnection(Proxy.NO_PROXY);
            long start=System.nanoTime();
            try{ok("{}".equals(RevenueHttp.read(connection)),"slow response succeeds with new budgets");}
            finally{connection.disconnect();}
            responder.join(12000);ok(!responder.isAlive()&&failure[0]==null,"slow server completed cleanly");
            ok(System.nanoTime()-start>=6000000000L,"test actually exceeded original timeout");
        }
        System.out.println("PASS "+checks+" HTTP timing/failure checks");
    }
}
