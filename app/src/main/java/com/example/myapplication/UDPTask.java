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

enum task {
    LEDon, LEDoff, Motor1on, Motor1off, Motor2on, Motor2off
}

public class UDPTask extends AsyncTask<Void, Void, Void> {
    // Wifi needs to be connected to NanoESP
    private task task;
    private String ip;
    private int port;
    private boolean isLEDOn;
    private String msg;

    public UDPTask(String msg, String ip, int port) {
        super();
        this.msg = msg;
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
            Log.i("UDP", msg);
            byte[] buf = msg.getBytes();
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}