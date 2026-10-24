package cn.local.rainreminder;
import android.webkit.CookieManager;
import org.json.*;
import java.net.*;
import java.io.*;

final class CourseApi {
 static JSONObject get(String endpoint)throws Exception{
  String cookies=CookieManager.getInstance().getCookie(endpoint);
  if(cookies==null||cookies.length()==0)throw new IOException("请先登录长江雨课堂");
  HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();
  try{
   c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);
   c.setRequestProperty("Cookie",cookies);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Referer",Scanner.HOME);c.setRequestProperty("User-Agent",Scanner.USER_AGENT);c.setRequestProperty("X-Requested-With","XMLHttpRequest");
   int code=c.getResponseCode();if(code!=200)throw new IOException("课程接口返回 "+code+"，请检查登录状态或网络");
   ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];
   try(InputStream in=c.getInputStream()){int n;while((n=in.read(buf))!=-1){b.write(buf,0,n);if(b.size()>4*1024*1024)throw new IOException("课程响应超出预期大小");}}
   JSONObject response=new JSONObject(b.toString("UTF-8"));
   return response;
  }finally{c.disconnect();}
 }
 static JSONArray list()throws Exception{
  JSONObject response=get("https://changjiang.yuketang.cn/v2/api/web/courses/list?identity=2");
   if(response.optInt("errcode",-1)!=0)throw new IOException("课程接口未授权或已变化，请重新登录");
   JSONArray raw=response.getJSONObject("data").getJSONArray("list"),out=new JSONArray();
   for(int k=0;k<raw.length();k++){
    JSONObject r=raw.getJSONObject(k),course=r.getJSONObject("course");String id=r.getString("classroom_id"),university=course.getString("university_id");
    // Changjiang's observed web route uses platform_id=3. course.platform may be null.
    String platform="3";
    if(!id.matches("[0-9]+")||!university.matches("[0-9]+"))throw new IOException("课程标识格式发生变化");
    String url="https://changjiang.yuketang.cn/v2/web/studentLog/"+id+"?university_id="+university+"&platform_id="+platform+"&classroom_id="+id+"&tab=unfinished";
    JSONObject result=new JSONObject();result.put("title",course.getString("name"));result.put("url",url);out.put(result);
   }
   if(out.length()==0)throw new IOException("未找到学生课程，请确认登录了正确账号");
   return out;
 }
}
