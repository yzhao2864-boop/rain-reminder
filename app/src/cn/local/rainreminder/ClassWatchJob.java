package cn.local.rainreminder;
import android.app.job.*;
public class ClassWatchJob extends JobService {
 volatile boolean stopped;
 public boolean onStartJob(JobParameters params){stopped=false;if(!ClassWatch.enabled(this))return false;new Thread(()->{try{ClassWatch.check(this);}catch(Exception error){Store.prefs(this).edit().putString("classWatchError","开课检查失败，请检查登录和网络").apply();}new android.os.Handler(getMainLooper()).post(()->{if(!stopped)jobFinished(params,false);});},"open-class-check").start();return true;}
 public boolean onStopJob(JobParameters p){stopped=true;return true;}
}
