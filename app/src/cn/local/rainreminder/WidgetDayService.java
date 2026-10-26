package cn.local.rainreminder;
import android.content.*;import android.appwidget.*;import android.widget.*;import android.view.View;import java.time.*;

/** One readable, vertically scrollable week, generated locally on demand. */
public class WidgetDayService extends RemoteViewsService {
 public RemoteViewsFactory onGetViewFactory(Intent intent){return new RemoteViewsFactory(){
  LocalDate monday;long now;int width;boolean valid;java.util.List<RemoteViews> rows=new java.util.ArrayList<>();
  public void onCreate(){onDataSetChanged();}
  public void onDataSetChanged(){int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);AppWidgetProviderInfo info=AppWidgetManager.getInstance(WidgetDayService.this).getAppWidgetInfo(id);valid=info!=null&&new ComponentName(WidgetDayService.this,RainWidget.class).equals(info.provider);now=System.currentTimeMillis();try{monday=LocalDate.ofEpochDay(intent.getLongExtra("day",0));}catch(Exception e){valid=false;}width=Math.max(220,Math.min(900,intent.getIntExtra("width",280)));rows=valid?WidgetWeekTable.rows(WidgetDayService.this,monday,width,now):new java.util.ArrayList<>();}
  public void onDestroy(){}
  public int getCount(){return rows.size();}
  public RemoteViews getViewAt(int position){return position>=0&&position<rows.size()?rows.get(position):null;}
  public RemoteViews getLoadingView(){RemoteViews row=new RemoteViews(getPackageName(),R.layout.rain_widget_row);row.setTextViewText(R.id.widget_course_title,"正在显示周课表…");row.setTextViewText(R.id.widget_course_detail,"本机读取");return row;}
  public int getViewTypeCount(){return 2;}
  public long getItemId(int position){return position;}
  public boolean hasStableIds(){return false;}
 };}
}
