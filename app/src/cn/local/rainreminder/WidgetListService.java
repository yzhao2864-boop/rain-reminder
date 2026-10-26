package cn.local.rainreminder;
import android.content.*;import android.widget.*;import java.util.*;

/** Android 9–11 collection adapter; newer systems receive inline collection items. */
public class WidgetListService extends RemoteViewsService {
 public RemoteViewsFactory onGetViewFactory(Intent intent){final boolean homework=intent.getBooleanExtra("homework",true),menu=intent.getBooleanExtra("menu",false);final int id=intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,-1);return new RemoteViewsFactory(){List<RemoteViews> items=new ArrayList<>();public void onCreate(){onDataSetChanged();}public void onDataSetChanged(){items=menu?WidgetMenu.rows(WidgetListService.this,id):WidgetLists.rows(WidgetListService.this,homework,System.currentTimeMillis(),id);}public void onDestroy(){items.clear();}public int getCount(){return items.size();}public RemoteViews getViewAt(int position){return position>=0&&position<items.size()?items.get(position):null;}public RemoteViews getLoadingView(){return null;}public int getViewTypeCount(){return 1;}public long getItemId(int position){return position;}public boolean hasStableIds(){return false;}};}
}
