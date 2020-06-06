package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class ToggleLED extends AsyncTask<Void, Void, Void> {
    // Wifi needs to be connected to NanoESP
    private String ip;
    private int port;
    private boolean isLEDOn;
    private String msg;

    public ToggleLED(boolean isLEDOn, String ip, int port) {
        super();
        this.isLEDOn = isLEDOn;
        this.ip = ip;
        this.port = port;
    }

    @SuppressLint("LongLogTag")
    @Override
    protected Void doInBackground(Void... voids) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(ip);
            Log.i("UDP", address.getHostAddress());
            if (isLEDOn) msg = "led0";
            if (!isLEDOn) msg = "led1";
            Log.i("UDP", Boolean.toString(isLEDOn));
            Log.i("UDP", msg);
            byte[] buf = msg.getBytes();
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
            //   socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}