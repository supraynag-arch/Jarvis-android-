package com.supratim.jarvis;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;

@CapacitorPlugin(name = "JarvisOverlay")
public class JarvisOverlayPlugin extends Plugin {
  @PluginMethod public void requestPermission(PluginCall call) {
    if (Settings.canDrawOverlays(getContext())) { JSObject r=new JSObject(); r.put("granted",true); call.resolve(r); return; }
    Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getContext().getPackageName()));
    getActivity().startActivity(i);
    JSObject r=new JSObject(); r.put("granted",false); r.put("openedSettings",true); call.resolve(r);
  }
  @PluginMethod public void start(PluginCall call) {
    if (!Settings.canDrawOverlays(getContext())) { call.reject("Allow Display over other apps first."); return; }
    getContext().startService(new Intent(getContext(), JarvisOverlayService.class));
    call.resolve();
  }
  @PluginMethod public void stop(PluginCall call) {
    getContext().stopService(new Intent(getContext(), JarvisOverlayService.class));
    call.resolve();
  }
  @PluginMethod public void permissionStatus(PluginCall call) {
    JSObject r=new JSObject(); r.put("granted",Settings.canDrawOverlays(getContext())); call.resolve(r);
  }
}