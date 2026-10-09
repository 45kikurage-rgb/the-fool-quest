package com.aruno.foolquest.widget;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

/** Only controlled reason codes are retained. No exception text, response body or credentials. */
final class RevenueFailure {
    static String code(Exception e) {
        if(e instanceof RevenueHttp.Offline)return "OFFLINE";
        if(e instanceof SocketTimeoutException)return "TIMEOUT";
        if(e instanceof UnknownHostException)return "DNS";
        if(e instanceof SSLException)return "TLS";
        if(e instanceof RevenueHttp.HttpFailure)return "HTTP_"+((RevenueHttp.HttpFailure)e).status;
        if(e instanceof RevenueHttp.InvalidResponse)return "INVALID_RESPONSE";
        return e instanceof IOException?"NETWORK":"UNKNOWN";
    }
    static String label(String code) {
        if("OFFLINE".equals(code))return "通信未接続";
        if("TIMEOUT".equals(code))return "通信時間切れ";
        if("DNS".equals(code))return "接続先の確認失敗";
        if("TLS".equals(code))return "安全な接続の失敗";
        if("INVALID_RESPONSE".equals(code))return "API応答の確認失敗";
        if("SCHEDULE".equals(code))return "更新の開始失敗";
        if(code!=null&&code.matches("HTTP_[1-5][0-9]{2}"))return "サーバー応答 "+code.substring(5);
        return "通信失敗";
    }
}
