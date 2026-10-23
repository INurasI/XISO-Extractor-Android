package com.base64e.xisoextractor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.system.Os;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.provider.DocumentsContract;
import android.util.TypedValue;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE=101, PICK_OUTPUT=102;
    private Uri imageUri;
    private TextView status;
    private Button extractButton;
    private File imageCopy;
    private final Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(28),dp(22),dp(20)); root.setBackgroundColor(0xFF10131A);
        TextView title=new TextView(this); title.setText("XISO Extractor"); title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP,27); title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(62)));
        TextView desc=new TextView(this); desc.setText("Xbox XISO / XDVDFS • ARM64\nФайлды таңдап, шығару бумасын таңдаңыз.");
        desc.setTextColor(0xFFB7C2D2); desc.setTextSize(15); desc.setGravity(Gravity.CENTER);
        desc.setPadding(0,0,0,dp(20)); root.addView(desc);
        Button pick=button("1. XISO файлын таңдау"); root.addView(pick,buttonParams());
        pick.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_IMAGE); });
        extractButton=button("2. Шығару бумасын таңдау"); extractButton.setEnabled(false); root.addView(extractButton,buttonParams());
        extractButton.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); startActivityForResult(i,PICK_OUTPUT); });
        ScrollView scroll=new ScrollView(this); status=new TextView(this); status.setText("Дайын. XISO файлын таңдаңыз.");
        status.setTextColor(0xFFE4E8EF); status.setTextSize(14); status.setPadding(dp(12),dp(14),dp(12),dp(14)); scroll.addView(status);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1); sp.topMargin=dp(16); root.addView(scroll,sp); setContentView(root);
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private LinearLayout.LayoutParams buttonParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));p.topMargin=dp(10);return p;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data); if(res!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(req==PICK_IMAGE){
            imageUri=uri;
            try {
                getContentResolver().takePersistableUriPermission(uri,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);
                imageCopy=new File(getCacheDir(),"selected-image.iso"); status.setText("Файл көшіріліп жатыр…"); copyUriToFile(uri,imageCopy);
                extractButton.setEnabled(true); status.setText("Файл: "+displayName(uri)+"\nӨлшемі: "+(imageCopy.length()/1024/1024)+" MiB\nЕнді шығару бумасын таңдаңыз.");
            }catch(Exception e){status.setText("Файлды оқу қатесі: "+e);}
        }else if(req==PICK_OUTPUT){
            try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}
            extractButton.setEnabled(false); status.setText("XISO шығарылуда…"); new Thread(()->extractToTree(uri)).start();
        }
    }
    private String displayName(Uri uri){try(Cursor c=getContentResolver().query(uri,null,null,null,null)){if(c!=null&&c.moveToFirst()){int x=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(x>=0)return c.getString(x);}}catch(Exception ignored){}return uri.getLastPathSegment();}
    private void copyUriToFile(Uri uri,File dst)throws IOException{try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new BufferedOutputStream(new FileOutputStream(dst))){if(in==null)throw new IOException("Файл ашылмады");byte[] b=new byte[1024*1024];int n;while((n=in.read(b))>0)out.write(b,0,n);}}
    private void extractToTree(Uri tree){
        File outDir=new File(getCacheDir(),"xiso-output"); deleteRec(outDir); outDir.mkdirs();
        try{
            File exe=new File(getFilesDir(),"xdvdfs");
            if(!exe.exists()){try(InputStream in=getAssets().open("xdvdfs");OutputStream out=new FileOutputStream(exe)){byte[] b=new byte[65536];int n;while((n=in.read(b))>0)out.write(b,0,n);} Os.chmod(exe.getAbsolutePath(),0700);}
            Process p=new ProcessBuilder(exe.getAbsolutePath(),"unpack",imageCopy.getAbsolutePath(),outDir.getAbsolutePath()).redirectErrorStream(true).start();
            StringBuilder log=new StringBuilder();try(BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()))){String line;while((line=br.readLine())!=null){if(log.length()>12000)log.delete(0,6000);log.append(line).append('\n');String m=line;handler.post(()->status.setText("Шығарылуда…\n"+m));}}
            int code=p.waitFor();if(code!=0)throw new IOException("xdvdfs exit="+code+"\n"+log);
            Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)); copyChildren(outDir,root);
            String done=log.toString();handler.post(()->status.setText("Дайын! Файлдар таңдалған бумаға шығарылды.\n"+done));
        }catch(Exception e){String err=e.toString();handler.post(()->status.setText("Қате: "+err+"\nXISO/XDVDFS кескіні қажет; барлық кәдімгі ISO түрлері қолдау таппауы мүмкін."));}
        finally{handler.post(()->extractButton.setEnabled(true));}
    }
    private void copyChildren(File src,Uri parent)throws IOException{
        if(!src.isDirectory())return;
        File[] children=src.listFiles();if(children==null)return;
        for(File child:children){
            try{
                if(child.isDirectory()){Uri dir=DocumentsContract.createDocument(getContentResolver(),parent,"vnd.android.document/directory",child.getName());if(dir==null)throw new IOException("Бума жасалмады: "+child.getName());copyChildren(child,dir);}
                else{Uri file=DocumentsContract.createDocument(getContentResolver(),parent,"application/octet-stream",child.getName());if(file==null)throw new IOException("Файл жасалмады: "+child.getName());try(InputStream in=new BufferedInputStream(new FileInputStream(child));OutputStream out=getContentResolver().openOutputStream(file,"w")){if(out==null)throw new IOException("Шығыс ашылмады: "+child.getName());byte[] b=new byte[65536];int n;while((n=in.read(b))>0)out.write(b,0,n);}}
            }catch(IOException e){throw e;}catch(Exception e){throw new IOException("Көшіру қатесі: "+child.getName(),e);}
        }
    }
    private void deleteRec(File f){if(!f.exists())return;if(f.isDirectory()){File[] a=f.listFiles();if(a!=null)for(File c:a)deleteRec(c);}f.delete();}
}
