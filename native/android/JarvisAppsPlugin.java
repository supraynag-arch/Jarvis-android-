package com.supratim.jarvis;

import android.content.Intent;
import android.content.pm.PackageManager;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;

@CapacitorPlugin(name = "JarvisApps")
public class JarvisAppsPlugin extends Plugin {
  @PluginMethod
  public void launchApp(PluginCall call) {
    String packageName = call.getString("packageName");
    if (packageName == null || packageName.trim().isEmpty()) {
      call.reject("An app package name is required.");
      return;
    }
    try {
      PackageManager pm = getContext().getPackageManager();
      Intent launch = pm.getLaunchIntentForPackage(packageName);
      if (launch == null) {
        call.reject("That app is not installed or cannot be launched.");
        return;
      }
      launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getContext().startActivity(launch);
      JSObject result = new JSObject();
      result.put("launched", true);
      result.put("packageName", packageName);
      call.resolve(result);
    } catch (Exception error) {
      call.reject("Android could not launch that app.", error);
    }
  }
}