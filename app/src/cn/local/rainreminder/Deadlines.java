package cn.local.rainreminder;
import org.json.*;import java.time.*;import java.util.*;import java.util.regex.*;
final class Deadlines {
 // The official list can put its label before OR after the date, including
 // on separate lines. Require adjacent deadline labels, never arbitrary dates.
 static final String LABEL="(?:截止(?:时间|日期)?|结束时间|提交期限|提交截止(?:时间|日期)?)";
 static final String DATE="(?:(20\\d{2})[-/.年])?(\\d{1,2})[-/.月](\\d{1,2})日?(?:[T\\s/／|｜]+(\\d{1,2})[:：](\\d{2})(?:[:：](\\d{2}))?)?";
 static final Pattern BEFORE=Pattern.compile(LABEL+"[：:\\s]*(?:为|于)?[：:\\s]*"+DATE);
 static final Pattern AFTER=Pattern.compile(DATE+"[\\s/／|｜]*(?:星期[一二三四五六日天][\\s/／|｜]*)?"+LABEL);
 static long due(JSONObject item){long saved=item.optLong("deadline",0);if(saved>0)return saved;
  String text=item.optString("deadlineText","");if(text.isEmpty())text=item.optString("detail");text=text.replace('\u00a0',' ').replace('\u202f',' ');
  if(Pattern.compile(LABEL+"[：:\\s]*(?:不限|无截止|未设置|长期有效)|无截止(?:时间|日期)?").matcher(text).find())return Long.MAX_VALUE;
  Matcher date=BEFORE.matcher(text);
  if(!date.find()){
   date=AFTER.matcher(text);boolean found=false;
   while(date.find()){
    String prefix=text.substring(Math.max(0,date.start()-24),date.start());
    if(!Pattern.compile("(?:发布(?:时间|日期)?|开始(?:时间|日期)?|创建(?:时间|日期)?)[：:\\s]*$").matcher(prefix).find()){found=true;break;}
   }
   if(!found)return Long.MAX_VALUE;
  }
  try{int year=date.group(1)==null?LocalDate.now().getYear():Integer.parseInt(date.group(1));LocalDate day=LocalDate.of(year,Integer.parseInt(date.group(2)),Integer.parseInt(date.group(3)));int hour=date.group(4)==null?23:Integer.parseInt(date.group(4)),minute=date.group(5)==null?59:Integer.parseInt(date.group(5)),second=date.group(6)==null?0:Integer.parseInt(date.group(6));return day.atTime(hour,minute,second).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();}catch(Exception e){return Long.MAX_VALUE;}}
 static JSONArray sorted(JSONArray tasks){List<JSONObject> items=new ArrayList<>();for(int n=0;n<tasks.length();n++)if(tasks.optJSONObject(n)!=null)items.add(tasks.optJSONObject(n));items.sort((a,b)->Long.compare(due(a),due(b)));JSONArray out=new JSONArray();for(JSONObject e:items)out.put(e);return out;}
 static String label(JSONObject item){long due=due(item);if(due==Long.MAX_VALUE)return "截止时间待核对";return (due<System.currentTimeMillis()?"已过截止 · ":"截止 · ")+Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));}
}
