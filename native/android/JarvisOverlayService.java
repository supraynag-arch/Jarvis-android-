package com.supratim.jarvis;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class JarvisOverlayService extends Service {
  private WindowManager wm; private View bubble; private View panel; private WindowManager.LayoutParams bubbleParams;
  private int purple=Color.rgb(159,65,235);
  public void onCreate() { super.onCreate(); wm=(WindowManager)getSystemService(WINDOW_SERVICE); showBubble(); }
  private GradientDrawable bg(int color,int stroke) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(48); d.setStroke(2,stroke); return d; }
  private TextView label(String s,int size) { TextView t=new TextView(this); t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(size); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); t.setGravity(Gravity.CENTER); t.setPadding(18,12,18,12); return t; }
  private WindowManager.LayoutParams params(int w,int h,int gravity) { return new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT); }
  private void showBubble() {
    bubble=label("J",19); bubble.setBackground(bg(Color.rgb(63,17,100),purple));
    bubbleParams=params(58,58,Gravity.TOP|Gravity.END); bubbleParams.gravity=Gravity.TOP|Gravity.END; bubbleParams.x=18; bubbleParams.y=220;
    bubble.setOnClickListener(v->togglePanel()); wm.addView(bubble,bubbleParams);
  }
  private void togglePanel() {
    if(panel!=null){ try{wm.removeView(panel);}catch(Exception ignored){} panel=null; return; }
    LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(8,8,8,8); box.setBackground(bg(Color.rgb(19,7,32),purple));
    TextView title=label("J.A.R.V.I.S  •  ONLINE",16); box.addView(title);
    TextView desc=label("Your assistant is ready.",13); box.addView(desc);
    TextView open=label("OPEN FULL ASSISTANT",14); open.setBackground(bg(Color.rgb(93,31,145),purple)); box.addView(open);
    TextView close=label("CLOSE",12); box.addView(close);
    open.setOnClickListener(v->{ Intent i=getPackageManager().getLaunchIntentForPackage(getPackageName()); if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);} });
    close.setOnClickListener(v->togglePanel());
    panel=box; WindowManager.LayoutParams p=params(260,WindowManager.LayoutParams.WRAP_CONTENT,Gravity.TOP|Gravity.END); p.gravity=Gravity.TOP|Gravity.END; p.x=18;p.y=285;wm.addView(panel,p);
  }
  public int onStartCommand(Intent i,int flags,int id){return START_STICKY;}
  public void onDestroy(){if(wm!=null){try{if(bubble!=null)wm.removeView(bubble);}catch(Exception ignored){}try{if(panel!=null)wm.removeView(panel);}catch(Exception ignored){}}super.onDestroy();}
  public IBinder onBind(Intent i){return null;}
}