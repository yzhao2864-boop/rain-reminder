package cn.local.rainreminder;
import org.json.*;import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;
/** Read-only projection; displaying a widget must never prune or edit the timetable. */
final class WidgetData {
 static final class Snapshot {LocalDate date;String heading,subtitle,homework,hint;List<WeekTable.Item> items=new ArrayList<>();long next;}
 static Snapshot build(JSONArray events,JSONArray tasks,long last,long now){Snapshot out=new Snapshot();ZoneId zone=ZoneId.systemDefault();LocalDate today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();out.date=today;out.next=today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
  for(int offset=0;offset<8;offset++){LocalDate date=today.plusDays(offset),monday=date.minusDays(date.getDayOfWeek().getValue()-1);List<WeekTable.Item> day=new ArrayList<>();for(WeekTable.Item item:WeekTable.occurrences(events,monday)){if(!item.event.optBoolean("enabled",true)||item.day!=date.getDayOfWeek().getValue()-1||item.end<=now)continue;if(item.start>now)out.next=Math.min(out.next,item.start);if(item.end>now)out.next=Math.min(out.next,item.end);day.add(item);}if(!day.isEmpty()){out.date=date;out.items=day;break;}}
  long days=java.time.temporal.ChronoUnit.DAYS.between(today,out.date);out.heading=out.items.isEmpty()?"今日安排":days==0?"今日安排":days==1?"明日预告":"接下来安排";out.subtitle=out.date.format(DateTimeFormatter.ofPattern("M月d日 E",Locale.CHINA))+(out.items.isEmpty()?"":" · "+out.items.size()+"项");
  if(last<=0){out.homework="作业 · 请先登录检查";out.hint="点击进入作业页";}else{JSONArray sorted=Deadlines.sorted(tasks);out.homework="待交作业 "+sorted.length()+"项 · 缓存 "+Instant.ofEpochMilli(last).atZone(zone).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));out.hint=sorted.length()==0?"以上次成功检查为准 · 点击查看":sorted.optJSONObject(0).optString("title","点击查看作业");}return out;
 }
 static String detail(WeekTable.Item item,long now){String from=Instant.ofEpochMilli(item.start).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")),to=Instant.ofEpochMilli(item.end).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"));return (item.start<=now&&item.end>now?"进行中 · ":"")+from+"–"+to+(item.event.optString("place").isEmpty()?"":" · "+item.event.optString("place"));}
}
