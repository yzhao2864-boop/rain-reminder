package cn.local.rainreminder;
import android.content.Context;import android.net.Uri;import org.json.*;import java.util.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;

/** Local display choices only; the saved task snapshot and submission state stay intact. */
final class HomeworkDisplay {
 static final String KEY="dimmed_homework";
 static String key(JSONObject task){
  String address=task.optString("url"),route=address;try{Uri u=Uri.parse(address);route=u.getHost()+"/"+u.getPath()+"/"+u.getQueryParameter("classroom_id");}catch(Exception ignored){}
  long due=Deadlines.due(task);String identity=task.optString("course")+"\n"+route+"\n"+task.optString("title")+"\n"+(due==Long.MAX_VALUE?task.optString("detail").replaceAll("\\s+"," "):String.valueOf(due));
  try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}catch(Exception e){throw new IllegalStateException(e);}
 }
 static boolean dimmed(Context c,JSONObject task){return Store.prefs(c).getStringSet(KEY,Collections.emptySet()).contains(key(task));}
 static void set(Context c,JSONObject task,boolean hidden){Set<String> values=new HashSet<>(Store.prefs(c).getStringSet(KEY,Collections.emptySet()));if(hidden)values.add(key(task));else values.remove(key(task));Store.prefs(c).edit().putStringSet(KEY,values).commit();RainWidget.updateAll(c);}
 static JSONArray select(Context c,boolean hidden){JSONArray out=new JSONArray(),tasks=Deadlines.sorted(Store.tasks(c));Set<String> values=Store.prefs(c).getStringSet(KEY,Collections.emptySet());for(int n=0;n<tasks.length();n++){JSONObject task=tasks.optJSONObject(n);if(task!=null&&values.contains(key(task))==hidden)out.put(task);}return out;}
 static JSONArray visible(Context c){return select(c,false);}
 static JSONArray dimmed(Context c){return select(c,true);}
}
