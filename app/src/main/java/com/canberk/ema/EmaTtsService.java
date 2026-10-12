
package com.canberk.ema;
import android.media.AudioFormat;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeechService;
import android.util.Log;
import com.chaquopy.python.Python;
import com.chaquopy.python.android.AndroidPlatform;
import java.io.File; import java.io.FileInputStream;
public class EmaTtsService extends TextToSpeechService {
    private static final String TAG="EmaTtsService"; private Python py; private boolean pythonReady=false;
    @Override public void onCreate(){
        super.onCreate();
        Log.i(TAG,"EMA TTS Service olusturuldu");
        if(!Python.isStarted()) Python.start(new AndroidPlatform(this));
        py=Python.getInstance();
        new Thread(()->{try{py.getModule("ema_wrapper").callAttr("init_model"); pythonReady=true; Log.i(TAG,"Model hazır");}catch(Exception e){Log.e(TAG,"Model hatası",e);}}).start();
    }
    @Override protected String[] onGetLanguage(){return new String[]{"tr","TR",""};}
    @Override protected int onIsLanguageAvailable(String lang,String country,String variant){if("tr".equalsIgnoreCase(lang)) return LANG_COUNTRY_AVAILABLE; return LANG_AVAILABLE;}
    @Override protected int onLoadLanguage(String lang,String country,String variant){return LANG_COUNTRY_AVAILABLE;}
    @Override protected void onStop(){}
    @Override protected synchronized void onSynthesizeText(SynthesisRequest request,SynthesisCallback callback){
        String text=request.getCharSequenceText().toString();
        if(text.trim().isEmpty()){callback.done();return;}
        if(!pythonReady){int w=0; while(!pythonReady && w<15000){try{Thread.sleep(300); w+=300;}catch(Exception ignored){}}}
        try{
            File outFile=new File(getCacheDir(),"tts_"+System.currentTimeMillis()+".wav");
            py.getModule("ema_wrapper").callAttr("synthesize_to_file", text, outFile.getAbsolutePath());
            FileInputStream fis=new FileInputStream(outFile);
            byte[] header=new byte[44]; fis.read(header);
            int sampleRate=(header[24]&0xFF)|((header[25]&0xFF)<<8)|((header[26]&0xFF)<<16)|((header[27]&0xFF)<<24);
            if(sampleRate<=0) sampleRate=48000;
            callback.start(sampleRate, AudioFormat.ENCODING_PCM_16BIT,1);
            byte[] buf=new byte[4096]; int r; while((r=fis.read(buf))!=-1){callback.audioAvailable(buf,0,r); if(callback.hasFinished()) break;}
            fis.close(); callback.done(); outFile.delete();
        }catch(Exception e){Log.e(TAG,"Sentez hatası",e); callback.error();}
    }
}
