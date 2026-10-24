package cn.local.rainreminder;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;import java.io.*;
public class UpdateProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 File checked(Uri uri)throws FileNotFoundException{if(!uri.getAuthority().equals(getContext().getPackageName()+".updates")||!"/verified.apk".equals(uri.getPath()))throw new FileNotFoundException("无效文件");File file=AppUpdates.readyFile(getContext());if(!file.isFile())throw new FileNotFoundException("安装包不存在");return file;}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("仅允许读取");return ParcelFileDescriptor.open(checked(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 public String getType(Uri uri){return "application/vnd.android.package-archive";}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){try{File file=checked(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor result=new MatrixCursor(columns);Object[] values=new Object[columns.length];for(int i=0;i<columns.length;i++)values[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?"RainReminder-update.apk":OpenableColumns.SIZE.equals(columns[i])?file.length():null;result.addRow(values);return result;}catch(Exception e){return null;}}
 public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}public int update(Uri uri,ContentValues values,String where,String[] args){throw new UnsupportedOperationException();}public int delete(Uri uri,String where,String[] args){throw new UnsupportedOperationException();}
}
