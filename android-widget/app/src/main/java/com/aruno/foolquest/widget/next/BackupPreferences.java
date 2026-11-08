package com.aruno.foolquest.widget.next;

import android.content.SharedPreferences;
import java.util.*;

/** All existing writes pass through one serialized durable transaction boundary. */
final class BackupPreferences implements SharedPreferences {
    final SharedPreferences raw;final BackupCoordinator backups;
    BackupPreferences(SharedPreferences p,BackupCoordinator b){raw=p;backups=b;}
    public Map<String,?> getAll(){synchronized(BackupCoordinator.LOCK){return raw.getAll();}}
    public String getString(String k,String d){return raw.getString(k,d);}
    public Set<String> getStringSet(String k,Set<String> d){return raw.getStringSet(k,d);}
    public int getInt(String k,int d){return raw.getInt(k,d);}
    public long getLong(String k,long d){return raw.getLong(k,d);}
    public float getFloat(String k,float d){return raw.getFloat(k,d);}
    public boolean getBoolean(String k,boolean d){return raw.getBoolean(k,d);}
    public boolean contains(String k){return raw.contains(k);}
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){raw.registerOnSharedPreferenceChangeListener(l);}
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){raw.unregisterOnSharedPreferenceChangeListener(l);}
    public Editor edit(){return new Edit();}
    final class Edit implements Editor {
        final Map<String,Object> changes=new LinkedHashMap<>();boolean clear;
        public Editor putString(String k,String v){changes.put(k,v);return this;}
        public Editor putStringSet(String k,Set<String> v){changes.put(k,v==null?null:new HashSet<>(v));return this;}
        public Editor putInt(String k,int v){changes.put(k,v);return this;}
        public Editor putLong(String k,long v){changes.put(k,v);return this;}
        public Editor putFloat(String k,float v){changes.put(k,v);return this;}
        public Editor putBoolean(String k,boolean v){changes.put(k,v);return this;}
        public Editor remove(String k){changes.put(k,null);return this;}
        public Editor clear(){clear=true;return this;}
        public boolean commit(){
            synchronized(BackupCoordinator.LOCK){
                Map<String,Object> before=new HashMap<>(raw.getAll()),after=new HashMap<>(before);
                if(clear)after.clear();for(Map.Entry<String,Object> e:changes.entrySet())if(e.getValue()==null)after.remove(e.getKey());else after.put(e.getKey(),e.getValue());
                try {return backups.commit(before,after);}catch(Exception e){backups.failure(e);return false;}
            }
        }
        public void apply(){if(!commit())throw new IllegalStateException("端末保存に失敗しました。バックアップ画面を確認してください");}
    }
    static boolean replace(SharedPreferences p,Map<String,?> values){
        Editor e=p.edit().clear();for(Map.Entry<String,?> entry:values.entrySet())put(e,entry.getKey(),entry.getValue());return e.commit();
    }
    @SuppressWarnings("unchecked") static void put(Editor e,String k,Object v){
        if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Long)e.putLong(k,(Long)v);
        else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);
        else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,(Set<String>)v);
        else throw new IllegalArgumentException("未対応の設定型です");
    }
}
