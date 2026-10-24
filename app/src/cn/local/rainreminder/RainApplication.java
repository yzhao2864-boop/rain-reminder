package cn.local.rainreminder;
import android.app.Application;

public class RainApplication extends Application {
 public void onCreate(){super.onCreate();UpdatePrompts.register(this);}
}
