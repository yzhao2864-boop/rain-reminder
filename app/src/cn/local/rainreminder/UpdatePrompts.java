package cn.local.rainreminder;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import java.lang.ref.WeakReference;

/** One update invitation per foreground session, after a confirmed release is available. */
final class UpdatePrompts implements Application.ActivityLifecycleCallbacks {
 static UpdatePrompts instance;
 int started,session,promptSession=-1;String promptVersion="";
 WeakReference<Activity> current=new WeakReference<>(null);
 AlertDialog dialog;
 static void register(Application app){if(instance!=null)return;instance=new UpdatePrompts();app.registerActivityLifecycleCallbacks(instance);AppUpdates.listeners.add(UpdatePrompts::maybeShow);}
 static void maybeShow(){if(instance!=null)instance.show();}
 void show(){
  Activity a=current.get();
  if(a==null||a.isFinishing()||a.isDestroyed()||a instanceof UpdateActivity||!AppUpdates.prefs(a).getBoolean("auto",true))return;
  if(a instanceof MainActivity&&((MainActivity)a).login!=null)return;
  if(dialog!=null&&dialog.isShowing())return;
  if(AppUpdates.busy&&"downloading".equals(AppUpdates.phase))return;
  AppUpdates.Info info=AppUpdates.latest(a);
  if(info==null||(promptSession==session&&info.version.equals(promptVersion)))return;
  promptSession=session;promptVersion=info.version;
  String notes=info.notes==null?"":info.notes.trim();if(notes.length()>400)notes=notes.substring(0,400)+"…";
  boolean ready=AppUpdates.ready(a);
  String body=(ready?"新版已下载，可继续安装。":"约 "+String.format(java.util.Locale.CHINA,"%.1f",info.size/1000000.0)+" MB，确认后下载更新。")+"\n覆盖安装会保留本机账号和课表。"+(notes.isEmpty()?"":"\n\n"+notes);
  dialog=new AlertDialog.Builder(a).setTitle("发现新版待课 "+info.version).setMessage(body)
   .setPositiveButton(ready?"安装更新":"立即更新",(d,n)->a.startActivity(new Intent(a,UpdateActivity.class).putExtra("startUpdate",true)))
   .setNegativeButton("稍后",null).create();
  dialog.setOnDismissListener(d->dialog=null);dialog.show();
 }
 public void onActivityStarted(Activity a){if(started++==0){session++;AppUpdates.maybeCheck(a);}}
 public void onActivityResumed(Activity a){current=new WeakReference<>(a);show();}
 public void onActivityPaused(Activity a){if(current.get()==a){current.clear();if(dialog!=null)dialog.dismiss();}}
 public void onActivityStopped(Activity a){started=Math.max(0,started-1);}
 public void onActivityCreated(Activity a,Bundle b){}
 public void onActivitySaveInstanceState(Activity a,Bundle b){}
 public void onActivityDestroyed(Activity a){if(current.get()==a)current.clear();}
}
