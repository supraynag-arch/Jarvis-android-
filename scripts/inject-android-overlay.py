#!/usr/bin/env python3
from pathlib import Path
import shutil
root=Path("android")
src=Path("native/android")
pkg=root/"app/src/main/java/com/supratim/jarvis"
pkg.mkdir(parents=True,exist_ok=True)
for name in ("JarvisOverlayPlugin.java","JarvisOverlayService.java"):
    shutil.copy2(src/name,pkg/name)
main=pkg/"MainActivity.java"
main.write_text("""package com.supratim.jarvis;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
public class MainActivity extends BridgeActivity {
  @Override public void onCreate(Bundle savedInstanceState) {
    registerPlugin(JarvisOverlayPlugin.class);
    super.onCreate(savedInstanceState);
  }
}
""")
manifest=root/"app/src/main/AndroidManifest.xml"
s=manifest.read_text()
s=s.replace('<application', '<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />' + chr(10) + '    <application',1)
s=s.replace('</application>', '<service android:name="com.supratim.jarvis.JarvisOverlayService" android:exported="false" />' + chr(10) + '    </application>')
manifest.write_text(s)
