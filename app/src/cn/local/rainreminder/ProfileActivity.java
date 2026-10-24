package cn.local.rainreminder;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;import android.graphics.BitmapFactory;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Official defaults and optional local overrides; no writes to the official profile. */
public class ProfileActivity extends Activity {
 public void onCreate(Bundle state){super.onCreate(state);}
 protected void onResume(){super.onResume();show();if(Store.prefs(this).getLong("last",0)>0)AccountProfile.refresh(this,false,message->{if(!isFinishing()&&!isDestroyed())Ui.keepScroll(this,this::show);});}
 void account(String action){startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("accountAction",action));finish();}
 String nickname(){return Store.prefs(this).getString("profileNickname","").trim();}
 void show(){
  LinearLayout root=Ui.screen(this,"我的",false);Ui.tabs(this,root,2);
  long last=Store.prefs(this).getLong("last",0);boolean saved=last>0;
  LinearLayout profile=Ui.card(this,root);profile.setPadding(Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18));
  LinearLayout identity=Ui.pair(this,profile);identity.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout avatar=Ui.column(this);avatar.setGravity(Gravity.CENTER);avatar.setBackground(Ui.bg(this,0xFFE1EEE8));avatar.setClipToOutline(true);java.io.File photo=AccountProfile.avatar(this,true);if(!photo.exists())photo=AccountProfile.avatar(this,false);android.graphics.Bitmap image=photo.exists()?BitmapFactory.decodeFile(photo.getPath()):null;if(image!=null){ImageView picture=new ImageView(this);picture.setImageBitmap(image);picture.setScaleType(ImageView.ScaleType.CENTER_CROP);avatar.addView(picture,new LinearLayout.LayoutParams(-1,-1));}else avatar.addView(new Ui.NavIcon(this,2,true),new LinearLayout.LayoutParams(Ui.dp(this,34),Ui.dp(this,34)));identity.addView(avatar,new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,52)));
  LinearLayout detail=Ui.column(this);detail.setPadding(Ui.dp(this,14),0,0,0);identity.addView(detail,new LinearLayout.LayoutParams(0,-2,1));String name=AccountProfile.name(this);TextView title=Ui.text(this,name.isEmpty()?(saved?"雨课堂用户":"欢迎使用"):name,21);title.setMaxLines(2);detail.addView(title);detail.addView(Ui.hint(this,(AccountProfile.custom(this)?"自定义资料":Store.prefs(this).contains("rainProfileId")?"雨课堂资料":"登录后同步雨课堂资料")+" · 编辑  ›"));profile.setContentDescription("编辑个人资料");profile.setFocusable(true);profile.setOnClickListener(v->profileMenu());

  LinearLayout account=Ui.card(this,root);account.addView(Ui.text(this,"长江雨课堂账号",17));
  TextView state=Ui.text(this,saved?"已保存本机登录状态":"尚无成功检查记录",14);state.setTextColor(Ui.GREEN);account.addView(state);
  if(saved)account.addView(Ui.hint(this,"最近成功检查："+new SimpleDateFormat("MM月dd日 HH:mm",Locale.CHINA).format(new Date(last))));
  else account.addView(Ui.hint(this,"登录自己的账号后，可自动检查并提醒作业。"));
  LinearLayout actions=Ui.pair(this,account);Ui.button(this,Ui.half(this,actions,false),saved?"重新确认登录":"登录长江雨课堂",()->account("login"));Ui.link(this,Ui.half(this,actions,true),"退出本机账号",this::confirmLogout);

  LinearLayout settings=Ui.card(this,root);settings.addView(Ui.text(this,"设置与服务",17));
  Ui.link(this,settings,"提醒与检查设置  ›",()->startActivity(new Intent(this,SettingsActivity.class)));
  Ui.link(this,settings,"添加桌面小组件  ›",()->RainWidget.pin(this));
  Ui.link(this,settings,"应用更新  ›",()->startActivity(new Intent(this,UpdateActivity.class)));
  TextView footer=Ui.hint(this,"待课 "+AppUpdates.version(this)+" · 测试版\n非官方应用 · 课表和作业数据仅存本机");footer.setGravity(Gravity.CENTER);footer.setPadding(0,Ui.dp(this,22),0,0);root.addView(footer);
 }
 void profileMenu(){new AlertDialog.Builder(this).setTitle("头像与名字").setItems(new String[]{"修改名字","从相册更换头像","恢复雨课堂头像和名字","同步雨课堂资料"},(d,n)->{if(n==0)editNickname();else if(n==1){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),41);}catch(Exception e){Toast.makeText(this,"无法打开图片选择器",1).show();}}else if(n==2)new AlertDialog.Builder(this).setTitle("恢复雨课堂资料？").setMessage("移除本机自定义头像和名字，使用最近同步的雨课堂资料。").setPositiveButton("恢复",(dialog,which)->{AccountProfile.reset(this);Ui.keepScroll(this,this::show);}).setNegativeButton("取消",null).show();else syncProfile();}).show();}
 void syncProfile(){Toast.makeText(this,"正在同步雨课堂资料…",0).show();AccountProfile.refresh(this,true,message->{if(!isFinishing()&&!isDestroyed()){Ui.keepScroll(this,this::show);Toast.makeText(this,message,1).show();}});}
 void editNickname(){LinearLayout form=Ui.column(this);form.setPadding(Ui.dp(this,20),Ui.dp(this,8),Ui.dp(this,20),0);EditText name=Ui.field(this,form,"名字",AccountProfile.name(this),InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(20)});form.addView(Ui.hint(this,"只修改本应用显示的名字，不修改雨课堂资料。留空使用雨课堂名字。"));new AlertDialog.Builder(this).setTitle("修改名字").setView(form).setPositiveButton("保存",(d,n)->{Store.prefs(this).edit().putString("profileNickname",name.getText().toString().trim()).apply();Ui.keepScroll(this,this::show);}).setNegativeButton("取消",null).show();}
 protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=41||result!=RESULT_OK||data==null||data.getData()==null)return;final int token=AccountProfile.generation;android.net.Uri uri=data.getData();new Thread(()->{String message="头像已修改";try{AccountProfile.save(this,getContentResolver().openInputStream(uri),true,token);}catch(Exception e){message="头像未修改："+(e.getMessage()==null?"无法读取图片":e.getMessage());}final String text=message;runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){Ui.keepScroll(this,this::show);Toast.makeText(this,text,1).show();}});},"custom-profile-avatar").start();}
 void confirmLogout(){new AlertDialog.Builder(this).setTitle("退出本机账号？").setMessage("清除登录和作业缓存，停止每日检查。本地课表、日程及其提醒保留。").setPositiveButton("退出账号",(d,n)->account("logout")).setNegativeButton("取消",null).show();}
}
