package cn.local.rainreminder;
import android.app.*;import android.appwidget.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;

public class WidgetSettingsActivity extends Activity {
 int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID,selected;LinearLayout preview;RadioGroup choices;TextView description;
 public void onCreate(Bundle b){super.onCreate(b);setResult(RESULT_CANCELED);widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID){AppWidgetProviderInfo info=AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);if(info==null||!new ComponentName(this,RainWidget.class).equals(info.provider)){finish();return;}}
  selected=b==null?(widgetId>0&&!WidgetOptions.prefs(this).contains("widget_"+widgetId)?-1:WidgetOptions.mode(this,widgetId)):b.getInt("selected");show();
 }
 void show(){LinearLayout root=Ui.screen(this,widgetId>0?"小组件显示":"桌面小组件");LinearLayout card=Ui.card(this,root);card.addView(Ui.text(this,widgetId>0?"显示内容":"默认显示内容",21));card.addView(Ui.hint(this,widgetId>0?"每个桌面组件可以单独设置。":"未单独设置的组件会使用这里的选项。"));choices=new RadioGroup(this);card.addView(choices);
  if(widgetId>0)addChoice(-1,"跟随默认设置");
  addChoice(WidgetOptions.HOMEWORK_FIRST,"作业优先");
  addChoice(WidgetOptions.SCHEDULE_FIRST,"课表优先");
  addChoice(WidgetOptions.HOMEWORK_ONLY,"只显示作业");
  addChoice(WidgetOptions.SCHEDULE_ONLY,"只显示课表");
  description=Ui.hint(this,"");card.addView(description);
  choices.setOnCheckedChangeListener((group,id)->{View choice=group.findViewById(id);selected=(Integer)choice.getTag();renderPreview();});
  root.addView(Ui.hint(this,"预览 · 桌面大小较小时优先显示主要内容"));preview=Ui.card(this,root);renderPreview();
  LinearLayout actions=Ui.column(this);actions.setPadding(Ui.dp(this,20),Ui.dp(this,6),Ui.dp(this,20),Ui.dp(this,12));Ui.page(root).addView(actions);Ui.primary(this,actions,"保存设置",()->{WidgetOptions.save(this,widgetId,selected);Intent result=new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);setResult(RESULT_OK,result);Toast.makeText(this,"小组件显示已更新",Toast.LENGTH_SHORT).show();finish();});
  if(widgetId<=0)Ui.link(this,root,"添加到桌面",()->RainWidget.pin(this));
  root.addView(Ui.hint(this,"隐藏课表只影响组件显示，已有课程和提醒仍保留。点组件右上角“⋯”可单独调整。"));
 }
 void addChoice(int mode,String title){RadioButton radio=new RadioButton(this);radio.setId(View.generateViewId());radio.setTag(mode);radio.setText(title);radio.setTextSize(14);radio.setTypeface(Ui.font(this));radio.setTextColor(Ui.INK);radio.setMinHeight(Ui.dp(this,40));radio.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,4));choices.addView(radio);radio.setChecked(mode==selected);}
 void renderPreview(){if(preview==null)return;preview.removeAllViews();int mode=WidgetOptions.valid(selected)?selected:WidgetOptions.mode(this,-1);description.setText(mode==WidgetOptions.HOMEWORK_FIRST?"主要显示待交作业，底部显示下一项课表安排。":mode==WidgetOptions.SCHEDULE_FIRST?"主要显示课程和日程，底部显示作业摘要。":mode==WidgetOptions.HOMEWORK_ONLY?"隐藏课表，按截止时间从近到远显示作业。":"隐藏作业，显示接下来的课程和日程。");View view=RainWidget.views(this,widgetId,180,System.currentTimeMillis(),mode).apply(this,preview);disable(view);preview.addView(view,new LinearLayout.LayoutParams(-1,Ui.dp(this,180)));}
 void disable(View view){view.setOnClickListener(null);view.setClickable(false);view.setFocusable(false);if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++)disable(((ViewGroup)view).getChildAt(n));}
 protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("selected",selected);}
}
