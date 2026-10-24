package cn.local.rainreminder;

import android.content.*;
import android.os.*;
import android.webkit.*;
import org.json.*;
import java.io.*;

final class Scanner {
 private static final java.util.Set<Scanner> ACTIVE=new java.util.HashSet<>();
 static final java.util.Set<Runnable> STATE_LISTENERS=new java.util.concurrent.CopyOnWriteArraySet<>();
 static boolean running(){return !ACTIVE.isEmpty();}
 void changed(){RainWidget.updateAll(context);for(Runnable r:STATE_LISTENERS)r.run();}
 static void cancelAll(){for(Scanner s:new java.util.ArrayList<>(ACTIVE))s.cancel();}
 interface Listener {void progress(String s);void done(boolean ok,String message);}
 static final String HOME="https://changjiang.yuketang.cn/v2/web/index";
 static final String USER_AGENT="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
 final Context context;final Listener listener;final boolean background;
 final Handler handler=new Handler(Looper.getMainLooper());
 WebView web;String script;JSONArray courses=new JSONArray(),tasks=new JSONArray();int course=0,tries=0;boolean reloaded=false;volatile boolean stopped=false;long start;
 Scanner(Context c,Listener l,boolean bg){context=c;listener=l;background=bg;}
 @SuppressWarnings("SetJavaScriptEnabled") static void configure(WebView w){
  WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  s.setUserAgentString(USER_AGENT);
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(w,true);
 }
 void start(){
  ACTIVE.add(this);changed();
  start=System.currentTimeMillis();
  try(InputStream in=context.getAssets().open("reader.js")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] v=new byte[4096];int n;while((n=in.read(v))!=-1)b.write(v,0,n);script=b.toString("UTF-8");}
  catch(Exception e){finish(false,"无法加载读取组件");return;}
  web=new WebView(context); configure(web);web.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(1920,android.view.View.MeasureSpec.EXACTLY));web.layout(0,0,1080,1920);
  web.setWebViewClient(new WebViewClient(){
   public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){return !allowed(r.getUrl().toString());}
   public void onReceivedError(WebView w,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())finish(false,"网络或网页加载失败，请稍后重试");}
  });
  listener.progress("正在通过接口获取学生课程…");
  new Thread(()->{try{JSONArray list=CourseApi.list();handler.post(()->{if(stopped)return;Store.prefs(context).edit().putString("course_names",list.toString()).apply();courses=list;loadCourse();});}catch(Exception e){handler.post(()->finish(false,e.getMessage()==null?"课程接口读取失败":e.getMessage()));}},"course-list").start();
 }
 void loadCourse(){if(stopped)return;tries=0;reloaded=false;try{JSONObject c=courses.getJSONObject(course);listener.progress("正在检查 "+(course+1)+"/"+courses.length()+" · "+c.getString("title"));web.loadUrl(c.getString("url"));step("read",2000);}catch(Exception e){finish(false,"课程导航信息无效");}}
 static boolean allowed(String u){try{android.net.Uri p=android.net.Uri.parse(u);return "https".equals(p.getScheme())&&"changjiang.yuketang.cn".equals(p.getHost());}catch(Exception e){return false;}}
 void step(String mode,long delay){handler.postDelayed(()->run(mode),delay);}
 void run(String mode){
  if(stopped)return;
  if(System.currentTimeMillis()-start>480000){finish(false,"检查超时，上次结果仍保留");return;}
  if(!allowed(web.getUrl())){finish(false,"需要重新登录，或网页地址发生变化");return;}
  JSONObject cmd=new JSONObject();
  try{cmd.put("mode",mode);if(mode.equals("open")){JSONObject c=courses.getJSONObject(course);cmd.put("index",c.getInt("index"));cmd.put("title",c.getString("title"));}}
  catch(Exception e){finish(false,"课程列表解析失败");return;}
  web.evaluateJavascript(script.replace("__COMMAND__",cmd.toString()),raw->{
   if(stopped)return;
   try{
    Object decoded=new JSONTokener(raw).nextValue();JSONObject result=new JSONObject(String.valueOf(decoded));
    if(result.has("error")){finish(false,result.getString("error"));return;}
    if(result.optBoolean("wait")){
     if(++tries>35){String reason=result.optString("reason");String cause="unfinished_missing".equals(reason)?"未找到未完成入口，页面可能发生变化":"count_missing".equals(reason)?"未显示待办总数，暂不能确认列表完整":"未完成列表加载超时，请检查网络后重试";finish(false,courses.getJSONObject(course).optString("title","当前课程")+"："+cause+"；上次结果已保留");return;}
     if(mode.equals("read")&&tries==18&&!reloaded){reloaded=true;listener.progress("页面加载较慢，正在重试 "+(course+1)+"/"+courses.length());web.reload();step(mode,2000);return;}
     step(mode,1000);return;
    }
    tries=0;
    if(mode.equals("courses")){
     courses=result.getJSONArray("courses");if(courses.length()==0){finish(false,"未找到课程，无法确认作业状态");return;}
     step("open",100);return;
    }
    if(mode.equals("open")){listener.progress("正在检查 "+(course+1)+"/"+courses.length()+" · "+courses.getJSONObject(course).getString("title"));step("unfinished",1800);return;}
    if(mode.equals("unfinished")){step("read",2200);return;}
    if(result.optBoolean("hasMore")||result.optInt("unknown")>0){finish(false,"发现尚未适配的任务类型或分页，请打开对应课程核对；未覆盖上次结果");return;}
    int expected=result.isNull("expected")?-1:result.optInt("expected",-1);
    if(expected<0||expected!=result.optInt("total")){finish(false,"列表条数无法核对，可能有分页或页面改版；未覆盖上次结果");return;}
    JSONArray list=result.getJSONArray("tasks");
    for(int k=0;k<list.length();k++){JSONObject t=list.getJSONObject(k);t.put("course",courses.getJSONObject(course).getString("title"));t.put("url",result.getString("url"));tasks.put(t);}
    course++;
    if(course==courses.length()){Store.save(context,tasks,background);finish(true,"已完成 "+courses.length()+" 门课程检查");}
    else{loadCourse();}
   }catch(Exception e){finish(false,"页面结构暂不兼容，请使用手动打开课程核对");}
  });
 }
 void finish(boolean ok,String message){if(stopped)return;stopped=true;ACTIVE.remove(this);handler.removeCallbacksAndMessages(null);if(web!=null){web.stopLoading();web.destroy();web=null;}try{listener.done(ok,message);}finally{changed();}}
 void cancel(){if(stopped)return;stopped=true;ACTIVE.remove(this);handler.removeCallbacksAndMessages(null);if(web!=null){web.stopLoading();web.destroy();web=null;}changed();}
}
