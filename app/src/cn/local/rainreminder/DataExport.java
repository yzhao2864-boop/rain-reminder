package cn.local.rainreminder;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.widget.Toast;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** One-way snapshot of the local homework/course data for sharing outside the app. */
final class DataExport {
static final String[] WEEKDAYS={"周一","周二","周三","周四","周五","周六","周日"};
static final DateTimeFormatter DAY=DateTimeFormatter.ofPattern("yyyy-MM-dd"),CLOCK=DateTimeFormatter.ofPattern("HH:mm"),STAMP=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),DUE_TEXT=DateTimeFormatter.ofPattern("MM-dd HH:mm");

static File folder(Context c){return new File(c.getCacheDir(),"exports");}
static String fileName(){return "daike-export-"+LocalDate.now().format(DAY)+".json";}
static String clock(long time,ZoneId zone){return Instant.ofEpochMilli(time).atZone(zone).format(CLOCK);}

static JSONObject build(Context c)throws Exception{
long now=System.currentTimeMillis();ZoneId zone=ZoneId.systemDefault();LocalDate today=LocalDate.now(),monday=Agenda.weekOf(now);
JSONArray stored=Agenda.all(c),usable=new JSONArray();
for(int n=0;n<stored.length();n++){JSONObject e=stored.optJSONObject(n);if(e!=null&&e.optBoolean("enabled",true))usable.put(e);}
// Expand stored courses/exams into dated occurrences, this week first.
List<WeekTable.Item> occurrences=new ArrayList<>();
for(int w=0;w<41;w++)occurrences.addAll(WeekTable.occurrences(usable,monday.plusWeeks(w)));
long floor=monday.atStartOfDay(zone).toInstant().toEpochMilli();
JSONArray courses=new JSONArray(),exams=new JSONArray(),schedule=new JSONArray();
List<WeekTable.Item> thisWeek=new ArrayList<>(),todayCourses=new ArrayList<>();
for(WeekTable.Item item:occurrences){
if(item.end<floor)continue;
JSONObject e=item.event;LocalDate date=Instant.ofEpochMilli(item.start).atZone(zone).toLocalDate();
if(e.optBoolean("course")){
courses.put(course(e,item,zone));
if(date.isBefore(monday.plusWeeks(1)))thisWeek.add(item);
if(date.equals(today))todayCourses.add(item);
}else if("schoolExam".equals(e.optString("source")))exams.put(exam(e,item,zone));
else schedule.put(timed(e,item,zone));
}
JSONArray homework=homework(c,now,zone);
JSONObject root=new JSONObject();
root.put("app","待课");root.put("appVersion",AppUpdates.version(c));
root.put("exportedAt",Instant.ofEpochMilli(now).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));root.put("exportedAtMillis",now);
root.put("summary",summary(c,now,zone,today,homework,todayCourses,thisWeek,exams));
root.put("homework",homework);root.put("courses",courses);root.put("exams",exams);root.put("schedule",schedule);
return root;
}

static JSONObject timed(JSONObject e,WeekTable.Item item,ZoneId zone)throws Exception{
JSONObject o=new JSONObject();LocalDate date=Instant.ofEpochMilli(item.start).atZone(zone).toLocalDate();
o.put("title",e.optString("title"));o.put("place",e.optString("place"));
o.put("date",date.format(DAY));o.put("weekday",item.day+1);o.put("weekdayName",WEEKDAYS[item.day]);
o.put("startTime",clock(item.start,zone));o.put("endTime",clock(item.end,zone));
o.put("startMillis",item.start);o.put("endMillis",item.end);
return o;
}
static JSONObject course(JSONObject e,WeekTable.Item item,ZoneId zone)throws Exception{
JSONObject o=timed(e,item,zone);
if(!e.optString("periods").isEmpty())o.put("periods",e.optString("periods"));
if(e.optInt("schoolWeek")>0)o.put("schoolWeek",e.optInt("schoolWeek"));
if(!e.optString("schoolTerm").isEmpty())o.put("term",e.optString("schoolTerm"));
if(e.optInt("repeatDays")>0)o.put("repeatDays",e.optInt("repeatDays"));
if(!e.optString("source").isEmpty())o.put("source",e.optString("source"));
return o;
}
static JSONObject exam(JSONObject e,WeekTable.Item item,ZoneId zone)throws Exception{
JSONObject o=timed(e,item,zone);
if(!e.optString("examKind").isEmpty())o.put("kind",e.optString("examKind"));
return o;
}

static JSONArray homework(Context c,long now,ZoneId zone)throws Exception{
JSONArray out=new JSONArray(),tasks=Deadlines.sorted(Store.tasks(c));
for(int n=0;n<tasks.length();n++){
JSONObject t=tasks.optJSONObject(n);if(t==null)continue;JSONObject o=new JSONObject();
o.put("course",t.optString("course"));o.put("title",t.optString("title"));o.put("detail",t.optString("detail"));
long due=Deadlines.due(t);
if(due==Long.MAX_VALUE)o.put("deadline",JSONObject.NULL).put("deadlineNote","截止时间待核对");
else o.put("deadline",Instant.ofEpochMilli(due).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).put("deadlineMillis",due).put("deadlineText",Instant.ofEpochMilli(due).atZone(zone).format(DUE_TEXT)).put("remaining",Deadlines.remaining(due,now));
if(!t.optString("url").isEmpty())o.put("url",t.optString("url"));
o.put("dimmed",HomeworkDisplay.dimmed(c,t));
out.put(o);
}
return out;
}

