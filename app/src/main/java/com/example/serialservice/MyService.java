package com.example.serialservice;

import static com.example.serialservice.MyApplication.CHANNEL_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.serialport.SerialPortFinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;


public class MyService extends Service  {
    private Timer publishTimer;
    private SerialPortManager serialPortManagerttyS5;
    private boolean mOpenedttyS5 = false;
    private String[] mDevices;
    private Device mDevicettyS5;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TAG", "onCreate");
        serialPortManagerttyS5 = new SerialPortManager("/dev/ttyS5");

        SerialPortFinder serialPortFinder = new SerialPortFinder();
        mDevices = serialPortFinder.getAllDevicesPath();
        if (mDevices.length == 0) {
            Log.d("TAG", "No Device: ");
        } else {
            StringBuilder devicesList = new StringBuilder();
            for (String device : mDevices) {
                devicesList.append(device).append("\n");
            }
        }
        try {
            serialPortManagerttyS5.close();
        } catch (Exception e) {
            // Handle exception
        }
        mDevicettyS5 = new Device("/dev/ttyS5", "9600");
        try {
            mOpenedttyS5 = serialPortManagerttyS5.open(mDevicettyS5) != null;

        } catch (Exception ex) {
            // Handle exception
        }

        publishTimer = new Timer();
        publishTimer.schedule(new PublishTask(), 10000, 10000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String strDataIntent = intent.getStringExtra("key_data_intent");
        sendNotification(strDataIntent);

        return START_REDELIVER_INTENT;
    }

    private void sendNotification(String strDataIntent) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Title notification service example")
                .setContentText(strDataIntent)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }
    private void sendData() {
        try {
            String text = "FF010102000000FD";
                if (TextUtils.isEmpty(text) || text.length() % 2 != 0) {
                    return;
                }
            serialPortManagerttyS5.sendCommand(text);
        } catch (Exception ex) {
        }
    }
    @Override
    public void onDestroy() {
        Log.d("TAG", "onDestroy");
        super.onDestroy();
        Intent restartServiceIntent = new Intent(getApplicationContext(), MyService.class);
        startService(restartServiceIntent);
    }

    private class PublishTask extends TimerTask {
        @Override
        public void run() {
            sendData();
        }
    }
}
