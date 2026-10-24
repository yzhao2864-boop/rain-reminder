package cn.local.rainreminder;
import java.util.regex.*;import java.text.Normalizer;
final class CourseText {
 // Match whole labels only: a course containing one of these words is still a course.
 static boolean isUiText(String raw){if(raw==null)return true;String text=Normalizer.normalize(raw,Normalizer.Form.NFKC).replaceAll("[^\\p{L}\\p{N}]","");return text.isEmpty()||text.matches("(?:待核对(?:课程)?|切回本周|返回本周|回到本周|回本周|切换本周|本周|上一周|下一周|上周|下周|回)+");}
 static String buildingLabels(String text){return text.replaceAll("([一二三四五六七八九十]+)綜(?=[A-Fa-f0-9]|合|楼|樓|$)","$1综");}
 static final class Parts {String title,place;boolean uncertainRoom;Parts(String t,String p){title=t;place=p;uncertainRoom=p.matches(".*[A-Za-z][0-9]{1,2}$");}}
 static Parts parse(String raw){String text=Normalizer.normalize(buildingLabels(raw).replaceAll("[①②③④⑤⑥](?=.{0,7}(?:教|楼|馆))","@"),Normalizer.Form.NFKC).replaceAll("\\s+","");text=text.replaceAll("[①②③④⑤⑥](?=.{0,7}(?:教|楼|馆))","@");text=text.replaceAll("(?<=.{2})回(?=(?:[一二三四五六七八九十]+教|逸夫楼|[东西南北]体育馆|[一二三四五六七八九十]+综))","@");int split=Math.max(text.indexOf('@'),text.indexOf('＠'));String title=text,place="";
  if(split>=0){title=text.substring(0,split);place=text.substring(split+1);}else{Matcher m=Pattern.compile("(?:[一二三四五六七八九十]+教|逸夫楼|[东西南北]体育馆|[一二三四五六七八九十]+综)").matcher(text);if(m.find()){title=text.substring(0,m.start());place=text.substring(m.start());}}
  title=title.replaceAll("[@①②③④⑤⑥回]+$","").replaceAll("^[_—-]+|[_—-]+$","");if(title.length()<2)title="待核对课程";
  Matcher room=Pattern.compile("([A-Fa-f])([0-9OoIl|]{2,5})$").matcher(place);if(room.find()){String digits=room.group(2).replace('O','0').replace('o','0').replace('I','1').replace('l','1').replace('|','1');place=place.substring(0,room.start())+room.group(1).toUpperCase(java.util.Locale.ROOT)+digits;}
  return new Parts(title,place);
 }
}
