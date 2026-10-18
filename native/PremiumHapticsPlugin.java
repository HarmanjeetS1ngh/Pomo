package com.harman.focus;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Uses the system's tuned haptic effects (same ones One UI / Android use for sliders and toggles). */
@CapacitorPlugin(name = "PremiumHaptics")
public class PremiumHapticsPlugin extends Plugin {

  /** Matches the Android status/navigation bars to the in-app theme. */
  @PluginMethod(returnType = PluginMethod.RETURN_NONE)
  public void theme(PluginCall call) {
    final boolean light = Boolean.TRUE.equals(call.getBoolean("light", false));
    int parsed = 0xFF121212;
    try { parsed = android.graphics.Color.parseColor(call.getString("bg", "#121212")); } catch (Exception ignored) {}
    final int color = parsed;
    getActivity().runOnUiThread(() -> {
      if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setBars(color, light);
    });
  }

  /** Current system-bar insets in CSS px, so the page can pad itself from the very first frame. */
  @PluginMethod
  public void insets(PluginCall call) {
    getActivity().runOnUiThread(() -> {
      if (!(getActivity() instanceof MainActivity)) { call.reject("no activity"); return; }
      float[] v = ((MainActivity) getActivity()).insetsDp();
      JSObject o = new JSObject();
      try { o.put("t", v[0]); o.put("b", v[1]); o.put("l", v[2]); o.put("r", v[3]); o.put("land", v[4] == 1); } catch (Exception ignored) {}
      call.resolve(o);
    });
  }

  /** Hides the system bars while the timer runs, shows them otherwise (any orientation). */
  @PluginMethod(returnType = PluginMethod.RETURN_NONE)
  public void immersive(PluginCall call) {
    final boolean on = Boolean.TRUE.equals(call.getBoolean("on", false));
    getActivity().runOnUiThread(() -> {
      if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setImmersive(on);
    });
  }

  @PluginMethod(returnType = PluginMethod.RETURN_NONE)
  public void play(PluginCall call) {
    final String t = call.getString("t", "step");
    final View v = getBridge().getWebView();
    v.post(() -> {
      v.setHapticFeedbackEnabled(true);
      int c = pick(t);
      if (c < 0 || !v.performHapticFeedback(c)) fallback(t);
    });
  }

  private int pick(String t) {
    int sdk = Build.VERSION.SDK_INT;
    switch (t) {
      case "step": // every slider step: lightest "frequent tick"
        return sdk >= 34 ? HapticFeedbackConstants.SEGMENT_FREQUENT_TICK : HapticFeedbackConstants.CLOCK_TICK;
      case "bp":   // breakpoint: crisper segment tick
        return sdk >= 34 ? HapticFeedbackConstants.SEGMENT_TICK : -1;
      case "end":  // slider min / max
        return sdk >= 34 ? HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
             : sdk >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.LONG_PRESS;
      case "on":   // timer start
        return sdk >= 34 ? HapticFeedbackConstants.TOGGLE_ON
             : sdk >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.KEYBOARD_TAP;
      case "off":  // timer pause
        return sdk >= 34 ? HapticFeedbackConstants.TOGGLE_OFF : HapticFeedbackConstants.KEYBOARD_TAP;
      case "nav":
        return HapticFeedbackConstants.CLOCK_TICK;
      default:     // "tap"
        return HapticFeedbackConstants.KEYBOARD_TAP;
    }
  }

  private void fallback(String t) {
    try {
      Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
      if (vib == null || !vib.hasVibrator()) return;
      if (Build.VERSION.SDK_INT >= 29) {
        int id = t.equals("step") || t.equals("nav") ? VibrationEffect.EFFECT_TICK
               : t.equals("end") ? VibrationEffect.EFFECT_HEAVY_CLICK : VibrationEffect.EFFECT_CLICK;
        vib.vibrate(VibrationEffect.createPredefined(id));
      } else if (Build.VERSION.SDK_INT >= 26) {
        vib.vibrate(VibrationEffect.createOneShot(8, 60));
      }
    } catch (Exception ignored) {}
  }
}
