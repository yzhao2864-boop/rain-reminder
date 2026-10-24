package cn.local.rainreminder;
import android.content.Context;import android.view.*;import android.widget.ScrollView;import java.util.function.IntConsumer;
/** Lock the intended direction early, then accept a short swipe or a quick flick. */
final class WeekScroll extends ScrollView {
 static final int NONE=0,HORIZONTAL=1,VERTICAL=2;
 final IntConsumer change;final float density;final int slop;final float minVelocity,maxVelocity;
 float x,y;int axis,pointer;boolean multi;VelocityTracker velocity;
 WeekScroll(Context c,IntConsumer action){super(c);change=action;density=getResources().getDisplayMetrics().density;ViewConfiguration config=ViewConfiguration.get(c);slop=config.getScaledTouchSlop();minVelocity=Math.max(config.getScaledMinimumFlingVelocity(),400*density);maxVelocity=config.getScaledMaximumFlingVelocity();setFillViewport(true);setVerticalScrollBarEnabled(false);}
 @Override public boolean dispatchTouchEvent(MotionEvent e){int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){release();velocity=VelocityTracker.obtain();x=e.getX();y=e.getY();pointer=e.getPointerId(0);axis=NONE;multi=false;}
  if(velocity!=null)velocity.addMovement(e);if(e.getPointerCount()>1||action==MotionEvent.ACTION_POINTER_DOWN)multi=true;
  int index=e.findPointerIndex(pointer);float dx=index<0?0:e.getX(index)-x,dy=index<0?0:e.getY(index)-y;
  if(action==MotionEvent.ACTION_MOVE&&axis==NONE&&!multi){float ax=Math.abs(dx),ay=Math.abs(dy);if(ax>slop&&ax>1.15f*ay){axis=HORIZONTAL;MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.onTouchEvent(cancel);cancel.recycle();if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);}else if(ay>slop&&(ay>1.15f*ax||Math.max(ax,ay)>2*slop))axis=VERTICAL;}
  int direction=0;if(action==MotionEvent.ACTION_UP&&axis==HORIZONTAL&&!multi&&index>=0){float ax=Math.abs(dx),ay=Math.abs(dy);boolean distance=ax>=24*density&&ax>1.15f*ay,fling=false;if(velocity!=null){velocity.computeCurrentVelocity(1000,maxVelocity);float vx=velocity.getXVelocity(pointer),vy=velocity.getYVelocity(pointer);fling=ax>=12*density&&Math.abs(vx)>=minVelocity&&Math.abs(vx)>1.2f*Math.abs(vy)&&vx*dx>0;}if(distance||fling)direction=dx<0?1:-1;}
  boolean handled=super.dispatchTouchEvent(e);if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){release();axis=NONE;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}if(direction!=0)change.accept(direction);return handled;
 }
 @Override public boolean onInterceptTouchEvent(MotionEvent e){if(axis==HORIZONTAL)return true;if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&axis==NONE&&!multi)return false;return super.onInterceptTouchEvent(e);}
 @Override public boolean onTouchEvent(MotionEvent e){return axis==HORIZONTAL||super.onTouchEvent(e);}
 void release(){if(velocity!=null){velocity.recycle();velocity=null;}}
 @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
}
