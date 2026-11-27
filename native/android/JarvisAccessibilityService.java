package com.supratim.jarvis;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

public class JarvisAccessibilityService extends AccessibilityService {
  private static JarvisAccessibilityService instance;

  @Override public void onServiceConnected() { super.onServiceConnected(); instance = this; }
  @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
  @Override public void onInterrupt() {}
  @Override public void onDestroy() { if (instance == this) instance = null; super.onDestroy(); }

  public static boolean isConnected() { return instance != null; }

  public static String readVisibleText() {
    if (instance == null) return null;
    AccessibilityNodeInfo root = instance.getRootInActiveWindow();
    if (root == null) return "";
    List<String> lines = new ArrayList<>();
    collect(root, lines);
    root.recycle();
    StringBuilder out = new StringBuilder();
    for (String line : lines) {
      if (out.length() + line.length() > 6000) break;
      if (out.length() > 0) out.append("\n");
      out.append(line);
    }
    return out.toString();
  }

  private static void collect(AccessibilityNodeInfo node, List<String> lines) {
    if (node == null || lines.size() >= 250 || node.isPassword()) return;
    CharSequence text = node.getText();
    CharSequence desc = node.getContentDescription();
    String value = text != null ? text.toString().trim() : "";
    if (value.isEmpty() && desc != null) value = desc.toString().trim();
    if (!value.isEmpty() && !lines.contains(value)) lines.add(value);
    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfo child = node.getChild(i);
      if (child != null) {
        collect(child, lines);
        child.recycle();
      }
    }
  }
}