package com.example.myapplication;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPReceive extends AsyncTask<Void, Void, Void> {
    private static final int BUFSIZE = 508;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected Void doInBackground(Void... voids) {
        int port = 55056;
        try (DatagramSocket socket = new DatagramSocket(port)) {
            DatagramPacket packet = new DatagramPacket(new byte[BUFSIZE], BUFSIZE);

            while (true) {
                socket.receive(packet);
                String data = new String(packet.getData(), 0, packet.getLength());
                System.out.println(data);
                Log.i("UDP DATA RECEIVED", "received: " + data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
