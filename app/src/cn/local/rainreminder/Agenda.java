package cn.local.rainreminder;

import android.app.*;import android.content.*;import android.os.Build;import org.json.*;import java.time.*;import java.util.*;

final class Agenda {
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("agenda",0);}
 static JSONArray all(Context c){try{return new JSONArray(prefs(c).getString("events","[]"));}catch(Exception e){return new JSONArray();}}
 static synchronized void put(Context c,JSONObject event)throws Exception{
  JSONArray old=all(c),out=new JSONArray();boolean updated=false;
  for(int i=0;i<old.length();i++){JSONObject item=old.getJSONObject(i);if(item.getString("id").equals(event.getString("id"))){out.put(event);updated=true;}else out.put(item);}
  if(!updated)out.put(event);save(c,out);
 }
 static synchronized int addBatch(Context c,JSONArray batch)throws Exception{
  JSONArray out=all(c);Set<String> keys=new HashSet<>();for(int i=0;i<out.length();i++){JSONObject e=out.getJSONObject(i);keys.add(e.optString("title")+"|"+e.optLong("start"));}
  int n=0;long now=System.currentTimeMillis();for(int i=0;i<batch.length();i++){JSONObject e=batch.getJSONObject(i);if(!expired(e,now)&&keys.add(e.getString("title")+"|"+e.getLong("start"))){out.put(e);n++;}}save(c,out);return n;
 }
 static synchronized void remove(Context c,String id)throws Exception{removeMany(c,Collections.singleton(id),Collections.emptySet());}
 static LocalDate weekOf(long time){LocalDate date=Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate();return date.minusDays(date.getDayOfWeek().getValue()-1);}
 static boolean imported(JSONObject e){return e.has("importWeek")||e.has("ocr")||e.has("sourceImage");}
 static boolean hasImportedWeek(Context c,LocalDate week){if(prefs(c).getStringSet("importedWeeks",Collections.emptySet()).contains(week.toString()))return true;for(WeekTable.Item item:WeekTable.occurrences(all(c),week))if(item.event.optBoolean("course")&&imported(item.event))return true;return false;}
 static final class ImportResult {int count,old,duplicates,conflicts;Set<String> skipped=new TreeSet<>();}
 static synchronized ImportResult importWeeks(Context c,JSONArray batch)throws Exception{
  JSONArray out=all(c);ImportResult result=new ImportResult();long now=System.currentTimeMillis();Set<String> weeks=new HashSet<>(prefs(c).getStringSet("importedWeeks",Collections.emptySet()));
  for(int i=0;i<batch.length();i++){JSONObject e=new JSONObject(batch.getJSONObject(i).toString());if(e.optString("title").trim().isEmpty()||e.getLong("end")<=e.getLong("start"))throw new IllegalArgumentException("请核对课程名称和开始结束时间");if(cleanupExpired(e,now)){result.old++;continue;}CourseMerge.Match match=CourseMerge.match(e,out,new JSONArray());if(match.kind==CourseMerge.SAME){result.duplicates++;continue;}if(match.kind==CourseMerge.DIFFERENT&&!CourseMerge.approved(e,match)){result.conflicts++;continue;}String week=weekOf(e.getLong("start")).toString();CourseMerge.clean(e);e.put("id",UUID.randomUUID().toString()).put("importWeek",week);out.put(e);weeks.add(week);result.count++;}
  if(result.count>0){if(!prefs(c).edit().putString("events",out.toString()).putStringSet("importedWeeks",weeks).commit())throw new IllegalStateException("保存失败，请检查手机存储空间");schedule(c);}return result;
 }
 static synchronized int importBatch(Context c,JSONArray batch)throws Exception{return importWeeks(c,batch).count;}
 static synchronized int removeMany(Context c,Set<String> ids,Set<String> resetWeeks)throws Exception{
  JSONArray old=all(c),out=new JSONArray();int count=0;for(int n=0;n<old.length();n++){JSONObject e=old.getJSONObject(n);if(ids.contains(e.optString("id")))count++;else out.put(e);}
  Set<String> weeks=new HashSet<>(prefs(c).getStringSet("importedWeeks",Collections.emptySet()));weeks.removeAll(resetWeeks);
  for(String week:new HashSet<>(weeks)){boolean remaining=false;for(WeekTable.Item item:WeekTable.occurrences(out,LocalDate.parse(week)))if(item.event.optBoolean("course")&&imported(item.event)){remaining=true;break;}if(!remaining)weeks.remove(week);}
  if(!prefs(c).edit().putString("events",out.toString()).putStringSet("importedWeeks",weeks).commit())throw new IllegalStateException("删除保存失败");schedule(c);return count;
 }
 static void save(Context c,JSONArray data){if(!prefs(c).edit().putString("events",data.toString()).commit())throw new IllegalStateException("保存失败，请检查手机存储空间");schedule(c);}
 static JSONObject create(String title,String place,long start,long end,boolean course)throws Exception{
  return new JSONObject().put("id",UUID.randomUUID().toString()).put("title",title).put("place",place).put("start",start).put("end",end).put("course",course).put("repeatDays",0).put("until",end).put("lead",10).put("scanLead",course?3:-1).put("enabled",true);
 }
 static final class Due {JSONObject event;long start,at;String kind,key;Due(JSONObject e,long s,int lead,String k){event=e;start=s;at=s-lead*60000L;kind=k;key=e.optString("id")+":"+s+":"+k;}}
 static List<Due> candidates(JSONArray data,long now,Set<String> fired)throws Exception{
  List<Due> out=new ArrayList<>();ZoneId zone=ZoneId.systemDefault();
  for(int i=0;i<data.length();i++){
   JSONObject e=data.getJSONObject(i);if(!e.optBoolean("enabled",true))continue;
   long base=e.getLong("start"),until=e.optLong("until",base);int days=e.optInt("repeatDays",0);if(days!=0&&days!=1&&days!=7&&days!=14)continue;
   ZonedDateTime z=Instant.ofEpochMilli(base).atZone(zone);long diff=java.time.temporal.ChronoUnit.DAYS.between(z.toLocalDate(),Instant.ofEpochMilli(now).atZone(zone).toLocalDate());
   long first=days==0?0:Math.max(0,diff/days-1);
   for(long k=first;k<=first+(days==0?0:3);k++){
    long s=days==0?base:z.plusDays(k*days).toInstant().toEpochMilli();if(days>0&&s>until)break;
    for(String kind:new String[]{"class","scan"}){int lead=e.optInt(kind.equals("class")?"lead":"scanLead",-1);if(lead<0||lead>1440||kind.equals("scan")&&!e.optBoolean("course",false))continue;
     Due d=new Due(e,s,lead,kind);if(d.at>now&&!fired.contains(d.key))out.add(d);
    }
   }
  }
  out.sort((a,b)->Long.compare(a.at,b.at));return out;
 }
 static PendingIntent pending(Context c,Intent intent){return PendingIntent.getBroadcast(c,7000,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static final String CHANNEL="agenda_banner_v2";
 static void channel(Context c){NotificationChannel ch=new NotificationChannel(CHANNEL,"上课与日程 · 静默横幅",NotificationManager.IMPORTANCE_HIGH);ch.setSound(null,null);ch.enableVibration(false);ch.setDescription("只发送通知，不响铃、不振动、不弹全屏；横幅需在系统中允许。");c.getSystemService(NotificationManager.class).createNotificationChannel(ch);}
 // A recurring record expires only after its actual last occurrence ends.
 static boolean expired(JSONObject e,long now){
  long start=e.optLong("start"),end=e.optLong("end",start),until=e.optLong("until",start);int days=e.optInt("repeatDays",0);
  if(days==0)return end<=now;if(days!=1&&days!=7&&days!=14)return false;
  ZonedDateTime base=Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault());long diff=java.time.temporal.ChronoUnit.DAYS.between(base.toLocalDate(),Instant.ofEpochMilli(until).atZone(ZoneId.systemDefault()).toLocalDate());
  long k=Math.max(0,diff/days),last=base.plusDays(k*days).toInstant().toEpochMilli();if(last>until&&k>0)last=base.plusDays((k-1)*days).toInstant().toEpochMilli();
  return last+Math.max(0,end-start)<=now;
 }
 static synchronized void prune(Context c,long now)throws Exception{
  JSONArray old=all(c),keep=new JSONArray();Set<String> ids=new HashSet<>();Map<String,Long> duration=new HashMap<>();
  for(int n=0;n<old.length();n++){JSONObject e=old.getJSONObject(n);if(!cleanupExpired(e,now)){keep.put(e);ids.add(e.optString("id"));duration.put(e.optString("id"),Math.max(0,e.optLong("end")-e.optLong("start")));}}
  android.content.SharedPreferences.Editor edit=prefs(c).edit();boolean changed=keep.length()!=old.length();if(changed)edit.putString("events",keep.toString());
  for(String key:prefs(c).getAll().keySet())if(key.startsWith("fired_")){String value=key.substring(6);String[] bits=value.split(":");boolean remove=bits.length!=3||!ids.contains(bits[0]);if(!remove)try{remove=Long.parseLong(bits[1])+duration.get(bits[0])<=now;}catch(Exception bad){remove=true;}if(remove){edit.remove(key);c.getSystemService(NotificationManager.class).cancel(value.hashCode());changed=true;}}
  if(changed&&!edit.commit())throw new IllegalStateException("自动清理保存失败");
 }
 static boolean cleanupExpired(JSONObject e,long now){LocalDate date=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();LocalDate monday=date.minusDays(date.getDayOfWeek().getValue()-1);return expired(e,monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());}
 static synchronized void schedule(Context c){
  AlarmManager am=c.getSystemService(AlarmManager.class);Intent intent=new Intent(c,AgendaReceiver.class);am.cancel(pending(c,intent));
  PendingIntent cleanup=PendingIntent.getBroadcast(c,7002,new Intent(c,AgendaReceiver.class).setAction("cn.local.rainreminder.CLEANUP"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(cleanup);
  try{long now=System.currentTimeMillis();prune(c,now);JSONArray data=all(c);
   if(data.length()>0){long midnight=LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,midnight,cleanup);}
   List<Due> next=candidates(data,now,Collections.emptySet());if(next.isEmpty()){prefs(c).edit().remove("next").apply();return;}
   Due due=next.get(0);intent.putExtra("id",due.event.getString("id")).putExtra("kind",due.kind).putExtra("start",due.start).putExtra("at",due.at).putExtra("key",due.key);
   JSONArray batch=new JSONArray();for(Due item:next){if(item.at!=due.at)break;batch.put(new JSONObject().put("id",item.event.getString("id")).put("kind",item.kind).put("start",item.start).put("at",item.at).put("key",item.key));}intent.putExtra("batch",batch.toString());
   PendingIntent p=pending(c,intent);if(Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due.at,p);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due.at,p);
   prefs(c).edit().putLong("next",due.at).apply();
  }catch(Exception e){prefs(c).edit().putString("alarmError","提醒安排失败，请重新打开课表："+e.getMessage()).apply();}finally{RainWidget.updateAll(c);}
 }
 static synchronized boolean deliver(Context c,Intent i,long now)throws Exception{
  String key=i.getStringExtra("key"),id=i.getStringExtra("id"),kind=i.getStringExtra("kind");long start=i.getLongExtra("start",0),at=i.getLongExtra("at",0);
  if(key==null||prefs(c).getBoolean("fired_"+key,false)||now<at-2000)return false;
  JSONObject found=null;JSONArray items=all(c);for(int n=0;n<items.length();n++){JSONObject e=items.getJSONObject(n);if(e.optString("id").equals(id)){found=e;break;}}
  if(found==null||!found.optBoolean("enabled",true))return false;
  int lead=found.optInt("scan".equals(kind)?"scanLead":"lead",-1);if(lead<0||at!=start-lead*60000L)return false;
  // Validate the occurrence against the current record after edits/deletions.
  boolean valid=false;for(Due d:candidates(new JSONArray().put(found),at-1,Collections.emptySet()))if(d.key.equals(key)&&d.at==at){valid=true;break;}if(!valid)return false;
  prefs(c).edit().putBoolean("fired_"+key,true).commit();
  if(now>start+5*60000L||"scan".equals(kind)&&now>=start)return false;
  NotificationManager nm=c.getSystemService(NotificationManager.class);channel(c);
  String title=("scan".equals(kind)?"扫码提醒 · ":found.optBoolean("course")?"上课提醒 · ":"日程提醒 · ")+found.getString("title");
  String body="scan".equals(kind)?"请查看老师是否已开放签到，由你手动扫码。":"请准备参加。";
  body+="\n开始时间："+Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0)+" "+found.optString("place","");
  PendingIntent open=PendingIntent.getActivity(c,7001,new Intent(c,AgendaActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(c.getApplicationInfo().icon).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(open).setCategory(Notification.CATEGORY_REMINDER).setOnlyAlertOnce(true).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
  try{nm.notify(key.hashCode(),n);}catch(SecurityException ignored){}return true;
 }
}
