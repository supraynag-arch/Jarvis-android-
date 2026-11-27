package com.supratim.jarvis;

import android.content.Intent;
import android.provider.Settings;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;

@CapacitorPlugin(name = "JarvisScreen")
public class JarvisScreenPlugin extends Plugin {
  @PluginMethod
  public void requestAccess(PluginCall call) {
    JSObject result = new JSObject();
    result.put("enabled", JarvisAccessibilityService.isConnected());
    if (!JarvisAccessibilityService.isConnected()) {
      Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
      getActivity().startActivity(intent);
      result.put("openedSettings", true);
    }
    call.resolve(result);
  }

  @PluginMethod
  public void status(PluginCall call) {
    JSObject result = new JSObject();
    result.put("enabled", JarvisAccessibilityService.isConnected());
    call.resolve(result);
  }

  @PluginMethod
  public void readVisibleText(PluginCall call) {
    if (!JarvisAccessibilityService.isConnected()) {
      call.reject("JARVIS screen-reading access is not enabled.");
      return;
    }
    String text = JarvisAccessibilityService.readVisibleText();
    JSObject result = new JSObject();
    result.put("text", text == null ? "" : text);
    call.resolve(result);
  }
}