static String courseLine(WeekTable.Item item,ZoneId zone){
JSONObject e=item.event;StringBuilder s=new StringBuilder();
s.append(clock(item.start,zone)).append("-").append(clock(item.end,zone)).append(" ").append(e.optString("title"));
if(!e.optString("place").isEmpty())s.append(" @").append(e.optString("place"));
if(!e.optString("periods").isEmpty())s.append("（第").append(e.optString("periods")).append("节）");
return s.toString();
}

static String summary(Context c,long now,ZoneId zone,LocalDate today,JSONArray homework,List<WeekTable.Item> todayCourses,List<WeekTable.Item> thisWeek,JSONArray exams){
StringBuilder s=new StringBuilder();
s.append("待课数据导出 · ").append(Instant.ofEpochMilli(now).atZone(zone).format(STAMP)).append(" · 版本 ").append(AppUpdates.version(c)).append("\n\n");
s.append("【未完成作业】").append(homework.length()).append(" 项\n");
if(homework.length()==0)s.append("（无）\n");
int shown=Math.min(homework.length(),20);
for(int n=0;n<shown;n++){
JSONObject o=homework.optJSONObject(n);
s.append(n+1).append(". ").append(o.optString("course")).append(" · ").append(o.optString("title")).append("\n ");
if(o.isNull("deadline"))s.append("截止时间待核对");else s.append("截止 ").append(o.optString("deadlineText")).append("（").append(o.optString("remaining")).append("）");
s.append("\n");
}
if(homework.length()>shown)s.append("…… 其余 ").append(homework.length()-shown).append(" 项见 JSON 文件\n");
s.append("\n【今日课程】").append(today.getMonthValue()).append("月").append(today.getDayOfMonth()).append("日 ").append(WEEKDAYS[today.getDayOfWeek().getValue()-1]).append("\n");
if(todayCourses.isEmpty())s.append("今日无课\n");
for(WeekTable.Item item:todayCourses)s.append(courseLine(item,zone)).append("\n");
s.append("\n【本周课程】\n");
if(thisWeek.isEmpty())s.append("本周无课\n");
int lastDay=-1;
for(WeekTable.Item item:thisWeek){
if(item.day!=lastDay){lastDay=item.day;LocalDate date=Instant.ofEpochMilli(item.start).atZone(zone).toLocalDate();s.append(WEEKDAYS[item.day]).append(" ").append(date.getMonthValue()).append("-").append(date.getDayOfMonth()).append("：\n");}
s.append(" ").append(courseLine(item,zone)).append("\n");
}
if(exams.length()>0){
s.append("\n【考试】\n");
for(int n=0;n<exams.length();n++){JSONObject o=exams.optJSONObject(n);s.append(o.optString("date")).append(" ").append(o.optString("startTime")).append("-").append(o.optString("endTime")).append(" ").append(o.optString("title"));if(!o.optString("place").isEmpty())s.append(" @").append(o.optString("place"));s.append("\n");}
}
return s.toString();
}

static void share(Activity a){
JSONObject root=null;String json=null;
try{
root=build(a);json=root.toString(2);
File dir=folder(a);if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("无法创建导出文件夹");
File file=new File(dir,fileName());
try(FileOutputStream out=new FileOutputStream(file)){out.write(json.getBytes(StandardCharsets.UTF_8));}
Uri uri=Uri.parse("content://"+a.getPackageName()+".export/"+file.getName());
Intent send=new Intent(Intent.ACTION_SEND).setType("application/json")
.putExtra(Intent.EXTRA_STREAM,uri)
.putExtra(Intent.EXTRA_SUBJECT,"待课作业与课表 · "+LocalDate.now().format(DAY))
.putExtra(Intent.EXTRA_TEXT,root.optString("summary"))
.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
send.setClipData(ClipData.newRawUri("待课导出",uri));
a.startActivity(Intent.createChooser(send,"导出作业与课表"));
}catch(Exception e){
// Sharing must never lose the snapshot: fall back to the clipboard.
try{
if(json==null)json=build(a).toString(2);
ClipboardManager clipboard=a.getSystemService(ClipboardManager.class);
clipboard.setPrimaryClip(ClipData.newPlainText("待课导出",json));
Toast.makeText(a,"分享不可用，导出内容已复制到剪贴板",Toast.LENGTH_LONG).show();
}catch(Exception bad){Toast.makeText(a,"导出失败："+(bad.getMessage()==null?"无法生成导出内容":bad.getMessage()),Toast.LENGTH_LONG).show();}
}
}
}
