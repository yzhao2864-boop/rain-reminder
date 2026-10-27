package cn.local.rainreminder;
import android.content.*;
public class AgendaReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  try{
   if("cn.local.rainreminder.CLEANUP".equals(i.getAction())){Agenda.schedule(c);return;}
   if(Agenda.TEST_ACTION.equals(i.getAction())){Agenda.testNotification(c,i.getStringExtra("kind"));Agenda.schedule(c,null);return;}
   String batch=i.getStringExtra("batch");
   if(batch==null){String key=i.getStringExtra("key");Agenda.deliver(c,i,System.currentTimeMillis());Agenda.schedule(c,key);}
   else{org.json.JSONArray items=new org.json.JSONArray(batch);for(int n=0;n<items.length();n++){org.json.JSONObject e=items.getJSONObject(n);Intent one=new Intent(c,AgendaReceiver.class).putExtra("id",e.getString("id")).putExtra("kind",e.getString("kind")).putExtra("start",e.getLong("start")).putExtra("at",e.getLong("at")).putExtra("key",e.getString("key"));Agenda.deliver(c,one,System.currentTimeMillis());}Agenda.schedule(c,null);}
  }catch(Exception e){Agenda.recordFailure(c,"提醒处理失败："+e.getMessage());Agenda.schedule(c,null);}
 }
}
