package com.example.serialservice;

import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import com.licheedev.hwutils.ByteUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SerialReadThread extends Thread {


    private BufferedInputStream mInputStream;
    private SerialPortManager serialPortManager;

    private List<Byte> buffer = new ArrayList<>();
    private Timer timeoutTimer;
    public SerialReadThread(InputStream is, SerialPortManager manager) {
        mInputStream = new BufferedInputStream(is);
        serialPortManager = manager;

    }

    @Override
    public void run() {
        byte[] received = new byte[1024];
        int size;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                int available = mInputStream.available();

                if (available > 0) {
                    size = mInputStream.read(received);
                    if (size > 0) {
                        onDataReceived(received, size);
                    }
                } else {
                    SystemClock.sleep(1);
                }
            } catch (IOException e) {
                Log.e("SerialReadThread", "IOException in run", e);
            }
        }
    }
    private void onDataReceived(byte[] received, int size) {
        buffer.clear(); // Clear buffer before adding new data

        // Add received bytes to buffer
        for (int i = 0; i < size; i++) {
            buffer.add(received[i]);
        }
        // Cancel previous timer and start a new one
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
        }
        timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimeoutTask(), 300); // Adjust timeout as needed
    }
    private void processMessage() {
        try{
            byte[] message = new byte[buffer.size()];
            for (int i = 0; i < buffer.size(); i++) {
                message[i] = buffer.get(i);
            }

            // Process the message
            serialPortManager.onDataReceive(message, message.length);
            logData(message);
        }
        catch (Exception ex){


        }

    }
    public void close() {
        try {
            mInputStream.close();
        } catch (IOException e) {
            // Handle exception
        } finally {
            interrupt();
        }
    }
    private void logData(byte[] data) {
        String timeStamp = getCurrentDateTime("yyyy-MM-dd HH");
        String fileName = timeStamp + "H.txt";

        File dir = new File(Environment.getExternalStorageDirectory(), "Documents");
        dir.mkdirs();
        File file = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(getCurrentDateTime("yyyy-MM-dd HH:mm:ss").getBytes());
            fos.write(ByteUtil.bytes2HexStr(data).getBytes());
            fos.write("\n".getBytes());
        } catch (IOException e) {
            Log.e("SerialReadThread", "Error writing to file", e);
        }
    }

    private String getCurrentDateTime(String pattern) {
        return android.text.format.DateFormat.format(pattern, new Date()).toString();
    }
    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            processMessage(); // Process the message when timeout occurs
        }
    }
}
