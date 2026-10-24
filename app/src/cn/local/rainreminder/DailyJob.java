package cn.local.rainreminder;
import android.app.job.*;
public class DailyJob extends JobService {
 private Scanner scanner;
 public boolean onStartJob(JobParameters p){
  if(!Store.prefs(this).getBoolean("enabled",false))return false;if(Scanner.running()){Store.schedule(this);return false;}
  scanner=new Scanner(this,new Scanner.Listener(){public void progress(String s){} public void done(boolean ok,String message){
   if(!ok){Store.status(DailyJob.this,"检查失败："+message);Store.notify(DailyJob.this,"作业检查未完成",message+"；上次作业记录仍保留，请打开 App 检查。");}
   Store.schedule(DailyJob.this);jobFinished(p,false);scanner=null;
  }},true); scanner.start();return true;
 }
 public boolean onStopJob(JobParameters p){if(scanner!=null){scanner.cancel();scanner=null;} Store.status(this,"后台检查被系统中断，上次结果仍保留");return true;}
}
