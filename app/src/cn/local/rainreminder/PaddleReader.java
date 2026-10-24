package cn.local.rainreminder;
import android.content.*;import android.graphics.*;import ai.onnxruntime.*;import java.io.*;import java.nio.*;import java.util.*;

/** PP-OCRv5 recognition on original colour lines. No image leaves the device. */
final class PaddleReader implements AutoCloseable {
 final OrtEnvironment env=OrtEnvironment.getEnvironment();final OrtSession session;final List<String> characters=new ArrayList<>();
 PaddleReader(Context c)throws Exception{
  File model=new File(c.getFilesDir(),"ocr/ppocr-v5-rec-int8-v1.onnx");model.getParentFile().mkdirs();if(!model.isFile())try(InputStream in=c.getAssets().open("ocr/rec.onnx");OutputStream out=new FileOutputStream(model)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
  characters.add("");try(BufferedReader reader=new BufferedReader(new InputStreamReader(c.getAssets().open("ocr/keys.txt"),"UTF-8"))){String line;while((line=reader.readLine())!=null)characters.add(line);}characters.add(" ");
  try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){options.setIntraOpNumThreads(2);options.setInterOpNumThreads(1);session=env.createSession(model.getAbsolutePath(),options);}
 }
 static final class Read {String text;float confidence;Read(String t,float c){text=t;confidence=c;}}
 Read line(Bitmap source)throws Exception{
  int width=Math.max(8,Math.min(1024,(int)Math.ceil(48.0*source.getWidth()/source.getHeight()))),tensorWidth=Math.max(32,width);Bitmap scaled=Bitmap.createScaledBitmap(source,width,48,true);int[] pixels=new int[width*48];scaled.getPixels(pixels,0,width,0,0,width,48);if(scaled!=source)scaled.recycle();float[] input=new float[3*48*tensorWidth];
  for(int y=0;y<48;y++)for(int x=0;x<width;x++){int color=pixels[y*width+x];input[y*tensorWidth+x]=Color.blue(color)/127.5f-1;input[48*tensorWidth+y*tensorWidth+x]=Color.green(color)/127.5f-1;input[96*tensorWidth+y*tensorWidth+x]=Color.red(color)/127.5f-1;}
  try(OnnxTensor tensor=OnnxTensor.createTensor(env,FloatBuffer.wrap(input),new long[]{1,3,48,tensorWidth});OrtSession.Result result=session.run(Collections.singletonMap("x",tensor))){OnnxTensor output=(OnnxTensor)result.get(0);long[] shape=output.getInfo().getShape();int steps=(int)shape[1],classes=(int)shape[2],last=-1,count=0;float score=0;FloatBuffer values=output.getFloatBuffer();StringBuilder text=new StringBuilder();for(int t=0;t<steps;t++){int best=0;float high=-Float.MAX_VALUE;for(int k=0;k<classes;k++){float v=values.get(t*classes+k);if(v>high){high=v;best=k;}}if(best!=0&&best!=last&&best<characters.size()){text.append(characters.get(best));score+=high;count++;}last=best;}return new Read(text.toString(),count==0?0:score/count);}
 }
 static List<Rect> lines(Bitmap cell){
  int w=cell.getWidth(),h=cell.getHeight();int[] pixels=new int[w*h];cell.getPixels(pixels,0,w,0,0,w,h);int[] red=new int[pixels.length],green=new int[pixels.length],blue=new int[pixels.length];for(int n=0;n<pixels.length;n++){red[n]=Color.red(pixels[n]);green[n]=Color.green(pixels[n]);blue[n]=Color.blue(pixels[n]);}Arrays.sort(red);Arrays.sort(green);Arrays.sort(blue);int r=red[red.length/2],g=green[green.length/2],b=blue[blue.length/2],min=Math.min(r,Math.min(g,b)),max=Math.max(r,Math.max(g,b));float saturation=(max-min)/(float)Math.max(1,max),limit=Math.min(90,Math.max(25,saturation*255*.65f));int[] white=new int[h],dark=new int[h];int whiteTotal=0,darkTotal=0;
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){int c=pixels[y*w+x],low=Math.min(Color.red(c),Math.min(Color.green(c),Color.blue(c)));if(LocalOcr.saturation(c)*255<limit&&low>Math.max(150,min+20)){white[y]++;whiteTotal++;}if(Color.red(c)+Color.green(c)+Color.blue(c)<r+g+b-160&&low<130){dark[y]++;darkTotal++;}}
  int[] histogram=whiteTotal>=Math.max(8,darkTotal/3)?white:dark;List<int[]> runs=new ArrayList<>();for(int y=0;y<h;y++)if(histogram[y]>=1){if(runs.isEmpty()||y-runs.get(runs.size()-1)[1]>3)runs.add(new int[]{y,y});else runs.get(runs.size()-1)[1]=y;}
  List<Rect> lines=new ArrayList<>();for(int[] run:runs)if(run[1]-run[0]>=3)lines.add(new Rect(0,Math.max(0,run[0]-2),w,Math.min(h,run[1]+3)));return lines;
 }
 Read block(Bitmap image,Rect rect)throws Exception{
  Bitmap cell=Bitmap.createBitmap(image,rect.left,rect.top,rect.width(),rect.height());StringBuilder text=new StringBuilder();float confidence=0;int count=0;
  try{for(Rect row:lines(cell)){int w=row.width(),h=row.height();Bitmap padded=Bitmap.createBitmap(w+6,h+6,Bitmap.Config.ARGB_8888);int[] pixels=new int[(w+6)*(h+6)];for(int y=0;y<h+6;y++)for(int x=0;x<w+6;x++)pixels[y*(w+6)+x]=cell.getPixel(Math.max(0,Math.min(w-1,x-3)),row.top+Math.max(0,Math.min(h-1,y-3)));padded.setPixels(pixels,0,w+6,0,0,w+6,h+6);Read read;try{read=line(padded);}finally{padded.recycle();}String value=read.text.trim();if(value.isEmpty()||value.matches("[_—-]+"))continue;text.append(value);confidence+=read.confidence*Math.max(1,value.length());count+=Math.max(1,value.length());}}finally{if(cell!=image)cell.recycle();}return new Read(text.toString().replaceAll("\\s+",""),count==0?0:confidence/count);
 }
 public void close()throws OrtException{session.close();}
}
