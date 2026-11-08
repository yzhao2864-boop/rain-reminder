package cn.local.rainreminder;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;import java.io.*;
public class ExportProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 File checked(Uri uri)throws FileNotFoundException{
  String name=uri.getLastPathSegment();
  if(!uri.getAuthority().equals(getContext().getPackageName()+".export")||name==null||!name.matches("daike-export-\\d{4}-\\d{2}-\\d{2}\\.json"))throw new FileNotFoundException("无效文件");
  File dir=new File(getContext().getCacheDir(),"exports"),file=new File(dir,name);
  try{if(!file.getCanonicalPath().startsWith(dir.getCanonicalPath()+File.separator))throw new FileNotFoundException("无效文件");}catch(IOException e){throw new FileNotFoundException("无效文件");}
  if(!file.isFile())throw new FileNotFoundException("导出文件不存在");return file;
 }
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("仅允许读取");return ParcelFileDescriptor.open(checked(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 public String getType(Uri uri){return "application/json";}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){try{File file=checked(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor result=new MatrixCursor(columns);Object[] values=new Object[columns.length];for(int i=0;i<columns.length;i++)values[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?file.getName():OpenableColumns.SIZE.equals(columns[i])?file.length():null;result.addRow(values);return result;}catch(Exception e){return null;}}
 public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}public int update(Uri uri,ContentValues values,String where,String[] args){throw new UnsupportedOperationException();}public int delete(Uri uri,String where,String[] args){throw new UnsupportedOperationException();}
}
