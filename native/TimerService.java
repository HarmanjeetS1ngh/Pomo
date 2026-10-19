package com.harman.focus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Foreground service that runs while a timer is counting down. It shows the ongoing "Focus" countdown in the
 * notification bar and keeps the app process alive, so phones that kill background apps (Realme, Xiaomi, ...)
 * can't wipe the end-of-timer alarm.
 */
public class TimerService extends Service {
  static final String CHANNEL = "timer_running_v1";
  static final int ID = 4241;

  @Override public IBinder onBind(Intent i) { return null; }

  @Override
  public int onStartCommand(Intent in, int flags, int startId) {
    long at = in != null ? in.getLongExtra("at", 0) : 0;
    String label = in != null ? in.getStringExtra("label") : null;
    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (nm.getNotificationChannel(CHANNEL) == null) {
      NotificationChannel ch = new NotificationChannel(CHANNEL, "Timer running", NotificationManager.IMPORTANCE_LOW);
      ch.setShowBadge(false);
      nm.createNotificationChannel(ch);
    }
    PendingIntent open = PendingIntent.getActivity(this, 4, new Intent(this, MainActivity.class)
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder b = new Notification.Builder(this, CHANNEL)
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentTitle(label != null ? label : "Timer running")
      .setContentText("Time left")
      .setContentIntent(open)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setCategory(Notification.CATEGORY_PROGRESS);
    if (at > 0) b.setWhen(at).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true);
    startForeground(ID, b.build());
    return START_NOT_STICKY;
  }
}
