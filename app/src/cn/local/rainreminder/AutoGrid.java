package cn.local.rainreminder;
import android.graphics.*;import java.util.*;

/** Locates seven-column coloured weekly grids without requiring touch alignment. */
final class AutoGrid {
 static final class Result {Rect grid;int rows;boolean reliable;float rowHeight;Result(Rect g,int n,boolean ok){this(g,n,ok,g.height()/(float)n);}Result(Rect g,int n,boolean ok,float pitch){grid=g;rows=n;reliable=ok;rowHeight=pitch;}}
 static Result locate(Bitmap image,int preferredRows){
  int scale=Math.max(1,(int)Math.ceil(image.getWidth()/600.0)),w=image.getWidth()/scale,h=image.getHeight()/scale;
  Bitmap small=Bitmap.createScaledBitmap(image,w,h,true);int[] pixels=new int[w*h];small.getPixels(pixels,0,w,0,0,w,h);if(small!=image)small.recycle();
  int step=3,nx=w/step,ny=h/step;boolean[] seen=new boolean[nx*ny],color=new boolean[nx*ny];int[] colors=new int[nx*ny],queue=new int[nx*ny];List<Rect> boxes=new ArrayList<>();
  for(int y=0;y<ny;y++)for(int x=0;x<nx;x++){int k=y*nx+x,c=pixels[y*step*w+x*step];colors[k]=c;color[k]=LocalOcr.saturation(c)>.18f&&Math.max(Color.red(c),Math.max(Color.green(c),Color.blue(c)))>80;}
  for(int index=0;index<colors.length;index++){
   if(seen[index]||!color[index])continue;int seed=colors[index],head=0,tail=1,left=index%nx,right=left,top=index/nx,bottom=top;queue[0]=index;seen[index]=true;
   while(head<tail){int at=queue[head++],x=at%nx,y=at/nx;left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
    for(int dir=0;dir<4;dir++){int xx=x+(dir==0?-1:dir==1?1:0),yy=y+(dir==2?-1:dir==3?1:0);if(xx<0||xx>=nx||yy<0||yy>=ny)continue;int k=yy*nx+xx;if(seen[k]||!color[k])continue;int c=colors[k];if(Math.abs(Color.red(c)-Color.red(seed))+Math.abs(Color.green(c)-Color.green(seed))+Math.abs(Color.blue(c)-Color.blue(seed))>110)continue;seen[k]=true;queue[tail++]=k;}
   }
   int bw=(right-left+1)*step,bh=(bottom-top+1)*step;
   if(bw>w*.055&&bw<w*.19&&bh>w*.12&&top*step>h*.12&&tail/(float)((right-left+1)*(bottom-top+1))>.38f)boxes.add(new Rect(left*step,top*step,(right+1)*step,(bottom+1)*step));
  }
  if(boxes.size()<4)return fallback(image,preferredRows);
  List<Double> widths=new ArrayList<>(),centers=new ArrayList<>();int top=h,maxBottom=0;for(Rect r:boxes){widths.add((double)r.width());centers.add(r.exactCenterX()*1.0);top=Math.min(top,r.top);maxBottom=Math.max(maxBottom,r.bottom);}double width=median(widths);Collections.sort(centers);List<Double> columns=new ArrayList<>();
  List<Double> cluster=new ArrayList<>();for(double v:centers){if(!cluster.isEmpty()&&v-cluster.get(cluster.size()-1)>width*.45){columns.add(median(cluster));cluster.clear();}cluster.add(v);}if(!cluster.isEmpty())columns.add(median(cluster));
  List<Double> pitches=new ArrayList<>();for(int i=1;i<columns.size();i++){double d=columns.get(i)-columns.get(i-1);int span=Math.max(1,(int)Math.round(d/(width*1.06)));pitches.add(d/span);}double pitch=pitches.isEmpty()?width*1.06:median(pitches);top=Math.max(0,top-2);
  double left=columns.get(0)-pitch/2;while(left>w*.18)left-=pitch;left=Math.max(0,left);double right=Math.min(w,left+pitch*7);
  double nominal=(maxBottom-top)/(double)preferredRows,best=Double.MAX_VALUE,rowHeight=nominal;
  for(double unit=Math.max(12,nominal*.8);unit<=nominal*1.4;unit+=.1){double error=0;int count=0;for(Rect r:boxes){double t=(r.top-top)/unit;error+=Math.min(.25,Math.abs(t-Math.rint(t)));count++;if(r.bottom<h*.94){double b=(r.bottom-top)/unit;error+=Math.min(.25,Math.abs(b-Math.rint(b)));count++;}}error/=Math.max(1,count);if(error<best){best=error;rowHeight=unit;}}
  int cap=h;for(int y=maxBottom;y<h;y++){int white=0,total=0;for(int x=(int)(w*.06);x<w*.98;x+=4){int c=pixels[y*w+x];if(Color.red(c)>235&&Color.green(c)>235&&Color.blue(c)>235)white++;total++;}if(white>total*.88){cap=y;break;}}
  int rows=Math.min(16,Math.max(1,(int)Math.ceil((cap-top)/rowHeight-.1)));int bottom=Math.min(cap,(int)Math.round(top+rows*rowHeight));
  boolean reliable=columns.size()>=2&&best<.10&&rows>=6&&rows<=16&&right-left>w*.65&&bottom-top>h*.35;
  Rect grid=new Rect(Math.max(0,(int)(left*scale)),top*scale,Math.min(image.getWidth(),(int)Math.round(right*scale)),Math.min(image.getHeight(),bottom*scale));
  // The final row may be cut off by the navigation bar: keep fitted pitch, not visible height / rows.
  return reliable?new Result(grid,rows,true,(float)(rowHeight*scale)):fallback(image,preferredRows);
 }
 static double median(List<Double> v){List<Double> a=new ArrayList<>(v);Collections.sort(a);int n=a.size();return n%2==1?a.get(n/2):(a.get(n/2-1)+a.get(n/2))/2;}
 static Result fallback(Bitmap b,int rows){return new Result(new Rect((int)(b.getWidth()*.09),(int)(b.getHeight()*.16),(int)(b.getWidth()*.985),(int)(b.getHeight()*.91)),rows,false);}
}
