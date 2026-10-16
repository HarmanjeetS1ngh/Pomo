package com.harman.focus;

import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  private int barColor = 0xFF0D0D0D;
  private boolean light = false;

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
    if (hasFocus) applyBars();
  }

  // Landscape: hide status + navigation bars (swipe from an edge shows them briefly) on a dark window.
  // Portrait: normal system bars. Window fitting is never toggled, so layout can't shift on rotation.
  private void applyBars() {
    Window w = getWindow();
    boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    int bg = barColor;
    w.setBackgroundDrawable(new ColorDrawable(bg));
    w.setStatusBarColor(bg);
    w.setNavigationBarColor(bg);
    if (Build.VERSION.SDK_INT >= 28) {
      WindowManager.LayoutParams lp = w.getAttributes();
      lp.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30
        ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
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
