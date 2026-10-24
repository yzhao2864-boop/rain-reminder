package cn.local.rainreminder;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.app.job.*;
import org.json.*;
import java.util.*;

final class Store {
 static android.content.SharedPreferences prefs(Context c){ return c.getSharedPreferences("rain",0); }
 static void status(Context c,String s){prefs(c).edit().putString("status",s).apply();}
 static void schedule(Context c){
  JobScheduler j=c.getSystemService(JobScheduler.class); j.cancel(2020);
  if(!prefs(c).getBoolean("enabled",false))return;
  Calendar next=Calendar.getInstance(); next.set(Calendar.HOUR_OF_DAY,prefs(c).getInt("hour",20)); next.set(Calendar.MINUTE,prefs(c).getInt("minute",0)); next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
  if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DATE,1);
  long delay=next.getTimeInMillis()-System.currentTimeMillis();
  JobInfo info=new JobInfo.Builder(2020,new ComponentName(c,DailyJob.class)).setMinimumLatency(delay).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).build();
  int result=j.schedule(info);
  prefs(c).edit().putLong("next",result==JobScheduler.RESULT_SUCCESS?next.getTimeInMillis():0).apply();
 }
 static void notify(Context c,String title,String body){
  NotificationManager nm=c.getSystemService(NotificationManager.class);
  NotificationChannel channel=new NotificationChannel("homework_banner_v2","作业提醒 · 静默横幅",NotificationManager.IMPORTANCE_HIGH);channel.setSound(null,null);channel.enableVibration(false);nm.createNotificationChannel(channel);
  Intent i=new Intent(c,MainActivity.class);
  PendingIntent p=PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification n=new Notification.Builder(c,"homework_banner_v2").setSmallIcon(c.getApplicationInfo().icon).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(p).setCategory(Notification.CATEGORY_REMINDER).setOnlyAlertOnce(true).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
  try{nm.notify(1,n);}catch(SecurityException ignored){}
 }
 static JSONArray tasks(Context c){try{return new JSONArray(prefs(c).getString("tasks","[]"));}catch(Exception e){return new JSONArray();}}
 static void save(Context c,JSONArray tasks,boolean background){
  tasks=Deadlines.sorted(tasks);
  prefs(c).edit().putString("tasks",tasks.toString()).putLong("last",System.currentTimeMillis()).putString("status","检查成功 · "+tasks.length()+" 项待核对作业/考试").commit();
  ClassWatch.schedule(c);RainWidget.updateAll(c);
  if(background&&tasks.length()>0){
   StringBuilder b=new StringBuilder();
   for(int k=0;k<Math.min(tasks.length(),6);k++){JSONObject t=tasks.optJSONObject(k); b.append(t.optString("course")).append(" · ").append(t.optString("title")).append("\n").append(t.optString("detail")).append("\n");}
   notify(c,"今日作业清单 · "+tasks.length()+" 项",b.toString());
  }
 }
}
