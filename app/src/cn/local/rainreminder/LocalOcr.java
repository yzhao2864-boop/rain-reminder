package cn.local.rainreminder;
import android.content.*;import android.graphics.*;import com.googlecode.tesseract.android.TessBaseAPI;import java.io.*;import java.util.*;
final class LocalOcr {
 interface Progress {void update(String text);}
 static final class Block {int day,first,last,confidence;String text;Rect rect;boolean hardBoundary;}
 static float saturation(int color){int max=Math.max(Color.red(color),Math.max(Color.green(color),Color.blue(color))),min=Math.min(Color.red(color),Math.min(Color.green(color),Color.blue(color)));return max==0?0:(max-min)/(float)max;}
 static List<Block> blocks(Bitmap image,Rect grid,int rows){return blocks(image,grid,rows,grid.height()/(float)rows);}
 static List<Block> blocks(Bitmap image,Rect grid,int rows,float rowHeight){
  List<Block> result=new ArrayList<>();float width=grid.width()/7f,height=rowHeight;
  int[] bins=new int[4096];for(int y=grid.top;y<grid.bottom;y+=4)for(int x=grid.left;x<grid.right;x+=4){int c=image.getPixel(x,y);if(saturation(c)<.17f&&Color.red(c)>100&&Color.green(c)>100&&Color.blue(c)>100)bins[(Color.red(c)/16)*256+(Color.green(c)/16)*16+Color.blue(c)/16]++;}int best=0;for(int n=1;n<bins.length;n++)if(bins[n]>bins[best])best=n;int background=Color.rgb((best/256)*16+8,((best/16)%16)*16+8,(best%16)*16+8);boolean touching=saturation(background)>.025f;
  for(int day=0;day<7;day++){
   int start=-1,gap=0,lastColor=0;boolean beganAtNotch=false;
   for(int y=grid.top;y<=grid.bottom;y++){
    int count=0,color=0;float strongest=0;
    if(y<grid.bottom)for(int k=1;k<=9;k++){int x=(int)(grid.left+day*width+width*k/10);int pixel=image.getPixel(Math.min(image.getWidth()-1,x),y);float sat=saturation(pixel);if(sat>.17f){count++;if(sat>strongest){strongest=sat;color=pixel;}}}
    boolean colored=count>=2;boolean notch=touching&&start>=0&&y<grid.bottom&&colored&&y-start>height*.65f&&saturation(image.getPixel((int)(grid.left+day*width+3),y))<.17f&&saturation(image.getPixel(Math.min(image.getWidth()-1,(int)(grid.left+(day+1)*width-3)),y))<.17f;
    boolean change=start>=0&&colored&&Math.abs(Color.red(color)-Color.red(lastColor))+Math.abs(Color.green(color)-Color.green(lastColor))+Math.abs(Color.blue(color)-Color.blue(lastColor))>65;
    if(start>=0&&(change||notch||!colored&&++gap>=2||y==grid.bottom)){
     int bottom=change||notch?y:y-gap+1;if(bottom-start>=height*.4f){Block block=new Block();block.day=day;block.first=Math.max(1,Math.min(rows,Math.round((start-grid.top)/height)+1));block.last=Math.max(block.first,Math.min(rows,Math.round((bottom-grid.top)/height)));block.rect=new Rect((int)(grid.left+day*width+width*.07f),Math.min(bottom-1,start+2),(int)(grid.left+(day+1)*width-width*.07f),Math.max(start+3,bottom-2));block.hardBoundary=beganAtNotch;result.add(block);}start=-1;gap=0;beganAtNotch=notch;
    }
    if(colored){if(start<0){start=y;lastColor=color;}gap=0;if(!touching||y-start<5)lastColor=color;else lastColor=Color.rgb((Color.red(lastColor)*49+Color.red(color))/50,(Color.green(lastColor)*49+Color.green(color))/50,(Color.blue(lastColor)*49+Color.blue(color))/50);}
   }
  }
  List<Block> merged=new ArrayList<>();for(Block next:result){if(!merged.isEmpty()){Block previous=merged.get(merged.size()-1);if(!next.hardBoundary&&previous.day==next.day&&next.rect.top-previous.rect.bottom<height*.25f&&sameColor(image,previous.rect,next.rect)){previous.rect.bottom=next.rect.bottom;previous.last=next.last;continue;}}merged.add(next);}return merged;
 }
 static int background(Bitmap b,Rect r){int[] bins=new int[4096];for(int y=r.top;y<r.bottom;y+=Math.max(1,r.height()/40))for(int x=r.left;x<r.right;x+=Math.max(1,r.width()/25)){int c=b.getPixel(x,y);if(saturation(c)>.17f)bins[(Color.red(c)/16)*256+(Color.green(c)/16)*16+Color.blue(c)/16]++;}int best=0;for(int i=1;i<bins.length;i++)if(bins[i]>bins[best])best=i;return Color.rgb((best/256)*16+8,((best/16)%16)*16+8,(best%16)*16+8);}
 static boolean sameColor(Bitmap image,Rect a,Rect b){float[] x=new float[3],y=new float[3];Color.colorToHSV(background(image,a),x);Color.colorToHSV(background(image,b),y);float hue=Math.abs(x[0]-y[0]);hue=Math.min(hue,360-hue);return hue<8&&Math.abs(x[1]-y[1])<.12f&&Math.abs(x[2]-y[2])<.12f;}
 static List<Block> read(Context c,Bitmap image,Rect grid,int rows,Progress progress)throws Exception{return read(c,image,grid,rows,grid.height()/(float)rows,progress);}
 static List<Block> read(Context c,Bitmap image,Rect grid,int rows,float rowHeight,Progress progress)throws Exception{
  List<Block> blocks=blocks(image,grid,rows,rowHeight);if(blocks.isEmpty())throw new IOException("没有找到课程色块，请调整方框到周一至周日、第一节至最后一节的网格。");
  if(blocks.size()>80)throw new IOException("色块太多，请重新框选课表正文，不要包含页头或菜单。");
  try(PaddleReader reader=new PaddleReader(c)){int index=0;for(Block block:blocks){progress.update("中文识别 "+(++index)+" / "+blocks.size());PaddleReader.Read read=reader.block(image,block.rect);block.text=read.text;block.confidence=Math.round(read.confidence*100);}}blocks.removeIf(block->CourseText.isUiText(block.text));return blocks;
 }

