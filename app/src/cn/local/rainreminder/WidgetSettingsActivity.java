package cn.local.rainreminder;
import android.app.*;import android.appwidget.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;

public class WidgetSettingsActivity extends Activity {
 int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID,selected,selectedStyle;LinearLayout preview,styleCard;RadioGroup choices,styles;TextView description;
 public void onCreate(Bundle b){super.onCreate(b);setResult(RESULT_CANCELED);widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID){AppWidgetProviderInfo info=AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);if(info==null||!new ComponentName(this,RainWidget.class).equals(info.provider)){finish();return;}}
  selected=b==null?(widgetId>0&&!WidgetOptions.prefs(this).contains("widget_"+widgetId)?-1:WidgetOptions.mode(this,widgetId)):b.getInt("selected");selectedStyle=b==null?(widgetId>0&&!WidgetOptions.prefs(this).contains("widget_style_"+widgetId)?-1:WidgetOptions.style(this,widgetId)):b.getInt("style");show();
 }
 void show(){LinearLayout root=Ui.screen(this,widgetId>0?"小组件显示":"桌面小组件");LinearLayout card=Ui.card(this,root);card.addView(Ui.text(this,widgetId>0?"显示内容":"默认显示内容",21));card.addView(Ui.hint(this,widgetId>0?"每个桌面组件可以单独设置。":"未单独设置的组件会使用这里的选项。"));choices=new RadioGroup(this);card.addView(choices);
  if(widgetId>0)addChoice(-1,"跟随默认设置");
  addChoice(WidgetOptions.HOMEWORK_FIRST,"作业优先");
  addChoice(WidgetOptions.SCHEDULE_FIRST,"课表优先");
  addChoice(WidgetOptions.HOMEWORK_ONLY,"只显示作业");
  addChoice(WidgetOptions.SCHEDULE_ONLY,"只显示课表");
  description=Ui.hint(this,"");card.addView(description);
  choices.setOnCheckedChangeListener((group,id)->{View choice=group.findViewById(id);selected=(Integer)choice.getTag();renderPreview();});
  styleCard=Ui.card(this,root);styleCard.addView(Ui.text(this,"课表样式",18));styles=new RadioGroup(this);styleCard.addView(styles);if(widgetId>0)addStyle(-1,"跟随默认样式");addStyle(0,"每日列表 · 可切换日期");addStyle(1,"周课表 · 可切换周次");styleCard.addView(Ui.hint(this,"周一到周日横向排列，保留课程颜色。上下滑动查看下午和晚上的课程。"));styles.setOnCheckedChangeListener((group,id)->{selectedStyle=(Integer)group.findViewById(id).getTag();renderPreview();});
  root.addView(Ui.hint(this,"预览 · 桌面大小较小时优先显示主要内容"));preview=Ui.card(this,root);renderPreview();
  LinearLayout actions=Ui.column(this);actions.setPadding(Ui.dp(this,20),Ui.dp(this,6),Ui.dp(this,20),Ui.dp(this,12));Ui.page(root).addView(actions);Ui.primary(this,actions,"保存设置",()->{WidgetOptions.saveStyle(this,widgetId,selectedStyle);WidgetOptions.save(this,widgetId,selected);Intent result=new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);setResult(RESULT_OK,result);Toast.makeText(this,"小组件显示已更新",Toast.LENGTH_SHORT).show();finish();});
  if(widgetId<=0)Ui.link(this,root,"添加到桌面",()->RainWidget.pin(this));
  root.addView(Ui.hint(this,"隐藏课表只影响组件显示，已有课程和提醒仍保留。点组件右上角“⋯”可单独调整。"));
 }
 void addChoice(int mode,String title){RadioButton radio=new RadioButton(this);radio.setId(View.generateViewId());radio.setTag(mode);radio.setText(title);radio.setTextSize(14);radio.setTypeface(Ui.font(this));radio.setTextColor(Ui.INK);radio.setMinHeight(Ui.dp(this,40));radio.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,4));choices.addView(radio);radio.setChecked(mode==selected);}
 void addStyle(int value,String title){RadioButton radio=new RadioButton(this);radio.setId(View.generateViewId());radio.setTag(value);radio.setText(title);radio.setTextSize(14);radio.setTextColor(Ui.INK);radio.setTypeface(Ui.font(this));styles.addView(radio);radio.setChecked(value==selectedStyle);}
 void renderPreview(){if(preview==null)return;preview.removeAllViews();int mode=WidgetOptions.valid(selected)?selected:WidgetOptions.mode(this,-1),style=selectedStyle>=0?selectedStyle:WidgetOptions.style(this,-1);boolean table=!WidgetOptions.homeworkFirst(mode)&&style==1;styleCard.setVisibility(WidgetOptions.homeworkFirst(mode)?View.GONE:View.VISIBLE);description.setText(mode==WidgetOptions.HOMEWORK_FIRST?"作业和底部课程列表可分别上下滑动。":mode==WidgetOptions.HOMEWORK_ONLY?"隐藏课表，按截止时间从近到远显示作业。":table?"周课表按日期分列，可切换周次，纵向查看课程。":mode==WidgetOptions.SCHEDULE_FIRST?"课程列表可切换日期；底部作业独立上下滑动。":"隐藏作业，显示接下来的课程和日程。");int height=table?360:300;android.widget.RemoteViews views=RainWidget.views(this,widgetId,height,System.currentTimeMillis(),mode,style);if(table)WidgetDayTable.preview(this,views,widgetId,height,System.currentTimeMillis(),WidgetOptions.both(mode));View view=views.apply(this,preview);disable(view);preview.addView(view,new LinearLayout.LayoutParams(-1,Ui.dp(this,height)));}
 void disable(View view){view.setOnClickListener(null);view.setClickable(false);view.setFocusable(false);if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++)disable(((ViewGroup)view).getChildAt(n));}
 protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("selected",selected);out.putInt("style",selectedStyle);}
}
