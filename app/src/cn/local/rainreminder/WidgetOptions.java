package cn.local.rainreminder;
import android.content.*;

final class WidgetOptions {
 static final int SCHEDULE_FIRST=0,HOMEWORK_FIRST=1,SCHEDULE_ONLY=2,HOMEWORK_ONLY=3;
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("widget_display",0);}
 static boolean valid(int mode){return mode>=0&&mode<=3;}
 static int mode(Context c,int id){int fallback=prefs(c).getInt("default",SCHEDULE_FIRST);if(!valid(fallback))fallback=SCHEDULE_FIRST;int value=id>0?prefs(c).getInt("widget_"+id,fallback):fallback;return valid(value)?value:fallback;}
 static void save(Context c,int id,int mode){SharedPreferences.Editor edit=prefs(c).edit();if(id>0&&!valid(mode))edit.remove("widget_"+id);else if(valid(mode))edit.putInt(id>0?"widget_"+id:"default",mode);edit.commit();RainWidget.updateAll(c);}
 static boolean homeworkFirst(int mode){return mode==HOMEWORK_FIRST||mode==HOMEWORK_ONLY;}
 static boolean both(int mode){return mode==SCHEDULE_FIRST||mode==HOMEWORK_FIRST;}
 static void remove(Context c,int[] ids){SharedPreferences.Editor edit=prefs(c).edit();for(int id:ids)edit.remove("widget_"+id);edit.apply();}
 static void prune(Context c,int[] ids){java.util.Set<String> live=new java.util.HashSet<>();for(int id:ids)live.add("widget_"+id);SharedPreferences.Editor edit=prefs(c).edit();for(String key:prefs(c).getAll().keySet())if(key.startsWith("widget_")&&!live.contains(key))edit.remove(key);edit.apply();}
}
