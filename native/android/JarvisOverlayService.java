package com.supratim.jarvis;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import android.net.ConnectivityManager;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JarvisOverlayService extends Service {
  private WindowManager wm;
  private View bubble, panel;
  private WindowManager.LayoutParams bubbleParams;
  private final int purple=Color.rgb(159,65,235);
  private EditText input;
  private TextView conversation, status;
  private ScrollView scroll;
  private SpeechRecognizer recognizer;
  private TextToSpeech tts;
  private final ExecutorService executor=Executors.newSingleThreadExecutor();
  private final Handler main=new Handler(Looper.getMainLooper());

  public void onCreate() {
    super.onCreate();
    wm=(WindowManager)getSystemService(WINDOW_SERVICE);
    tts=new TextToSpeech(this, result -> { if(result==TextToSpeech.SUCCESS) tts.setLanguage(Locale.US); });
    showBubble();
  }
  private int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
  private GradientDrawable bg(int color,int stroke) {
    GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(20));
    d.setStroke(dp(1),stroke); return d;
  }
  private TextView label(String s,int size) {
    TextView t=new TextView(this); t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(size);
    t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); t.setGravity(Gravity.CENTER); t.setPadding(dp(12),dp(9),dp(12),dp(9)); return t;
  }
  private WindowManager.LayoutParams params(int w,int h,int flags) {
    return new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,flags,PixelFormat.TRANSLUCENT);
  }
  private void showBubble() {
    ArcReactor reactor=new ArcReactor();
    bubble=reactor;
    bubbleParams=params(dp(66),dp(66),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
    bubbleParams.gravity=Gravity.TOP|Gravity.END; bubbleParams.x=dp(16); bubbleParams.y=dp(210);
    bubble.setOnClickListener(v->togglePanel());
    wm.addView(bubble,bubbleParams);
  }
  private void togglePanel() {
    if(panel!=null){ removePanel(); return; }
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(12),dp(10),dp(12),dp(10)); root.setBackground(bg(Color.rgb(13,9,27),purple));
    LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setOrientation(LinearLayout.HORIZONTAL);
    TextView title=label("◉  J.A.R.V.I.S",16); title.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
    header.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
    TextView close=label("×",24); close.setOnClickListener(v->removePanel()); header.addView(close);
    root.addView(header);
    status=label("ONLINE  •  READY",10); status.setTextColor(Color.rgb(120,235,255)); root.addView(status);
    scroll=new ScrollView(this); scroll.setFillViewport(true);
    conversation=new TextView(this); conversation.setText("JARVIS: I'm here. What's up?"); conversation.setTextColor(Color.WHITE);
    conversation.setTextSize(14); conversation.setPadding(dp(8),dp(12),dp(8),dp(12));
    scroll.addView(conversation); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1); sp.topMargin=dp(8); sp.bottomMargin=dp(8); root.addView(scroll,sp);
    LinearLayout controls=new LinearLayout(this); controls.setGravity(Gravity.CENTER_VERTICAL);
    input=new EditText(this); input.setSingleLine(true); input.setTextColor(Color.WHITE); input.setHintTextColor(Color.LTGRAY);
    input.setHint("Message JARVIS..."); input.setTextSize(14); input.setPadding(dp(10),0,dp(8),0);
    input.setBackground(bg(Color.rgb(35,25,51),Color.rgb(100,60,140))); input.setImeOptions(EditorInfo.IME_ACTION_SEND);
    controls.addView(input,new LinearLayout.LayoutParams(0,dp(48),1));
    TextView mic=label("🎙",18); mic.setBackground(bg(Color.rgb(54,35,79),purple)); controls.addView(mic,new LinearLayout.LayoutParams(dp(48),dp(48)));
    TextView send=label("➤",19); send.setBackground(bg(Color.rgb(100,35,155),purple)); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(48),dp(48)); bp.leftMargin=dp(6); controls.addView(send,bp);
    root.addView(controls);
    send.setOnClickListener(v->sendMessage());
    input.setOnEditorActionListener((v,action,event)->{ if(action==EditorInfo.IME_ACTION_SEND){sendMessage();return true;} return false; });
    mic.setOnClickListener(v->startVoiceInput());
    panel=root;
    WindowManager.LayoutParams p=params(dp(310),dp(440),WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
    p.gravity=Gravity.TOP|Gravity.END; p.x=dp(12); p.y=dp(285); p.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
    wm.addView(panel,p);
  }
  private void removePanel(){ if(panel!=null){try{wm.removeView(panel);}catch(Exception ignored){} panel=null; input=null; conversation=null; status=null; scroll=null;} stopVoiceInput(); }
  private void append(String who,String text){
    if(conversation==null)return;
    conversation.append("\\n\\n"+who+": "+text);
    if(scroll!=null) scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
  }
  private void sendMessage(){
    if(input==null)return;
    String message=input.getText().toString().trim(); if(message.isEmpty())return;
    input.setText(""); append("YOU",message);
    if(status!=null)status.setText("THINKING...");
    executor.execute(()->{
      HttpURLConnection c=null;
      try{
        c=(HttpURLConnection)new URL("https://jarvis-ai.goru041212.workers.dev/").openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(30000);
        c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json");
        JSONObject body=new JSONObject(); body.put("message",message);
        try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes("UTF-8"));}
        int code=c.getResponseCode(); InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[2048]; int n;
        while((n=stream.read(buf))!=-1)out.write(buf,0,n);
        JSONObject data=new JSONObject(out.toString("UTF-8"));
        if(code<200||code>=300)throw new Exception(data.optString("error","AI request failed"));
        String answer=data.optString("reply","I couldn't generate a response.");
        main.post(()->{append("JARVIS",answer); if(status!=null)status.setText("ONLINE  •  READY"); if(tts!=null)tts.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"jarvis-overlay");});
      }catch(Exception e){main.post(()->{append("JARVIS","I couldn't reach my AI brain. Please try again.");if(status!=null)status.setText("CONNECTION ISSUE");});}
      finally{if(c!=null)c.disconnect();}
    });
  }
  private void startVoiceInput(){
    if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){append("JARVIS","Microphone permission is needed. Open the main JARVIS app, tap its microphone once, and allow access.");return;}
    if(!SpeechRecognizer.isRecognitionAvailable(this)){append("JARVIS","Speech recognition isn't available on this phone.");return;}
    stopVoiceInput();
    try{
      recognizer=SpeechRecognizer.createSpeechRecognizer(this);
      recognizer.setRecognitionListener(new RecognitionListener(){
        public void onReadyForSpeech(Bundle b){main.post(()->{if(status!=null)status.setText("LISTENING...");});}
        public void onBeginningOfSpeech(){} public void onRmsChanged(float v){} public void onBufferReceived(byte[] b){}
        public void onEndOfSpeech(){}
        public void onError(int e){main.post(()->{if(status!=null)status.setText("ONLINE  •  READY");});}
        public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);main.post(()->{if(r!=null&&!r.isEmpty()&&input!=null){input.setText(r.get(0));sendMessage();}});}
        public void onPartialResults(Bundle b){} public void onEvent(int e,Bundle b){}
      });
      Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN"); recognizer.startListening(i);
    }catch(Exception e){append("JARVIS","I couldn't start voice input.");}
  }
  private void stopVoiceInput(){if(recognizer!=null){try{recognizer.cancel();recognizer.destroy();}catch(Exception ignored){}recognizer=null;}}
  private class ArcReactor extends View {
    private final Paint paint=new Paint(3);
    ArcReactor(){super(JarvisOverlayService.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    protected void onDraw(Canvas c){
      super.onDraw(c); float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-dp(3);
      paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(24,9,44)); c.drawCircle(cx,cy,r,paint);
      paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); paint.setColor(Color.rgb(175,92,255)); paint.setShadowLayer(dp(12),0,0,Color.rgb(155,55,255)); c.drawCircle(cx,cy,r-dp(2),paint); paint.clearShadowLayer();
      for(int k=0;k<3;k++){paint.setStrokeWidth(dp(k==0?3:1.5f));paint.setColor(k==0?Color.rgb(75,225,255):Color.rgb(155,100,220));c.drawCircle(cx,cy,r-dp(8+k*7),paint);}
      paint.setStrokeWidth(dp(2));paint.setColor(Color.rgb(115,225,255));
      for(int a=0;a<360;a+=30){double q=Math.toRadians(a);float x1=cx+(float)Math.cos(q)*(r-dp(9)),y1=cy+(float)Math.sin(q)*(r-dp(9));float x2=cx+(float)Math.cos(q)*(r-dp(17)),y2=cy+(float)Math.sin(q)*(r-dp(17));c.drawLine(x1,y1,x2,y2,paint);}
      paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(28,130,170));paint.setShadowLayer(dp(13),0,0,Color.CYAN);c.drawCircle(cx,cy,dp(12),paint);paint.clearShadowLayer();
      paint.setColor(Color.rgb(205,250,255));c.drawCircle(cx,cy,dp(7),paint);
      paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(Color.WHITE);c.drawCircle(cx,cy,dp(3),paint);
    }
  }
  public int onStartCommand(Intent i,int flags,int id){return START_STICKY;}
  public void onDestroy(){removePanel();stopVoiceInput();if(tts!=null)tts.shutdown();executor.shutdownNow();if(wm!=null){try{if(bubble!=null)wm.removeView(bubble);}catch(Exception ignored){}}super.onDestroy();}
  public IBinder onBind(Intent i){return null;}
}