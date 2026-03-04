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

    // FOR TESTING: Replace with your server's public IP
    private static final String SERVER_ADDRESS = "YOUR_PUBLIC_IP_HERE";
    private static final int SERVER_PORT = 9999;
    private static final byte MASK_KEY = 0x42; // Simple XOR key for masking

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mThread != null) { mThread.interrupt(); }
        mThread = new Thread(this, "MyVpnThread");
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void run() {
        DatagramSocket tunnelSocket = null;
        try {
            // 1. Create and PROTECT the socket
            // This prevents the socket's own traffic from being routed back into the VPN.
            tunnelSocket = new DatagramSocket();
            protect(tunnelSocket);

            // 2. Configure VPN Interface
            mInterface = new Builder()
                    .setMtu(1500)
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .setSession("MaskedVPN")
                    .establish();

            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
            ByteBuffer packetBuffer = ByteBuffer.allocate(32767);
            InetAddress serverAddr = InetAddress.getByName(SERVER_ADDRESS);

            Log.i(TAG, "Started masking traffic to " + SERVER_ADDRESS);

            while (!Thread.interrupted()) {
                int length = in.read(packetBuffer.array());
                if (length > 0) {
                    // --- MASKING LOGIC ---
                    byte[] rawData = packetBuffer.array();
                    byte[] maskedData = new byte[length];
                    for (int i = 0; i < length; i++) {
                        maskedData[i] = (byte) (rawData[i] ^ MASK_KEY); // XOR Masking
                    }

                    // Send masked packet to server
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
        if (mThread != null) mThread.interrupt();
        super.onDestroy();
    }
}
