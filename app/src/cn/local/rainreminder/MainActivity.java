package cn.local.rainreminder;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

public class MainActivity extends Activity {
 final int green=Color.rgb(33,92,82),ink=Color.rgb(29,43,40),muted=Color.rgb(92,109,102);
 boolean examsTab;TextView examOverview,examInfo;LinearLayout examList;LinearLayout root,list,updateBox;TextView overview,status,loginTip,tasksHeading;Button checkButton;ProgressBar checkProgress;Scanner scanner;WebView login;boolean checking=false,loginOnly=false,confirmingLogin=false,loggingOut=false;final Runnable updateListener=()->renderUpdateBanner();final Runnable scanListener=()->{if(login==null&&status!=null&&!isDestroyed()){renderStatus();if(list!=null)renderTasks();}};String mobileScript;int loginGeneration=0;LinearLayout liveBox;boolean watching=false;final Handler watchHandler=new Handler(Looper.getMainLooper());final Runnable watchTick=new Runnable(){public void run(){refreshOpenClasses();watchHandler.postDelayed(this,60000);}};
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(Color.rgb(244,247,242));Agenda.schedule(this);examsTab=b!=null&&b.getBoolean("examsTab",false);home();accountAction(getIntent());}
 protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putBoolean("examsTab",examsTab);}
 int dp(int x){return (int)(x*getResources().getDisplayMetrics().density);}
 TextView text(String s,int size,int color){TextView t=Ui.text(this,s,size);t.setTextColor(color);return t;}
 GradientDrawable background(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));return d;}
 void insets(View v){v.setOnApplyWindowInsetsListener((view,i)->{int top,bottom;if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars());top=bars.top;bottom=bars.bottom;}else{top=i.getSystemWindowInsetTop();bottom=i.getSystemWindowInsetBottom();}view.setPadding(view.getPaddingLeft(),top,view.getPaddingRight(),bottom);return i;});}
 LinearLayout panel(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dp(18),dp(16),dp(18),dp(16));l.setBackground(background(Color.WHITE));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(14),0,0);l.setLayoutParams(p);return l;}
 void button(LinearLayout box,String name,Runnable fn){Ui.button(this,box,name,fn);}
 void home(){
  login=null;list=null;liveBox=null;overview=null;status=null;checkButton=null;checkProgress=null;examList=null;root=Ui.screen(this,"作业与考试",false);Ui.tabs(this,root,false);Ui.segments(this,root,new String[]{"作业","考试"},examsTab?1:0,index->{examsTab=index==1;home();});
  updateBox=Ui.column(this);root.addView(updateBox);renderUpdateBanner();if(examsTab){examHome();return;}
  LinearLayout summary=Ui.card(this,root);summary.setPadding(dp(18),dp(16),dp(18),dp(16));summary.addView(text("雨课堂 · 作业助手",12,green));overview=text("",27,ink);overview.setTypeface(Ui.strong(this));summary.addView(overview);status=text("",12,muted);status.setMaxLines(4);summary.addView(status);
  checkProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);checkProgress.setIndeterminate(true);summary.addView(checkProgress,new LinearLayout.LayoutParams(-1,dp(3)));checkButton=Ui.primary(this,summary,"检查最新作业",()->{if(Store.prefs(this).getLong("last",0)==0)openLogin(Scanner.HOME);else scan();});checkButton.setTextSize(15);checkButton.setMinHeight(dp(48));renderStatus();
  liveBox=Ui.column(this);root.addView(liveBox);renderOpenClasses();
  LinearLayout tasksBar=new LinearLayout(this);tasksBar.setGravity(Gravity.CENTER_VERTICAL);tasksBar.setPadding(0,dp(18),0,dp(4));root.addView(tasksBar);tasksHeading=text("待交作业",17,ink);tasksHeading.setTypeface(Ui.strong(this));tasksBar.addView(tasksHeading,new LinearLayout.LayoutParams(0,-2,1));TextView order=Ui.hint(this,"截止时间排序");order.setTextSize(11);tasksBar.addView(order);list=Ui.column(this);root.addView(list);renderTasks();

 }
 void examHome(){
  LinearLayout summary=Ui.card(this,root);summary.addView(text("学校教务 · 考试安排",12,green));examOverview=text("考试早知道",27,ink);examOverview.setTypeface(Ui.strong(this));summary.addView(examOverview);examInfo=Ui.hint(this,"");summary.addView(examInfo);Ui.primary(this,summary,"查询学校考试",()->startActivity(new Intent(this,SchoolImportActivity.class).putExtra("exams",true)));summary.addView(Ui.hint(this,"只添加已公布时间的考试"));
  TextView heading=text("已添加的考试",17,ink);heading.setTypeface(Ui.strong(this));heading.setPadding(0,dp(20),0,dp(4));root.addView(heading);examList=Ui.column(this);root.addView(examList);renderExams();
 }
 void renderExams(){if(examList==null)return;examList.removeAllViews();java.util.List<JSONObject> events=new ArrayList<>();JSONArray data=Agenda.all(this);for(int i=0;i<data.length();i++){JSONObject e=data.optJSONObject(i);if(e!=null&&"schoolExam".equals(e.optString("source"))&&e.optLong("end")>System.currentTimeMillis())events.add(e);}events.sort((a,b)->Long.compare(a.optLong("start"),b.optLong("start")));if(examOverview!=null)examOverview.setText(events.isEmpty()?"考试早知道":events.size()+" 场待考");long last=Agenda.prefs(this).getLong("examQueryLast",0);if(examInfo!=null)examInfo.setText(last>0?"上次读取："+format(last)+"\n"+Agenda.prefs(this).getString("examQuerySummary",""):"学校公布后可查询，核对后添加提醒。");
  if(events.isEmpty()){LinearLayout empty=Ui.card(this,examList);empty.setPadding(dp(18),dp(24),dp(18),dp(24));empty.addView(text("暂无已添加的考试",17,ink));empty.addView(Ui.hint(this,"临近期末时点上方查询，公布后即可核对并设置提醒。"));return;}
  for(JSONObject e:events){LinearLayout card=Ui.card(this,examList);TextView date=text(new SimpleDateFormat("MM月dd日 E  HH:mm",Locale.CHINA).format(new Date(e.optLong("start")))+" – "+new SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date(e.optLong("end"))),12,green);card.addView(date);card.addView(text(e.optString("title").replaceFirst("^考试 · ",""),18,ink));if(!e.optString("place").isEmpty())card.addView(Ui.hint(this,e.optString("place")));int lead=e.optInt("lead",-1);String reminder=lead<0?"提醒已关闭":"提前 "+(lead>0&&lead%1440==0?(lead/1440)+" 天":lead>0&&lead%60==0?(lead/60)+" 小时":lead+" 分钟")+"提醒";card.addView(Ui.hint(this,reminder));card.setOnClickListener(v->examMenu(e));card.setOnLongClickListener(v->{examMenu(e);return true;});card.setFocusable(true);card.setContentDescription(e.optString("title")+"，点击修改或删除");}
 }
 void examMenu(JSONObject e){new AlertDialog.Builder(this).setTitle(e.optString("title")).setItems(new String[]{"修改考试与提醒","删除这场考试"},(d,n)->{if(n==0)AgendaActivity.edit(this,e,changed->{try{Agenda.put(this,changed);Ui.keepScroll(this,this::home);}catch(Exception error){Ui.error(this,error);}});else new AlertDialog.Builder(this).setTitle("删除这场考试？").setMessage("只移除这条考试安排和对应提醒。").setPositiveButton("删除",(dialog,which)->{try{Agenda.remove(this,e.getString("id"));Ui.keepScroll(this,this::home);}catch(Exception error){Ui.error(this,error);}}).setNegativeButton("取消",null).show();}).show();}
 void renderUpdateBanner(){if(updateBox==null||login!=null||isDestroyed())return;updateBox.removeAllViews();AppUpdates.Info info=AppUpdates.latest(this);if(info==null)return;LinearLayout banner=Ui.card(this,updateBox);banner.setBackground(Ui.bg(this,0xFFE4EEE9));TextView label=Ui.text(this,"新版本 "+info.version+" 已可更新  ›",13);label.setTypeface(Ui.strong(this));label.setTextColor(green);banner.addView(label);banner.setOnClickListener(v->startActivity(new Intent(this,UpdateActivity.class)));}
 void accountAction(Intent i){if(i!=null&&(i.hasExtra("accountAction")||i.hasExtra("widgetCheckHomework")||i.hasExtra("widgetShowError"))&&examsTab){examsTab=false;home();}if(i!=null&&i.getBooleanExtra("widgetShowError",false)){i.removeExtra("widgetShowError");new AlertDialog.Builder(this).setTitle("作业检查需要处理").setMessage(Store.prefs(this).getString("status","请重新检查作业")).setPositiveButton("重新登录",(d,n)->openLogin(Scanner.HOME)).setNeutralButton("重试检查",(d,n)->{if(Store.prefs(this).getLong("last",0)>0)scan();else openLogin(Scanner.HOME);}).setNegativeButton("关闭",null).show();}String action=i==null?null:i.getStringExtra("accountAction");if(i!=null)i.removeExtra("accountAction");if("login".equals(action))openLogin(Scanner.HOME);else if("logout".equals(action))performLogout();if(i!=null&&i.getBooleanExtra("widgetCheckHomework",false)){i.removeExtra("widgetCheckHomework");if(Scanner.running()){renderStatus();Toast.makeText(this,"作业正在检查，请稍候",Toast.LENGTH_SHORT).show();}else if(Store.prefs(this).getLong("last",0)<=0)openLogin(Scanner.HOME);else if(login!=null)closeLogin(true);else scan();}}
 protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);accountAction(i);}
 void renderOpenClasses(){if(liveBox==null)return;liveBox.removeAllViews();if(!Store.prefs(this).getBoolean("watchOpenClass",true))return;try{JSONArray active=new JSONArray(Store.prefs(this).getString("openClasses","[]"));long last=Store.prefs(this).getLong("classWatchLast",0);if(active.length()>0){LinearLayout box=Ui.card(this,liveBox);box.addView(Ui.text(this,"正在开课 · 请核对扫码",18));box.addView(Ui.hint(this,"检测于 "+format(last)));for(int n=0;n<active.length();n++)box.addView(Ui.text(this,active.getJSONObject(n).optString("courseName"),16));}String error=Store.prefs(this).getString("classWatchError","");if(!error.isEmpty())liveBox.addView(Ui.hint(this,error));}catch(Exception ignored){}}
 void refreshOpenClasses(){ClassWatch.schedule(this);if(watching||!ClassWatch.enabled(this)||loggingOut)return;watching=true;new Thread(()->{try{ClassWatch.check(this);}catch(Exception error){Store.prefs(this).edit().putString("classWatchError","开课检查失败，请检查登录和网络").apply();}runOnUiThread(()->{watching=false;if(!isDestroyed()&&login==null)renderOpenClasses();});},"foreground-class-watch").start();}
 void renderStatus(){if(status==null)return;boolean active=checking||Scanner.running();if(overview!=null)overview.setText(active?"正在检查作业":Store.prefs(this).getLong("last",0)>0?Store.tasks(this).length()+" 项待交作业":"记住每一份作业");String s=active?"正在检查，请稍候…":Store.prefs(this).getString("status","登录后可读取自己的待交作业。");long last=Store.prefs(this).getLong("last",0);if(last>0)s+="\n上次成功："+format(last);long next=Store.prefs(this).getLong("next",0);if(Store.prefs(this).getBoolean("enabled",false)&&next>0)s+="\n下次计划："+format(next)+" 后";if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)s+="\n通知尚未允许，请在应用设置中开启";status.setText(s);if(checkButton!=null){checkButton.setEnabled(!active);checkButton.setText(active?"正在检查…":Store.prefs(this).getLong("last",0)==0?"登录雨课堂":"检查最新作业");}if(checkProgress!=null)checkProgress.setVisibility(active?View.VISIBLE:View.GONE);}
 String format(long n){return new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(n));}
 void renderTasks(){if(list==null)return;boolean active=checking||Scanner.running();list.removeAllViews();JSONArray tasks=Deadlines.sorted(Store.tasks(this));if(tasksHeading!=null)tasksHeading.setText("待交作业"+(tasks.length()>0?" · "+tasks.length():""));if(tasks.length()==0){LinearLayout empty=Ui.card(this,list);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(18),dp(24),dp(18),dp(24));Ui.NavIcon icon=new Ui.NavIcon(this,false,false);empty.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(32)));empty.addView(text(active?"正在获取最新作业":Store.prefs(this).getLong("last",0)>0?"暂无待交作业":"登录后查看作业",17,ink));empty.addView(Ui.hint(this,active?"检查完成后会显示结果，请稍候。":Store.prefs(this).getLong("last",0)>0?"以最近一次成功检查为准":"登录自己的雨课堂账号即可检查。"));return;}if(active)list.addView(Ui.hint(this,"正在更新，下面保留上次成功检查的结果。"));for(int k=0;k<tasks.length();k++){JSONObject task=tasks.optJSONObject(k);LinearLayout card=Ui.card(this,list);if(!task.optString("course").isEmpty())card.addView(text(task.optString("course"),11,muted));TextView title=text(task.optString("title"),17,ink);title.setTypeface(Ui.strong(this));card.addView(title);card.addView(text(Deadlines.label(task),12,green));card.setFocusable(true);card.setContentDescription(task.optString("title")+"，"+Deadlines.label(task)+"，点击查看详情");card.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(task.optString("title")).setMessage(task.optString("detail")).setPositiveButton("到官网核对",(d,n)->openLogin(task.optString("url",Scanner.HOME))).setNegativeButton("关闭",null).show());}}
 void permission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},33);}
 void scan(){
  if(loggingOut)return;
  if(checking||Scanner.running()){Toast.makeText(this,"正在检查，请稍候",0).show();return;}
  checking=true;renderStatus();renderTasks();scanner=new Scanner(this,new Scanner.Listener(){public void progress(String s){if(status!=null)status.setText(s);}public void done(boolean ok,String message){checking=false;scanner=null;if(!ok)Store.status(MainActivity.this,"检查失败："+message);renderStatus();if(list!=null)renderTasks();Toast.makeText(MainActivity.this,message,1).show();}},false);scanner.start();
 }
 void openLogin(String url){
  if(loggingOut)return;
  if(checking){Toast.makeText(this,"请等待检查完成，避免登录状态冲突",0).show();return;}
  loginGeneration++;loginOnly=Scanner.HOME.equals(url);confirmingLogin=false;
  if(mobileScript==null)try(InputStream in=getAssets().open("mobile-login.js")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);mobileScript=b.toString("UTF-8");}catch(IOException e){Toast.makeText(this,"无法加载手机登录布局",1).show();return;}
  LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(1);insets(wrap);wrap.setBackgroundColor(Color.rgb(244,247,242));
  LinearLayout bar=new LinearLayout(this);bar.setOrientation(0);bar.setPadding(dp(12),dp(6),dp(12),dp(6));wrap.addView(bar);
  Button back=new Button(this);back.setText("‹ 返回");back.setTextColor(green);back.setStateListAnimator(null);back.setBackgroundColor(Color.TRANSPARENT);bar.addView(back,new LinearLayout.LayoutParams(dp(90),dp(48)));back.setOnClickListener(v->closeLogin(false));TextView title=text(loginOnly?"登录长江雨课堂":"课程官网",19,ink);title.setPadding(dp(12),dp(5),0,dp(5));bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  loginTip=text(loginOnly?"使用已绑定的手机号登录，成功后自动返回作业列表。":"官网页面支持双指放大；返回可查看作业清单。",13,muted);loginTip.setPadding(dp(20),dp(6),dp(20),dp(10));wrap.addView(loginTip);
  login=new WebView(this);Scanner.configure(login);login.setBackgroundColor(Color.rgb(244,247,242));login.getSettings().setBuiltInZoomControls(true);login.getSettings().setDisplayZoomControls(false);login.getSettings().setUseWideViewPort(true);login.getSettings().setLoadWithOverviewMode(true);
  login.setWebViewClient(new WebViewClient(){
   public void onPageCommitVisible(WebView w,String url){onPageFinished(w,url);}
   public void onPageFinished(WebView w,String url){
    if(w!=login)return;
    if(Scanner.allowed(url)&&Uri.parse(url).getPath().startsWith("/web")){
     loginTip.setText("使用已绑定的手机号登录，成功后自动返回作业列表。");
     w.evaluateJavascript(mobileScript,null);w.postDelayed(()->{if(w==login)w.evaluateJavascript(mobileScript,null);},700);
    }else if(loginOnly&&Scanner.allowed(url)&&Uri.parse(url).getPath().startsWith("/v2/")){
     w.evaluateJavascript("!!document.getElementById('tab-student')",v->{if(w==login&&"true".equals(v))confirmSession(w);});
    }else if(!loginOnly&&Scanner.allowed(url)){
     float scale=w.getWidth()/getResources().getDisplayMetrics().density/1100f;
     w.evaluateJavascript("(function(){var m=document.querySelector('meta[name=viewport]');if(m)m.content='width=1100,initial-scale="+scale+",maximum-scale=4,user-scalable=yes';})();",null);
    }
   }
   public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){
   Uri u=r.getUrl();String host=u.getHost();if("https".equals(u.getScheme())&&host!=null&&(host.equals("yuketang.cn")||host.endsWith(".yuketang.cn")||host.equals("open.weixin.qq.com")||host.equals("mp.weixin.qq.com")||host.equals("www.xuetangx.com")&&u.getPath().startsWith("/mobile-app/private-url")))return false;
   if(r.isForMainFrame()&&"weixin".equals(u.getScheme()))new AlertDialog.Builder(MainActivity.this).setMessage("打开微信完成雨课堂登录？").setPositiveButton("打开",(d,n)->{try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){Toast.makeText(MainActivity.this,"无法打开微信，请尝试网页登录方式",1).show();}}).setNegativeButton("取消",null).show();return true;
  }});
  wrap.addView(login,new LinearLayout.LayoutParams(-1,0,1));setContentView(wrap);login.loadUrl(Scanner.allowed(url)?url:Scanner.HOME);
 }
 void confirmSession(WebView w){
  if(confirmingLogin)return;confirmingLogin=true;final int generation=loginGeneration;w.setVisibility(View.INVISIBLE);loginTip.setText("正在确认登录状态…");CookieManager.getInstance().flush();
  new Thread(()->{try{CourseApi.list();AccountProfile.refresh(this,true,null);runOnUiThread(()->{if(generation!=loginGeneration||w!=login)return;Toast.makeText(this,"登录成功，正在检查作业",0).show();closeLogin(true);});}catch(Exception e){runOnUiThread(()->{if(generation!=loginGeneration||w!=login)return;confirmingLogin=false;w.setVisibility(View.VISIBLE);loginTip.setText("登录状态暂未确认，请稍后返回首页再检查。");});}},"login-confirm").start();
 }
 void closeLogin(boolean scanAfter){loginGeneration++;confirmingLogin=false;if(login!=null){CookieManager.getInstance().flush();WebView old=login;login=null;old.stopLoading();if(old.getParent() instanceof ViewGroup)((ViewGroup)old.getParent()).removeView(old);old.destroy();}loginTip=null;home();if(scanAfter)scan();}
 void performLogout(){
  if(loggingOut)return;loggingOut=true;AccountProfile.clear(this);loginGeneration++;confirmingLogin=false;getSystemService(android.app.job.JobScheduler.class).cancel(WidgetCheckJob.ID);WidgetCheckJob.clear(this);Scanner.cancelAll();scanner=null;checking=false;
  if(login!=null){WebView old=login;login=null;old.stopLoading();if(old.getParent() instanceof ViewGroup)((ViewGroup)old.getParent()).removeView(old);old.destroy();}
  Store.prefs(this).edit().putBoolean("enabled",false).remove("tasks").remove("last").remove("next").remove("course_names").remove("profileNickname").putString("status","已退出账号，请重新登录").commit();
  synchronized(ClassWatch.class){Store.schedule(this);RainWidget.updateAll(this);getSystemService(android.app.job.JobScheduler.class).cancel(ClassWatch.JOB);android.content.SharedPreferences.Editor clean=Store.prefs(this).edit().remove("openClasses").remove("classWatchLast").remove("classWatchError");for(String key:Store.prefs(this).getAll().keySet())if(key.startsWith("seenLesson_"))clean.remove(key);clean.apply();getSystemService(NotificationManager.class).cancelAll();}
  LinearLayout waiting=new LinearLayout(this);waiting.setOrientation(1);waiting.setGravity(Gravity.CENTER);waiting.setPadding(dp(24),dp(48),dp(24),dp(24));waiting.addView(new ProgressBar(this));waiting.addView(text("正在退出账号…",18,ink));setContentView(waiting);
  WebStorage.getInstance().deleteAllData();WebView cleaner=new WebView(this);cleaner.clearCache(true);cleaner.clearHistory();cleaner.clearFormData();cleaner.destroy();WebViewDatabase.getInstance(this).clearHttpAuthUsernamePassword();
  CookieManager.getInstance().removeAllCookies(removed->{CookieManager.getInstance().flush();loggingOut=false;if(!isFinishing()&&!isDestroyed()){home();Toast.makeText(this,"已退出本机账号",0).show();}});
 }
 @Override public void onBackPressed(){if(loggingOut)return;if(login!=null)closeLogin(false);else super.onBackPressed();}
 @Override protected void onPause(){Scanner.STATE_LISTENERS.remove(scanListener);AppUpdates.listeners.remove(updateListener);watchHandler.removeCallbacks(watchTick);super.onPause();}
 @Override protected void onResume(){super.onResume();Scanner.STATE_LISTENERS.add(scanListener);AppUpdates.listeners.add(updateListener);AppUpdates.maybeCheck(this);renderUpdateBanner();if(examsTab&&examList!=null)renderExams();watchHandler.removeCallbacks(watchTick);watchHandler.post(watchTick);if(login==null&&status!=null){renderStatus();if(list!=null)renderTasks();}}
 @Override protected void onDestroy(){watchHandler.removeCallbacks(watchTick);loginGeneration++;if(scanner!=null)scanner.cancel();if(login!=null){login.destroy();login=null;}super.onDestroy();}
}
