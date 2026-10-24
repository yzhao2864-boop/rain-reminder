package cn.local.rainreminder;
import org.json.*;import java.util.*;
final class CourseNames {
 static String clean(String s){return s.replaceAll("[^\\p{IsHan}A-Za-z]","").toLowerCase(java.util.Locale.ROOT);}
 static String suggest(String raw,JSONArray courses){String observed=clean(raw),best="";float high=0,second=0;for(int k=0;k<courses.length();k++){JSONObject c=courses.optJSONObject(k);String name=c==null?courses.optString(k):c.optString("title");String target=clean(name);if(target.isEmpty())continue;int[][] lcs=new int[observed.length()+1][target.length()+1];for(int i=1;i<=observed.length();i++)for(int j=1;j<=target.length();j++)lcs[i][j]=observed.charAt(i-1)==target.charAt(j-1)?lcs[i-1][j-1]+1:Math.max(lcs[i-1][j],lcs[i][j-1]);int count=lcs[observed.length()][target.length()];float score=count/(float)target.length()+count*.035f;if(count<2)score=0;if(score>high){second=high;high=score;best=name;}else second=Math.max(second,score);}return high>=.5f&&high-second>=.04f?best:"";}
}
