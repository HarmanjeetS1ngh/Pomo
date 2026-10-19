package com.harman.focus;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

/** Fires when a timer ends while the app is in the background and posts the "time's up" notification. */
public class TimerAlarmReceiver extends BroadcastReceiver {
  static final String CHANNEL = "timer_done_v1";
  static final int NOTIF_ID = 4242;
  /** Same buzz pattern as the in-app alert. */
  static final long[] BUZZ = {0, 200, 100, 200, 100, 200, 600, 200, 100, 200, 100, 200, 600, 200, 100, 200, 100, 200};

  static void ensureChannel(Context c) {
    if (Build.VERSION.SDK_INT < 26) return;
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (nm == null || nm.getNotificationChannel(CHANNEL) != null) return;
    NotificationChannel ch = new NotificationChannel(CHANNEL, "Timer finished", NotificationManager.IMPORTANCE_HIGH);
    ch.setDescription("Sound and vibration when a focus or break session ends");
    int res = c.getResources().getIdentifier("timer_done", "raw", c.getPackageName());
    if (res != 0) {
      ch.setSound(Uri.parse("android.resource://" + c.getPackageName() + "/" + res),
        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
    }
    ch.enableVibration(true);
    ch.setVibrationPattern(BUZZ);
    nm.createNotificationChannel(ch);
  }

  static PendingIntent alarmIntent(Context c, String title, String body) {
    Intent i = new Intent(c, TimerAlarmReceiver.class).putExtra("title", title).putExtra("body", body);
    return PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  static void schedule(Context c, long at, String title, String body) {
    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    if (am == null) return;
    PendingIntent pi = alarmIntent(c, title, body);
    try {
      if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
      } else {
        // No exact-alarm permission: an alarm-clock alarm is still exact and exempt from Doze.
        PendingIntent show = PendingIntent.getActivity(c, 2, new Intent(c, MainActivity.class),
          PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), pi);
      }
    } catch (SecurityException e) {
      am.set(AlarmManager.RTC_WAKEUP, at, pi);
    }
  }

  static void cancel(Context c) {
    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    if (am != null) am.cancel(alarmIntent(c, "", ""));
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (nm != null) nm.cancel(NOTIF_ID);
  }

  @Override
  public void onReceive(Context c, Intent in) {
    if (MainActivity.inForeground) return; // app is open: the in-app beep and buzz already handle it
    ensureChannel(c);
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (nm == null) return;
    PendingIntent open = PendingIntent.getActivity(c, 3, new Intent(c, MainActivity.class)
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
      ? new android.app.Notification.Builder(c, CHANNEL) : new android.app.Notification.Builder(c);
    b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentTitle(in.getStringExtra("title"))
      .setContentText(in.getStringExtra("body"))
      .setContentIntent(open)
      .setCategory(android.app.Notification.CATEGORY_ALARM)
      .setAutoCancel(true);
    if (Build.VERSION.SDK_INT < 26) { // pre-O has no channels
      b.setPriority(android.app.Notification.PRIORITY_HIGH).setVibrate(BUZZ);
      int res = c.getResources().getIdentifier("timer_done", "raw", c.getPackageName());
      if (res != 0) b.setSound(Uri.parse("android.resource://" + c.getPackageName() + "/" + res));
    }
    try { nm.notify(NOTIF_ID, b.build()); } catch (SecurityException ignored) {}
  }
}
