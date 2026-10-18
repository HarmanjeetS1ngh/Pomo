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
  private int tries = 0;
  private boolean hideBars = false; // bars hidden only while the timer runs
  private final Runnable visRun = this::enforce;

  private boolean isLand() {
    android.util.DisplayMetrics m = getResources().getDisplayMetrics();
    return m.widthPixels > m.heightPixels;
  }

  private void calc(WindowInsetsCompat insets) {
    Insets sb = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars());
    Insets cu = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
    insLand = isLand();
    iT = Math.max(sb.top, cu.top); iB = sb.bottom; iL = Math.max(sb.left, cu.left); iR = Math.max(sb.right, cu.right);
  }

  // Pull API for the web layer (called on load, so first launch never depends on push timing).
  public float[] insetsDp() {
    WindowInsetsCompat r = ViewCompat.getRootWindowInsets(getWindow().getDecorView());
    if (r != null) calc(r);
    float d = getResources().getDisplayMetrics().density;
    return new float[] { iT / d, iB / d, iL / d, iR / d, isLand() ? 1 : 0 };
  }

  // Pushes current system-bar / cutout insets (CSS px) to the web layer.
  private void pushInsets() {
    if (getBridge() == null || getBridge().getWebView() == null) return;
    if (insLand != isLand()) return; // values belong to the other orientation; the next inset pass sends fresh ones
    float d = getResources().getDisplayMetrics().density;
    String js = "window.__ins&&window.__ins(" + (iT / d) + "," + (iB / d) + "," + (iL / d) + "," + (iR / d) + "," + isLand() + ")";
    getBridge().getWebView().evaluateJavascript(js, null);
  }

  // Called from the web layer: hide status + navigation bars only while the timer is running.
  public void setImmersive(boolean on) {
    boolean was = hideBars;
    hideBars = on;
    tries = 0;
    final View d = getWindow().getDecorView();
    d.removeCallbacks(visRun);
    if (isTablet() && was && !on) { tabletRestoreBars(d); return; }
    enforce();
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
      calc(insets);
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
    applyBars(false);
    getWindow().getDecorView().removeCallbacks(visRun);
    getWindow().getDecorView().postDelayed(visRun, 450);
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) { applyBars(); pushInsets(); }
  }

  // Landscape: hide status + navigation bars (swipe from an edge shows them briefly) on a dark window.
  // Portrait: normal system bars. Window fitting is never toggled, so layout can't shift on rotation.
  private void applyBars() { applyBars(true); }
  private void applyBars(boolean visNow) {
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
      lp.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_ROTATE; // real rotate animation
      w.setAttributes(lp);
    }
    WindowInsetsControllerCompat ic = WindowCompat.getInsetsController(w, w.getDecorView());
    ic.setAppearanceLightStatusBars(light);
    ic.setAppearanceLightNavigationBars(light);
    ic.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    if (visNow) applyVisibility();
  }

  // Layout is edge-to-edge, so bar visibility never changes layout; doing it after the rotation
  // animation avoids inset dispatch + relayout work while the system animation is running.
  // Applies the wanted bar visibility, then checks the REAL state and retries until it matches.
  // (After a rotation the system can silently drop show()/hide() for the navigation bar.)
  private void enforce() {
    View d = getWindow().getDecorView();
    Window w = getWindow();
    WindowInsetsControllerCompat ic = WindowCompat.getInsetsController(w, d);
    if (!hideBars && tries == 6) ic.hide(WindowInsetsCompat.Type.systemBars()); // kick a stuck state
    else if (isTablet() && !hideBars && tries > 0) applyBars(true); // tablets only: redo what a rotation does
    else applyVisibility();
    WindowInsetsCompat r = ViewCompat.getRootWindowInsets(d);
    boolean ok = r == null || (r.isVisible(WindowInsetsCompat.Type.statusBars()) != hideBars
      && r.isVisible(WindowInsetsCompat.Type.navigationBars()) != hideBars);
    if (!ok && tries++ < 16) d.postDelayed(visRun, 120); else tries = 0;
  }

  // Tablets only. After a rotation made while the bars were hidden, a plain show() can be ignored for
  // the navigation bar (a second rotation fixes it). The visibility state is unreliable there, so don't
  // check it: just run a hide -> show cycle plus the full window setup, and repeat it while it settles.
  @SuppressWarnings("deprecation")
  private void tabletRestoreBars(final View d) {
    Window w = getWindow();
    WindowCompat.getInsetsController(w, d).hide(WindowInsetsCompat.Type.systemBars());
    d.postDelayed(() -> {
      if (hideBars) return;
      d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
      applyBars(true);
    }, 120);
    d.postDelayed(() -> { if (!hideBars) applyBars(true); }, 450);
    d.postDelayed(() -> { if (!hideBars) applyBars(true); }, 1000);
  }

  private boolean isTablet() {
    return getResources().getConfiguration().smallestScreenWidthDp >= 600;
  }

  private void applyVisibility() {
    Window w = getWindow();
    WindowInsetsControllerCompat ic = WindowCompat.getInsetsController(w, w.getDecorView());
    if (hideBars) ic.hide(WindowInsetsCompat.Type.systemBars());
    else ic.show(WindowInsetsCompat.Type.systemBars());
  }
}
