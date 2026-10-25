package cn.local.rainreminder;
import android.content.*;import android.app.*;import android.appwidget.*;import android.graphics.*;import android.text.*;import android.view.View;import android.widget.RemoteViews;import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;

/** Raster drawing is confined to our own timetable; widget controls remain native. */
final class WidgetDayTable {
 static final String NAVIGATE="cn.local.rainreminder.WIDGET_DAY_NAVIGATE";
 static PendingIntent navigation(Context c,int id,int delta){Intent intent=new Intent(c,RainWidget.class).setAction(NAVIGATE).setData(android.net.Uri.parse("rainwidget://day/"+id+"/"+delta)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra("delta",delta);return PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static List<WeekTable.Item> items(Context c,LocalDate date){LocalDate monday=date.minusDays(date.getDayOfWeek().getValue()-1);List<WeekTable.Item> out=new ArrayList<>();for(WeekTable.Item item:WeekTable.occurrences(Agenda.all(c),monday))if(item.day==date.getDayOfWeek().getValue()-1)out.add(item);return out;}
 static void bind(Context c,RemoteViews views,int id,int height,long now,boolean secondary){
  LocalDate date=WidgetOptions.date(c,id,now);List<WeekTable.Item> items=items(c,date);PendingIntent open=RainWidget.open(c,AgendaActivity.class,7620+id,date.toEpochDay());
  views.setTextViewText(R.id.widget_heading,"单日课表");views.setViewVisibility(R.id.widget_date,View.GONE);views.setViewVisibility(R.id.widget_rows,View.GONE);views.setViewVisibility(R.id.widget_day_nav,View.VISIBLE);views.setViewVisibility(R.id.widget_day_grid,View.VISIBLE);
  views.setTextViewText(R.id.widget_day_title,date.format(DateTimeFormatter.ofPattern("M月d日 E",Locale.CHINA))+" · "+items.size()+"项");
  views.setOnClickPendingIntent(R.id.widget_day_previous,navigation(c,id,-1));views.setOnClickPendingIntent(R.id.widget_day_next,navigation(c,id,1));views.setOnClickPendingIntent(R.id.widget_day_today,navigation(c,id,0));views.setOnClickPendingIntent(R.id.widget_day_title,open);views.setOnClickPendingIntent(R.id.widget_day_grid,open);views.setOnClickPendingIntent(R.id.widget_heading,open);views.setOnClickPendingIntent(R.id.widget_root,open);
  int width=AppWidgetManager.getInstance(c).getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300);int body=Math.max(45,height-88-(secondary?91:0));views.setImageViewBitmap(R.id.widget_day_grid,draw(c,date,items,Math.max(160,width-24),body,now));
  StringBuilder description=new StringBuilder(date.toString()+"，"+(items.isEmpty()?"这一天没有课程":"共"+items.size()+"项"));for(WeekTable.Item item:items)description.append("；").append(item.event.optString("title")).append("，").append(WidgetData.detail(item,now));views.setContentDescription(R.id.widget_day_grid,description.toString());
 }
 static Bitmap draw(Context c,LocalDate date,List<WeekTable.Item> items,int width,int height,long now){
  float scale=Math.max(1,Math.min(4,c.getResources().getDisplayMetrics().density));Bitmap bitmap=Bitmap.createBitmap(Math.max(1,Math.round(width*scale)),Math.max(1,Math.round(height*scale)),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.scale(scale,scale);canvas.drawColor(0xFFF8FAF9);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(c.getResources().getFont(R.font.ui));
  if(items.isEmpty()){paint.setColor(Ui.MUTED);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(14);LocalDate today=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();canvas.drawText(date.equals(today)?"今天没有课程":"这一天没有课程",width/2f,height/2f,paint);return bitmap;}
  List<LocalTime[]> periods=ImportActivity.parseTimes(ImportActivity.periodTimes(c));

  int first=0,last=periods.size();float row=height/(float)(last-first),axis=row>=22?40:25;
  for(int p=first;p<last;p++){float y=(p-first)*row;paint.setColor(p%2==0?0xFFF2F6F4:0xFFFAFBFB);canvas.drawRect(axis,y,width,y+row,paint);paint.setColor(0xFFDEE7E2);paint.setStrokeWidth(.6f);canvas.drawLine(axis,y,width,y,paint);paint.setColor(Ui.MUTED);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.min(11,Math.max(7,row*.65f)));canvas.drawText(String.valueOf(p+1),axis/2,y+Math.min(row-1,row*.6f),paint);if(row>=24){paint.setTextSize(7);canvas.drawText(periods.get(p)[0].toString(),axis/2,y+row-3,paint);}}
  for(WeekTable.Item item:items){float top=(WeekTable.position(item.start,periods)-first)*row,bottom=(Math.max(WeekTable.position(item.start,periods)+.5f,endPosition(item,periods))-first)*row;top=Math.max(0,top);bottom=Math.min(height,bottom);if(bottom<=top)continue;
   int peers=1,index=0;for(WeekTable.Item other:items)if(other!=item&&other.start<item.end&&other.end>item.start){peers++;if(other.start<item.start||other.start==item.start&&other.event.optString("id").compareTo(item.event.optString("id"))<0)index++;}
   float column=(width-axis)/peers,x=axis+index*column+2,w=column-4,h=bottom-top-2;paint.setColor(color(item,now));canvas.drawRoundRect(new RectF(x,top+1,x+w,bottom-1),5,5,paint);
   TextPaint text=new TextPaint(Paint.ANTI_ALIAS_FLAG);text.setTypeface(Typeface.create(c.getResources().getFont(R.font.ui),Typeface.BOLD));text.setColor(Ui.INK);text.setTextSize(h<32?10:12);int lines=h>=52?2:1;canvas.save();canvas.clipRect(x+4,top+2,x+w-3,bottom-1);canvas.translate(x+5,top+3);String title=item.event.optString("title");StaticLayout layout=StaticLayout.Builder.obtain(title,0,title.length(),text,Math.max(1,(int)w-9)).setIncludePad(false).setMaxLines(lines).setEllipsize(TextUtils.TruncateAt.END).build();layout.draw(canvas);
   if(h-layout.getHeight()>=13){text.setTypeface(c.getResources().getFont(R.font.ui));text.setTextSize(9);String detail=WidgetData.detail(item,now);canvas.translate(0,layout.getHeight()+1);StaticLayout meta=StaticLayout.Builder.obtain(detail,0,detail.length(),text,Math.max(1,(int)w-9)).setIncludePad(false).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).build();meta.draw(canvas);}canvas.restore();
  }
  LocalTime clock=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime();boolean inPeriod=false;for(LocalTime[] period:periods)if(!clock.isBefore(period[0])&&!clock.isAfter(period[1]))inPeriod=true;
  if(inPeriod&&date.equals(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate())){float position=WeekTable.position(now,periods);if(position>=first&&position<=last){paint.setColor(Ui.GREEN);paint.setStrokeWidth(1.5f);canvas.drawLine(axis,(position-first)*row,width,(position-first)*row,paint);}}
  return bitmap;
 }
 static float endPosition(WeekTable.Item item,List<LocalTime[]> periods){return Instant.ofEpochMilli(item.end).atZone(ZoneId.systemDefault()).toLocalDate().isAfter(Instant.ofEpochMilli(item.start).atZone(ZoneId.systemDefault()).toLocalDate())?periods.size():WeekTable.position(item.end,periods);}
 static int color(WeekTable.Item item,long now){int color=WeekTable.color(item.event);if(item.end<now||!item.event.optBoolean("enabled",true))return Color.rgb((Color.red(color)+255)/2,(Color.green(color)+255)/2,(Color.blue(color)+255)/2);return color;}
}
