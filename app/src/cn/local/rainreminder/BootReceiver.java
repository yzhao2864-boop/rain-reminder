package cn.local.rainreminder;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){Store.schedule(c);Agenda.schedule(c);ClassWatch.schedule(c);}
}
