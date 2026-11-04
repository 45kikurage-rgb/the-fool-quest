package com.aruno.foolquest.widget.next;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Scoped read-only authorization and validated daily snapshots. */
final class CouponDailyReader {
    private static final String TOKEN = "myBestCouponDailyCredentialV1";
    private static final String SNAPSHOTS = "couponRevenueDailyV2";
    private static final String API = "https://aruno-consolidated-ledger-api.45kikurage.workers.dev/api/v1/revenue/daily?month=";

    private static SecretKey key(boolean create) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey key = (SecretKey) store.getKey(TOKEN, null);
        if (key != null || !create) return key;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(TOKEN,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }

    static synchronized void setToken(Context c, String token) throws Exception {
        SharedPreferences.Editor editor = RevenueStore.prefs(c).edit().remove(SNAPSHOTS);
        if (token.isEmpty()) {
            editor.remove(TOKEN);
        } else {
            if (!token.matches("[A-Za-z0-9_-]{32,128}"))
                throw new IllegalArgumentException("日別連携専用キーの形式を確認してください");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(true));
            String encoded = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "."
                + Base64.encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.US_ASCII)), Base64.NO_WRAP);
            editor.putString(TOKEN, encoded);
        }
        if (!editor.commit()) throw new IllegalStateException("接続設定を保存できませんでした");
    }

    static JSONObject snapshot(SharedPreferences preferences, String month) {
        try{return new JSONObject(preferences.getString(SNAPSHOTS, "{}")).optJSONObject(month);}
        catch(Exception invalid){return null;}
    }

    static JSONObject validate(String raw, String month, long now) throws Exception {
        long total = RevenueStore.validateCoupon(raw, month);
        JSONObject source = new JSONObject(raw);
        String current = RevenueMath.month(now);
        int maxDay = CalendarDateMath.daysInMonth(month);
        int last = month.equals(current) ? Integer.parseInt(RevenueMath.date("dd",now))-1 : maxDay;
        if (month.compareTo(current) > 0) throw new IllegalArgumentException("future month");
        String through = last == 0 ? null : String.format(Locale.US,"%s-%02d",month,last);
        if (through == null ? !source.has("through") || !source.isNull("through") : !through.equals(source.getString("through")))
            throw new IllegalArgumentException("invalid coverage");
        JSONObject days = source.getJSONObject("days");
        if (days.length() != last) throw new IllegalArgumentException("incomplete dates");
        long shown = 0;
        for (int day=1; day<=last; day++) {
            long value = RevenueManager.strictNumber(days.get(String.format(Locale.US,"%s-%02d",month,day)),false);
            shown = RevenueManager.plus(shown,value);
        }
        if (shown > total || (last == maxDay && shown != total))
            throw new IllegalArgumentException("daily total differs");
        if (month.equals(current)) {
            // The verified Ledger reply includes today's priced coupons in its month
            // total, and supplies every previous day in the same reconciled snapshot.
            days.put(String.format(Locale.US,"%s-%02d",month,last+1),total-shown);
            shown=total;
        }
        return new JSONObject().put("total",total).put("sum",shown).put("days",days);
    }

    static void refresh(Context c, String month) throws Exception {
        if (!RevenueMath.validMonth(month) || month.compareTo("2026-10") < 0 ||
                month.compareTo(RevenueMath.month(System.currentTimeMillis())) > 0) return;
        SharedPreferences p = RevenueStore.prefs(c);
        String credential = p.getString(TOKEN,"");
        if (credential.isEmpty()) return;
        String[] parts = credential.split("\\.", -1);
        SecretKey key = key(false);
        if (key == null || parts.length != 2) return;
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
        String token = new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.US_ASCII);
        HttpURLConnection connection = (HttpURLConnection)new URL(API+month).openConnection();
        try {
            connection.setRequestProperty("Authorization","Bearer "+token);
            JSONObject data = validate(RevenueHttp.read(connection),month,System.currentTimeMillis());
            save(p,credential,month,data);
        } catch(RevenueHttp.HttpFailure e) {
            if (e.status == 409) save(p,credential,month,null);
            else if (e.status == 401 || e.status == 403) save(p,credential,null,null);
            else throw e;
        } finally {
            connection.disconnect();
        }
    }

    private static synchronized void save(SharedPreferences p,String credential,String month,JSONObject data) throws Exception {
        if (!credential.equals(p.getString(TOKEN,""))) return;
        if (month == null) {
            p.edit().remove(SNAPSHOTS).apply();
            return;
        }
        JSONObject snapshots = new JSONObject(p.getString(SNAPSHOTS,"{}"));
        if (data == null) snapshots.remove(month);
        else snapshots.put(month,data);
        p.edit().putString(SNAPSHOTS,snapshots.toString()).apply();
    }
}
