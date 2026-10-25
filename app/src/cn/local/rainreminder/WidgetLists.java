package cn.local.rainreminder;
import android.app.*;import android.appwidget.*;import android.content.*;import android.os.Build;import android.view.View;import android.widget.*;import java.util.*;import org.json.*;

/** Native collection views: both panels scroll independently without opening the app. */
final class WidgetLists {
 static List<RemoteViews> rows(Context c,boolean homework,long now){
  List<RemoteViews> out=new ArrayList<>();
  if(homework){JSONArray tasks=Deadlines.sorted(HomeworkDisplay.visible(c));for(int n=0;n<tasks.length();n++){JSONObject t=tasks.optJSONObject(n);if(t!=null)out.add(row(c,t.optString("title"),Deadlines.label(t)+" · "+t.optString("course"),0));}
   if(out.isEmpty())out.add(row(c,Store.prefs(c).getLong("last",0)>0?"暂无需要显示的作业":"登录后查看作业","点击进入作业页",0));
  }else{WidgetData.Snapshot data=WidgetData.build(Agenda.all(c),HomeworkDisplay.visible(c),Store.prefs(c).getLong("last",0),now);for(WeekTable.Item item:data.items)out.add(row(c,item.event.optString("title"),WidgetData.detail(item,now),data.date.toEpochDay()));if(out.isEmpty())out.add(row(c,"近期没有后续安排","点击进入课表",data.date.toEpochDay()));}
  return out;
 }
 static RemoteViews row(Context c,String title,String detail,long day){RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.rain_widget_row);row.setTextViewText(R.id.widget_course_title,title);row.setTextViewText(R.id.widget_course_detail,detail);row.setOnClickFillInIntent(R.id.widget_course_area,new Intent().putExtra("widgetDay",day));return row;}
 static void bind(Context c,RemoteViews views,int widgetId,int viewId,boolean homework,long now){
  views.setViewVisibility(viewId,View.VISIBLE);
  Intent open=new Intent(c,homework?MainActivity.class:AgendaActivity.class).setData(android.net.Uri.parse("rainwidget://list/open/"+widgetId+"/"+viewId)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  views.setPendingIntentTemplate(viewId,PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0)));
  if(Build.VERSION.SDK_INT>=31)Inline.bind(c,views,viewId,homework,now);
  else views.setRemoteAdapter(viewId,new Intent(c,WidgetListService.class).putExtra("homework",homework).setData(android.net.Uri.parse("rainwidget://list/data/"+widgetId+"/"+viewId+"/"+homework)));
 }
 static final class Inline {static void bind(Context c,RemoteViews views,int viewId,boolean homework,long now){RemoteViews.RemoteCollectionItems.Builder items=new RemoteViews.RemoteCollectionItems.Builder().setViewTypeCount(1).setHasStableIds(false);List<RemoteViews> rows=rows(c,homework,now);for(int n=0;n<rows.size();n++)items.addItem(n,rows.get(n));views.setRemoteAdapter(viewId,items.build());}}
}
