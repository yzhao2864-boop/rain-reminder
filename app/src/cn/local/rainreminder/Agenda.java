package cn.local.rainreminder;

import android.app.*;import android.content.*;import android.os.Build;import org.json.*;import java.time.*;import java.util.*;

final class Agenda {
 static final int ALARM_BASE=7100,ALARM_SLOTS=32,TEST_ALARM=7200;
 static final long CLASS_GRACE=15*60000L,SCAN_GRACE=10*60000L;
 static final String TEST_ACTION="cn.local.rainreminder.TEST";
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
 static final class Due {JSONObject event;long start,at;String kind,key;Due(JSONObject e,long s,int lead,String k){event=e;start=s;at=s-lead*60000L;kind=k;key=e.optString("id")+":"+s+":"+k+":"+at;}}
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
 static List<Due> catchUps(JSONArray data,long now,Set<String> fired)throws Exception{
  List<Due> out=new ArrayList<>();for(Due d:candidates(data,now-86400000L,fired))if(d.at<=now&&now<=deadline(d))out.add(d);return out;
 }
 static long deadline(Due d){return d.start+("scan".equals(d.kind)?SCAN_GRACE:CLASS_GRACE);}
 static Set<String> firedKeys(Context c){Set<String> out=new HashSet<>();for(Map.Entry<String,?> e:prefs(c).getAll().entrySet())if(e.getKey().startsWith("fired_")&&e.getValue() instanceof Boolean&&(Boolean)e.getValue())out.add(e.getKey().substring(6));return out;}
 static Intent dueIntent(Context c,Due d){return new Intent(c,AgendaReceiver.class).putExtra("id",d.event.optString("id")).putExtra("kind",d.kind).putExtra("start",d.start).putExtra("at",d.at).putExtra("key",d.key);}
 static PendingIntent pending(Context c,Intent intent){return PendingIntent.getBroadcast(c,7000,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static PendingIntent cleanupPending(Context c){return PendingIntent.getBroadcast(c,7002,new Intent(c,AgendaReceiver.class).setAction("cn.local.rainreminder.CLEANUP"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static PendingIntent slotPending(Context c,int slot,Intent intent){return PendingIntent.getBroadcast(c,ALARM_BASE+slot,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static final String CHANNEL="agenda_banner_v2";
 // Keep the existing channel: its importance/sound and the user's banner choice are immutable user settings.
 static void channel(Context c){NotificationManager nm=c.getSystemService(NotificationManager.class);if(nm.getNotificationChannel(CHANNEL)!=null)return;NotificationChannel ch=new NotificationChannel(CHANNEL,"上课与日程 · 静默横幅",NotificationManager.IMPORTANCE_HIGH);ch.setSound(null,null);ch.enableVibration(false);ch.setDescription("只发送通知，不响铃、不振动、不弹全屏；横幅需在系统中允许。");nm.createNotificationChannel(ch);}
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
  NotificationManager nm=c.getSystemService(NotificationManager.class);
  for(String key:new ArrayList<>(prefs(c).getAll().keySet())){if(!key.startsWith("fired_")&&!key.startsWith("missed_"))continue;String value=key.substring(key.startsWith("fired_")?6:7);String[] bits=value.split(":");boolean remove=bits.length!=4||!ids.contains(bits[0]);if(!remove)try{Long d=duration.get(bits[0]);remove=d==null||Long.parseLong(bits[1])+d<=now;}catch(Exception bad){remove=true;}if(remove){edit.remove(key);if(key.startsWith("fired_"))nm.cancel(value.hashCode());changed=true;}}
  if(changed&&!edit.commit())throw new IllegalStateException("自动清理保存失败");
 }
 static boolean cleanupExpired(JSONObject e,long now){LocalDate date=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();LocalDate monday=date.minusDays(date.getDayOfWeek().getValue()-1);return expired(e,monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());}
 static void schedule(Context c){schedule(c,null);}
 static synchronized void schedule(Context c,String skipKey){
  AlarmManager am=c.getSystemService(AlarmManager.class);
  am.cancel(pending(c,new Intent(c,AgendaReceiver.class)));
  am.cancel(cleanupPending(c));
  for(int slot=0;slot<ALARM_SLOTS;slot++){PendingIntent old=PendingIntent.getBroadcast(c,ALARM_BASE+slot,new Intent(c,AgendaReceiver.class),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);if(old!=null)am.cancel(old);}
  try{
   long now=System.currentTimeMillis();prune(c,now);JSONArray data=all(c);channel(c);
   if(data.length()>0){long midnight=LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,midnight,cleanupPending(c));}
   List<Due> late=catchUps(data,now,firedKeys(c));
   for(Due d:late){if(skipKey!=null&&skipKey.equals(d.key))continue;try{deliver(c,dueIntent(c,d),now);}catch(Exception itemError){recordFailure(c,"补发失败："+itemError.getMessage());}}
   List<Due> next=candidates(data,now,firedKeys(c));int count=Math.min(next.size(),ALARM_SLOTS);
   boolean exactAllowed=Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms(),inexact=false;int armed=0;long earliest=0;
   for(int slot=0;slot<count;slot++){
    Due d=next.get(slot);PendingIntent p=slotPending(c,slot,dueIntent(c,d));
    try{
     if(exactAllowed)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,d.at,p);else{am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,d.at,p);inexact=true;}
     armed++;if(earliest==0||d.at<earliest)earliest=d.at;
    }catch(SecurityException denied){
     am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,d.at,p);inexact=true;armed++;if(earliest==0||d.at<earliest)earliest=d.at;
    }
   }
   android.content.SharedPreferences.Editor edit=prefs(c).edit().putInt("scheduledCount",armed).putLong("lastScheduleAt",now).putString("lastScheduleMode",inexact?"inexact":exactAllowed?"exact":"inexact").remove("alarmError");
   if(armed>0)edit.putLong("next",earliest);else edit.remove("next");
   edit.apply();
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
  if(now>deadline(new Due(found,start,lead,kind))){if(!prefs(c).getBoolean("missed_"+key,false))prefs(c).edit().putBoolean("missed_"+key,true).apply();record(c,"未发送 · "+label(kind)+" · 已超过补发时限（计划 "+time(at)+"）");return false;}
  channel(c);String blocked=blockedReason(c);if(blocked!=null){record(c,"未发送 · "+label(kind)+" · "+blocked);return false;}
  NotificationManager nm=c.getSystemService(NotificationManager.class);
  String title=("scan".equals(kind)?"扫码提醒 · ":found.optBoolean("course")?"上课提醒 · ":"日程提醒 · ")+found.getString("title");
  String body="scan".equals(kind)?"请查看老师是否已开放签到，由你手动扫码。":"请准备参加。";
  body+="\n开始时间："+Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0)+" "+found.optString("place","");
  if(now-at>60000L)body+="\n系统延迟送达（计划 "+time(at)+"）。";
  if("scan".equals(kind)&&now>=start)body+="\n课程已开始，请查看是否仍可扫码。";
  PendingIntent open=PendingIntent.getActivity(c,7001,new Intent(c,AgendaActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(c.getApplicationInfo().icon).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(open).setCategory(Notification.CATEGORY_REMINDER).setOnlyAlertOnce(true).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
  try{nm.notify(key.hashCode(),n);}catch(Exception e){record(c,"发送失败 · "+label(kind)+" · "+e.getMessage());return false;}
  if(!prefs(c).edit().putBoolean("fired_"+key,true).commit())record(c,"已提交但去重标记保存失败 · "+label(kind));
  else record(c,"已提交 · "+label(kind)+" · 计划 "+time(at)+" · 实际 "+time(now)+(now-at>60000L?" · 延迟 "+((now-at)/60000L)+" 分钟":""));
  return true;
 }
 static String label(String kind){return "scan".equals(kind)?"扫码提醒":"上课提醒";}
 static String time(long t){return Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0).toString();}
 static void record(Context c,String result){prefs(c).edit().putString("lastNotifyResult",result).putLong("lastNotifyAt",System.currentTimeMillis()).apply();}
 static void recordFailure(Context c,String result){record(c,"失败 · "+result);}
 static String lastResult(Context c){return prefs(c).getString("lastNotifyResult","尚无发送记录");}
 static String alarmError(Context c){return prefs(c).getString("alarmError","");}
 static long nextAt(Context c){return prefs(c).getLong("next",0);}
 static int scheduledCount(Context c){return prefs(c).getInt("scheduledCount",0);}
 static String lastScheduleMode(Context c){return prefs(c).getString("lastScheduleMode","");}
 static long lastScheduleAt(Context c){return prefs(c).getLong("lastScheduleAt",0);}
 static boolean exactAllowed(Context c){return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
 static String blockedReason(Context c){
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return "通知权限未允许";
  NotificationManager nm=c.getSystemService(NotificationManager.class);if(!nm.areNotificationsEnabled())return "系统通知已关闭";
  NotificationChannel ch=nm.getNotificationChannel(CHANNEL);if(ch==null)return "课程通知通道不存在";if(ch.getImportance()==NotificationManager.IMPORTANCE_NONE)return "课程通知通道已关闭";return null;
 }
 static String notificationIssue(Context c){
  channel(c);String blocked=blockedReason(c);if(blocked!=null)return blocked;
  NotificationChannel ch=c.getSystemService(NotificationManager.class).getNotificationChannel(CHANNEL);
  if(ch!=null&&ch.getImportance()<NotificationManager.IMPORTANCE_HIGH)return "课程通知通道重要度低于“高”，系统可能不显示横幅；可在通道设置中核对。";
  if(!exactAllowed(c))return "精确定时未开启，提醒可能延迟；迟到时会在宽限内补发。";
  return null;
 }
 static boolean testNotification(Context c,String kind){
  try{
   channel(c);String blocked=blockedReason(c);if(blocked!=null){record(c,"未发送 · 课程通道测试 · "+blocked);return false;}
   NotificationManager nm=c.getSystemService(NotificationManager.class);
   PendingIntent open=PendingIntent.getActivity(c,7004,new Intent(c,AgendaActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   String body="这是课程通知通道测试，和上课/扫码提醒使用同一通道。\n不会响铃、振动或全屏；若系统仍不显示横幅，请在通道设置中确认横幅/悬浮通知。";
   Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(c.getApplicationInfo().icon).setContentTitle("测试 · "+label(kind)).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(open).setCategory(Notification.CATEGORY_REMINDER).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
   nm.notify(("test:"+kind+":"+System.currentTimeMillis()).hashCode(),n);record(c,"已提交 · 课程通道测试（"+("scan".equals(kind)?"扫码":"上课")+"）");return true;
  }catch(Exception e){record(c,"发送失败 · 课程通道测试 · "+e.getMessage());return false;}
 }
}
