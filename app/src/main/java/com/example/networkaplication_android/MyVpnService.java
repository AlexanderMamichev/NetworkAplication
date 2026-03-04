/*
 * Copyright 2026 Your Name
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */


package com.example.networkaplication_android;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class MyVpnService extends VpnService implements Runnable {
    private static final String TAG = "MyVpnService";
    private Thread mThread;
    private ParcelFileDescriptor mInterface;

    private static final String SERVER_ADDRESS = "PUBLIC_IP_HERE";   // ЗДЕС НАДО ДОБАВИТЬ ПУБЛИЧНЫЙ АДРЕСС
    private static final int SERVER_PORT = 9999;     // КАКОй-ТО ПОРТ пока не плейсхолдер
    private static final byte MASK_KEY = 0x42;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopVpn();
            return START_NOT_STICKY;
        }

        if (mThread != null) {
            mThread.interrupt();
        }
        mThread = new Thread(this, "MyVpnThread");
        mThread.start();
        return START_STICKY;
    }

    private void stopVpn() {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        try {
            if (mInterface != null) {
                mInterface.close();
                mInterface = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing interface", e);
        }
        stopSelf();
        Log.i(TAG, "VPN Service Stopped");
    }

    @Override
    public void run() {
        DatagramSocket tunnelSocket = null;
        try {
            tunnelSocket = new DatagramSocket();
            protect(tunnelSocket);

            mInterface = new Builder()
                    .setMtu(1500)
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .setSession("MaskedVPN")
                    .establish();

            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
            ByteBuffer packetBuffer = ByteBuffer.allocate(32767);
            InetAddress serverAddr = InetAddress.getByName(SERVER_ADDRESS);

            while (!Thread.interrupted()) {
                int length = in.read(packetBuffer.array());
                if (length > 0) {
                    byte[] rawData = packetBuffer.array();
                    byte[] maskedData = new byte[length];
                    for (int i = 0; i < length; i++) {
                        maskedData[i] = (byte) (rawData[i] ^ MASK_KEY);
                    }

                    DatagramPacket outPacket = new DatagramPacket(maskedData, length, serverAddr, SERVER_PORT);
                    tunnelSocket.send(outPacket);
                    packetBuffer.clear();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "VPN Loop Error", e);
        } finally {
            if (tunnelSocket != null) tunnelSocket.close();
            try { if (mInterface != null) mInterface.close(); } catch (Exception e) {}
        }
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }
}
