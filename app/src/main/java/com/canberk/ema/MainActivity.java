
package com.canberk.ema;
import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.chaquopy.python.Python;
import com.chaquopy.python.android.AndroidPlatform;
import java.io.File;
public class MainActivity extends Activity {
    TextView tvStatus; EditText etText; Python py; boolean modelReady=false;
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus=findViewById(R.id.tvStatus);
        etText=findViewById(R.id.etText);
        Button btnSpeak=findViewById(R.id.btnSpeak);
        Button btnSave=findViewById(R.id.btnSave);
        etText.setText("Merhaba, size nasil yardimci olabilirim? Bu ses tamamen telefonda üretiliyor.");
        if(!Python.isStarted()) Python.start(new AndroidPlatform(this));
        py=Python.getInstance();
        tvStatus.setText("Model yükleniyor... İlk açılışta 34MB indirilecek.");
        new Thread(()->{
            try{
                py.getModule("ema_wrapper").callAttr("init_model");
                modelReady=true;
                runOnUiThread(()->{tvStatus.setText("✓ Model hazır! Offline TTS aktif."); Toast.makeText(this,"EMA hazır",Toast.LENGTH_SHORT).show();});
            }catch(Exception e){runOnUiThread(()->tvStatus.setText("✗ Hata: "+e.getMessage()));}
        }).start();
        btnSpeak.setOnClickListener(v->synthesizeAndPlay(false));
        btnSave.setOnClickListener(v->synthesizeAndPlay(true));
    }
    void synthesizeAndPlay(boolean saveOnly){
        String text=etText.getText().toString().trim();
        if(text.isEmpty()){Toast.makeText(this,"Metin girin",Toast.LENGTH_SHORT).show();return;}
        if(!modelReady){Toast.makeText(this,"Model hazır değil",Toast.LENGTH_SHORT).show();return;}
        tvStatus.setText("Sentezleniyor...");
        new Thread(()->{
            try{
                File outFile=new File(getCacheDir(),"ema_out.wav");
                if(outFile.exists()) outFile.delete();
                py.getModule("ema_wrapper").callAttr("synthesize_to_file", text, outFile.getAbsolutePath());
                runOnUiThread(()->{
                    tvStatus.setText("✓ Bitti: "+outFile.length()/1024+" KB");
                    if(!saveOnly){
                        try{MediaPlayer mp=new MediaPlayer(); mp.setDataSource(outFile.getAbsolutePath()); mp.prepare(); mp.start(); Toast.makeText(this,"Çalıyor",Toast.LENGTH_SHORT).show(); mp.setOnCompletionListener(m->m.release());}catch(Exception e){tvStatus.setText("Çalma hatası: "+e.getMessage());}
                    }else{
                        File dest=new File(getExternalFilesDir(null),"ema_"+System.currentTimeMillis()+".wav");
                        try{java.nio.file.Files.copy(outFile.toPath(), dest.toPath()); tvStatus.setText("✓ Kaydedildi: "+dest.getAbsolutePath());}catch(Exception ex){tvStatus.setText("Kaydetme hatası: "+ex.getMessage());}
                    }
                });
            }catch(Exception e){runOnUiThread(()->tvStatus.setText("Hata: "+e.getMessage()));}
        }).start();
    }
}
