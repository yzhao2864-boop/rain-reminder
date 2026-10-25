package cn.local.rainreminder;
import android.content.*;import android.appwidget.*;import android.widget.*;import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;

/** Day cards are rendered on demand, so seven timetable bitmaps never share one widget update. */
public class WidgetDayService extends RemoteViewsService {
 public RemoteViewsFactory onGetViewFactory(Intent intent){return new RemoteViewsFactory(){
  LocalDate date;long now;int width,body;boolean valid;
  public void onCreate(){onDataSetChanged();}
  public void onDataSetChanged(){int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);AppWidgetProviderInfo info=AppWidgetManager.getInstance(WidgetDayService.this).getAppWidgetInfo(id);valid=info!=null&&new ComponentName(WidgetDayService.this,RainWidget.class).equals(info.provider);now=System.currentTimeMillis();try{date=LocalDate.ofEpochDay(intent.getLongExtra("day",0));}catch(Exception e){valid=false;}width=Math.max(160,Math.min(900,intent.getIntExtra("width",276)));body=Math.max(60,Math.min(800,intent.getIntExtra("body",180)));}
  public void onDestroy(){}
  public int getCount(){return valid?7:0;}
  public RemoteViews getViewAt(int position){if(!valid||position<0||position>=7)return null;LocalDate day=date.plusDays(position);List<WeekTable.Item> items=WidgetDayTable.items(WidgetDayService.this,day);RemoteViews row=new RemoteViews(getPackageName(),R.layout.rain_widget_day);row.setTextViewText(R.id.widget_day_card_date,day.format(DateTimeFormatter.ofPattern("M月d日 E",Locale.CHINA))+" · "+items.size()+"项");row.setImageViewBitmap(R.id.widget_day_card_grid,WidgetDayTable.draw(WidgetDayService.this,day,items,width,body,now,2));row.setContentDescription(R.id.widget_day_card_grid,WidgetDayTable.description(day,items,now));row.setOnClickFillInIntent(R.id.widget_day_card,new Intent().putExtra("widgetDay",day.toEpochDay()));return row;}
  public RemoteViews getLoadingView(){RemoteViews row=new RemoteViews(getPackageName(),R.layout.rain_widget_row);row.setTextViewText(R.id.widget_course_title,"正在显示课表…");row.setTextViewText(R.id.widget_course_detail,"本机读取");return row;}
  public int getViewTypeCount(){return 2;}
  public long getItemId(int position){return date==null?position:date.plusDays(position).toEpochDay();}
  public boolean hasStableIds(){return true;}
 };}
}
