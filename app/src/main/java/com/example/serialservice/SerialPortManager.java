package com.example.serialservice;

import android.os.HandlerThread;
import android.serialport.SerialPort;

import com.licheedev.hwutils.ByteUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class SerialPortManager {

    private static final String TAG = "SerialPortManager";

    private String mName;
    private SerialReadThread mReadThread;
    private OutputStream mOutputStream;
    private HandlerThread mWriteThread;
    private Scheduler mSendScheduler;

    private static class InstanceHolder {
        public static SerialPortManager sManager = new SerialPortManager("default");
    }


    public static SerialPortManager instance() {
        return InstanceHolder.sManager;
    }

    private SerialPort mSerialPort;

    public SerialPortManager(String name) {
        this.mName = name;
    }

    public SerialPort open(Device device) {
        return open(device.getPath(), device.getBaudrate());
    }

    public SerialPort open(String devicePath, String baudrateString) {
        if (mSerialPort != null) {
            close();
        }

        try {
            File device = new File(devicePath);
            int baudrate = Integer.parseInt(baudrateString);
            if(devicePath.equals("/dev/ttyS3")){
                mSerialPort = new SerialPort(device, baudrate, 8, 2, 1);
            }
            else{
                mSerialPort = new SerialPort(device, baudrate);
            }
            mReadThread = new SerialReadThread(mSerialPort.getInputStream(), this);
            mReadThread.start();

            mOutputStream = mSerialPort.getOutputStream();

            mWriteThread = new HandlerThread("write-thread");
            mWriteThread.start();
            mSendScheduler = AndroidSchedulers.from(mWriteThread.getLooper());

            return mSerialPort;
        } catch (Throwable tr) {
            close();
            return null;
        }
    }

    public void close() {
        if (mReadThread != null) {
            mReadThread.close();
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mWriteThread != null) {
            mWriteThread.quit();
        }

        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }

    public void onDataReceive(byte[] received, int size) {
        String hexStr = ByteUtil.bytes2HexStr(received, 0, size);
    }

    private void sendData(byte[] datas) throws Exception {
        mOutputStream.write(datas);
    }

    private Observable<Object> rxSendData(final byte[] datas) {
        return Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> emitter) throws Exception {
                try {
                    sendData(datas);
                    emitter.onNext(new Object());
                } catch (Exception e) {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                        return;
                    }
                }
                emitter.onComplete();
            }
        });
    }

    public void sendCommand(final String command) {
        byte[] bytes = ByteUtil.hexStr2bytes(command);
        rxSendData(bytes).subscribeOn(mSendScheduler).subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(Object o) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });
    }
}
