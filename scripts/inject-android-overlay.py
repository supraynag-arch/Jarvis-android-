#!/usr/bin/env python3
from pathlib import Path
import shutil

root = Path("android")
src = Path("native/android")
pkg = root / "app/src/main/java/com/supratim/jarvis"
pkg.mkdir(parents=True, exist_ok=True)

for name in ("JarvisOverlayPlugin.java", "JarvisOverlayService.java", "JarvisAppsPlugin.java", "JarvisScreenPlugin.java", "JarvisAccessibilityService.java"):
    shutil.copy2(src / name, pkg / name)

main = pkg / "MainActivity.java"
main.write_text("""package com.supratim.jarvis;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
public class MainActivity extends BridgeActivity {
  @Override public void onCreate(Bundle savedInstanceState) {
    registerPlugin(JarvisOverlayPlugin.class);
    registerPlugin(JarvisAppsPlugin.class);
    registerPlugin(JarvisScreenPlugin.class);
    super.onCreate(savedInstanceState);
  }
}
""")

# Install accessibility-service metadata and description.
res = root / "app/src/main/res"
xml_dir = res / "xml"
xml_dir.mkdir(parents=True, exist_ok=True)
shutil.copy2(src / "accessibility_service_config.xml", xml_dir / "jarvis_accessibility_service.xml")
values = res / "values"
values.mkdir(parents=True, exist_ok=True)
strings = values / "strings.xml"
description = '<string name="jarvis_accessibility_description">Allows JARVIS to read visible screen text only when you explicitly ask and approve each read.</string>'
if strings.exists():
    content = strings.read_text()
    if "jarvis_accessibility_description" not in content:
        content = content.replace("</resources>", "    " + description + "\n</resources>")
    strings.write_text(content)
else:
    strings.write_text('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    ' + description + '\n</resources>\n')

manifest = root / "app/src/main/AndroidManifest.xml"
s = manifest.read_text()
s = s.replace(
    "<application",
    '<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />\n'
    '<queries><intent><action android:name="android.intent.action.MAIN" />'
    '<category android:name="android.intent.category.LAUNCHER" />'
    '</intent></queries>\n    <application',
    1
)
s = s.replace(
    "</application>",
    '<service android:name="com.supratim.jarvis.JarvisOverlayService" '
    'android:exported="false" />\n'
    '<service android:name="com.supratim.jarvis.JarvisAccessibilityService" '
    'android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE" '
    'android:exported="false"><intent-filter><action android:name="android.accessibilityservice.AccessibilityService" />'
    '</intent-filter><meta-data android:name="android.accessibilityservice" '
    'android:resource="@xml/jarvis_accessibility_service" /></service>\n'
    '</application>'
)
manifest.write_text(s)
