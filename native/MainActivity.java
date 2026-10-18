package com.harman.focus;

import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  private int barColor = 0xFF121212;
  private boolean light = false;
  private int iT, iB, iL, iR;
  private boolean insLand;

  private boolean isLand() {
    android.util.DisplayMetrics m = getResources().getDisplayMetrics();
    return m.widthPixels > m.heightPixels;
  }

  // Pushes current system-bar / cutout insets (CSS px) to the web layer.
  private void pushInsets() {
    if (getBridge() == null || getBridge().getWebView() == null) return;
    if (insLand != isLand()) return; // values belong to the other orientation; the next inset pass sends fresh ones
    float d = getResources().getDisplayMetrics().density;
    String js = "window.__ins&&window.__ins(" + (iT / d) + "," + (iB / d) + "," + (iL / d) + "," + (iR / d) + "," + isLand() + ")";
    getBridge().getWebView().evaluateJavascript(js, null);
  }

  // Called from the web layer (PremiumHaptics.theme) so system bars follow the in-app theme.
  public void setBars(int color, boolean l) {
    barColor = color;
    light = l;
    applyBars();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    registerPlugin(PremiumHapticsPlugin.class);
    super.onCreate(savedInstanceState);
    // Always edge-to-edge: showing/hiding bars on rotation never resizes the WebView (no jump);
    // the web layer pads itself using the insets pushed below.
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    final View decor = getWindow().getDecorView();
    ViewCompat.setOnApplyWindowInsetsListener(decor, (v, insets) -> {
      // Deterministic insets: independent of whether the bars are currently shown/hidden/animating,
      // so rotation never pushes transient values (the cause of the upward jump).
      Insets sb = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars());
      Insets cu = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
      insLand = isLand();
      if (insLand) { iT = cu.top; iB = 0; iL = cu.left; iR = cu.right; }  // bars hidden in landscape
      else { iT = Math.max(sb.top, cu.top); iB = sb.bottom; iL = cu.left; iR = cu.right; }
      pushInsets();
      return WindowInsetsCompat.CONSUMED;
    });
    decor.postDelayed(this::pushInsets, 500);
    decor.postDelayed(this::pushInsets, 1500);
    applyBars();
  }

  @Override
  public void onConfigurationChanged(Configuration c) {
    super.onConfigurationChanged(c);
    applyBars();
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) { applyBars(); pushInsets(); }
  }

  // Landscape: hide status + navigation bars (swipe from an edge shows them briefly) on a dark window.
  // Portrait: normal system bars. Window fitting is never toggled, so layout can't shift on rotation.
  private void applyBars() {
    Window w = getWindow();
    boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    int bg = barColor;
    w.setBackgroundDrawable(new ColorDrawable(bg));
    // system rotation animation paints this colour behind the app (otherwise a grey theme default)
    if (Build.VERSION.SDK_INT >= 33) setTaskDescription(new android.app.ActivityManager.TaskDescription.Builder().setBackgroundColor(bg).build());
    // WebView surface follows the theme too, so no off-colour strip shows while it resizes on rotation
    if (getBridge() != null && getBridge().getWebView() != null) getBridge().getWebView().setBackgroundColor(bg);
    w.setStatusBarColor(bg);
    w.setNavigationBarColor(bg);
    if (Build.VERSION.SDK_INT >= 28) {
      WindowManager.LayoutParams lp = w.getAttributes();
      lp.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30
        ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      lp.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE; // no grey frame / jerk on rotate
      w.setAttributes(lp);
    }
    WindowInsetsControllerCompat ic = WindowCompat.getInsetsController(w, w.getDecorView());
    ic.setAppearanceLightStatusBars(light);
    ic.setAppearanceLightNavigationBars(light);
    ic.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    if (land) ic.hide(WindowInsetsCompat.Type.systemBars());
    else ic.show(WindowInsetsCompat.Type.systemBars());
  }
}
