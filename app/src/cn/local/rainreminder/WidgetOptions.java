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
 static int style(Context c,int id){return id>0?prefs(c).getInt("widget_style_"+id,prefs(c).getInt("style_default",0)):prefs(c).getInt("style_default",0);}
 static void saveStyle(Context c,int id,int value){SharedPreferences.Editor edit=prefs(c).edit();if(id>0&&value<0)edit.remove("widget_style_"+id);else edit.putInt(id>0?"widget_style_"+id:"style_default",value==1?1:0);edit.commit();}
 static java.time.LocalDate date(Context c,int id,long now){java.time.LocalDate today=java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate();try{return java.time.LocalDate.ofEpochDay(prefs(c).getLong("widget_day_"+id,today.toEpochDay()));}catch(Exception e){return today;}}
 static void moveDay(Context c,int id,int delta,long now){SharedPreferences.Editor edit=prefs(c).edit();if(delta==0)edit.remove("widget_day_"+id);else{java.time.LocalDate day=date(c,id,now).plusDays(delta),today=java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate();if(day.equals(today))edit.remove("widget_day_"+id);else edit.putLong("widget_day_"+id,day.toEpochDay());}edit.commit();}
 static void remove(Context c,int[] ids){SharedPreferences.Editor edit=prefs(c).edit();for(int id:ids)edit.remove("widget_"+id).remove("widget_style_"+id).remove("widget_day_"+id);edit.apply();}
 static void prune(Context c,int[] ids){java.util.Set<String> live=new java.util.HashSet<>();for(int id:ids){live.add("widget_"+id);live.add("widget_style_"+id);live.add("widget_day_"+id);}SharedPreferences.Editor edit=prefs(c).edit();for(String key:prefs(c).getAll().keySet())if(key.startsWith("widget_")&&!live.contains(key))edit.remove(key);edit.apply();}
}
