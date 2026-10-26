package cn.local.rainreminder;
import android.content.*;import android.appwidget.*;import android.widget.*;import android.view.View;import java.time.*;

/** One readable, vertically scrollable week, generated locally on demand. */
public class WidgetDayService extends RemoteViewsService {
 public RemoteViewsFactory onGetViewFactory(Intent intent){return new RemoteViewsFactory(){
  LocalDate monday;long now;int width;boolean valid;
  public void onCreate(){onDataSetChanged();}
  public void onDataSetChanged(){int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);AppWidgetProviderInfo info=AppWidgetManager.getInstance(WidgetDayService.this).getAppWidgetInfo(id);valid=info!=null&&new ComponentName(WidgetDayService.this,RainWidget.class).equals(info.provider);now=System.currentTimeMillis();try{monday=LocalDate.ofEpochDay(intent.getLongExtra("day",0));}catch(Exception e){valid=false;}width=Math.max(220,Math.min(900,intent.getIntExtra("width",280)));}
  public void onDestroy(){}
  public int getCount(){return valid?1:0;}
  public RemoteViews getViewAt(int position){if(!valid||position!=0)return null;RemoteViews row=new RemoteViews(getPackageName(),R.layout.rain_widget_day);row.setViewVisibility(R.id.widget_day_card_date,View.GONE);row.setImageViewBitmap(R.id.widget_day_card_grid,WidgetWeekTable.draw(WidgetDayService.this,monday,width,false,now));row.setContentDescription(R.id.widget_day_card_grid,WidgetWeekTable.description(WidgetDayService.this,monday,now));return row;}
  public RemoteViews getLoadingView(){RemoteViews row=new RemoteViews(getPackageName(),R.layout.rain_widget_row);row.setTextViewText(R.id.widget_course_title,"正在显示周课表…");row.setTextViewText(R.id.widget_course_detail,"本机读取");return row;}
  public int getViewTypeCount(){return 2;}
  public long getItemId(int position){return monday==null?0:monday.toEpochDay();}
  public boolean hasStableIds(){return true;}
 };}
}