 static TessBaseAPI open(Context c)throws Exception{File data=new File(c.getFilesDir(),"ocr/tessdata");if(!data.exists()&&!data.mkdirs())throw new IOException("无法创建本地识别目录");File model=new File(data,"chi_sim.traineddata");if(!model.exists())try(InputStream in=c.getAssets().open("tessdata/chi_sim.traineddata");FileOutputStream out=new FileOutputStream(model)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}TessBaseAPI api=new TessBaseAPI();if(!api.init(data.getParent(),"chi_sim",TessBaseAPI.OEM_LSTM_ONLY)){api.recycle();throw new IOException("中文识别模型加载失败");}return api;}
 static String header(Context c,Bitmap image,int bottom)throws Exception{Bitmap part=Bitmap.createBitmap(image,0,0,image.getWidth(),Math.max(1,Math.min(image.getHeight(),bottom)));Bitmap up=Bitmap.createScaledBitmap(part,part.getWidth()*2,part.getHeight()*2,true);TessBaseAPI api=open(c);try{api.setPageSegMode(11);api.setImage(up);return api.getUTF8Text();}finally{api.recycle();if(up!=part)up.recycle();if(part!=image)part.recycle();}}
 static Bitmap dateContrast(Bitmap image,Rect rect){Bitmap out=Bitmap.createBitmap(rect.width(),rect.height(),Bitmap.Config.ARGB_8888);int[] pixels=new int[rect.width()*rect.height()],hist=new int[256];image.getPixels(pixels,0,rect.width(),rect.left,rect.top,rect.width(),rect.height());for(int color:pixels)hist[(Color.red(color)*30+Color.green(color)*59+Color.blue(color)*11)/100]++;int total=0,background=255;for(int i=0;i<256;i++){total+=hist[i];if(total>=pixels.length*.8){background=i;break;}}for(int i=0;i<pixels.length;i++){int color=pixels[i],gray=(Color.red(color)*30+Color.green(color)*59+Color.blue(color)*11)/100;gray=Math.max(0,Math.min(255,255+(gray-background)*5));pixels[i]=Color.rgb(gray,gray,gray);}out.setPixels(pixels,0,rect.width(),0,0,rect.width(),rect.height());return out;}
 static String columnDates(Context c,Bitmap image,Rect grid)throws Exception{String[] texts={"","","","","","",""};int top=Math.max(0,grid.top-Math.max(40,(int)(image.getHeight()*.055)));if(grid.top<=top)return "";try(PaddleReader reader=new PaddleReader(c)){for(int day=0;day<7;day++){int left=grid.left+grid.width()*day/7,right=grid.left+grid.width()*(day+1)/7;Bitmap part=dateContrast(image,new Rect(left,top,right,grid.top));try{PaddleReader.Read read=reader.block(part,new Rect(0,0,part.getWidth(),part.getHeight()));if(read.confidence>=.65f)texts[day]=read.text;}finally{part.recycle();}}}java.time.LocalDate week=ScheduleDates.columns(texts,java.time.LocalDate.now());return week==null?"":week.toString();}
 static java.time.LocalDate monday(String text,java.time.LocalDate today){return ScheduleDates.read(text,today);}
 static boolean semesterOverview(Bitmap image,String header){if(!header.replaceAll("\\s+","").contains("学期课表"))return false;int left=0,right=0;for(int y=(int)(image.getHeight()*.11);y<image.getHeight()*.18;y+=2)for(int x=(int)(image.getWidth()*.2);x<image.getWidth()*.8;x+=2){int c=image.getPixel(x,y);if(Color.blue(c)>Color.red(c)*1.3&&Color.blue(c)>Color.green(c)*1.1&&saturation(c)>.4){if(x<image.getWidth()*.5)left++;else right++;}}return right>100&&right>left*2;}
}
