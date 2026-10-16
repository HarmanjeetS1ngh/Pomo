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

  // Landscape: edge-to-edge, draw into the cutout area and hide status + navigation bars
  // (swipe from an edge shows them briefly). Portrait: normal system bars.
  private void applyBars() {
    Window w = getWindow();
    boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    w.setBackgroundDrawable(new ColorDrawable(0xFF0D0D0D));
    w.setStatusBarColor(0xFF0D0D0D);
    w.setNavigationBarColor(0xFF0D0D0D);
    if (Build.VERSION.SDK_INT >= 28) {
      WindowManager.LayoutParams lp = w.getAttributes();
      lp.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30
        ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      w.setAttributes(lp);
    }
    WindowCompat.setDecorFitsSystemWindows(w, !land);
    WindowInsetsControllerCompat ic = WindowCompat.getInsetsController(w, w.getDecorView());
    ic.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    if (land) ic.hide(WindowInsetsCompat.Type.systemBars());
    else ic.show(WindowInsetsCompat.Type.systemBars());
  }
}
