package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor light;
    private float referenceBrightness;
    private boolean isReferenceInitialized = false;
    private int signal = 0;
    private final int maxSignal = 50;
    private final int minSignal = -50;
    private final int sensitivity = 100; //how many lux the light needs to increase to start the motor
    TextView brightnessTextView;
    TextView signalTextView;
    TextView stopStart;
    TextView toggleLEDBtn;
    private float currentLux;
    private boolean hasLightSensor = false;
    private TextView serverSignal;
    OkHttpClient postClient = new OkHttpClient();
    Boolean isSearchingShadow = false;
    Boolean isStopped = false;

    private String ip = "192.168.4.1"; //default values for NanoESP
    private int port = 55057;
    boolean isLEDOn;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        brightnessTextView = (TextView) findViewById(R.id.lightSignal);
        signalTextView = (TextView) findViewById(R.id.electricitySignal);
        serverSignal = (TextView) findViewById(R.id.serverSignal);
        stopStart = (TextView) findViewById(R.id.stop);
        toggleLEDBtn = (TextView) findViewById(R.id.toggleLEDBtn);

        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        checkIfLightSensorIsAvailable();

        sendMotorSignal(signal);
    }

    public void toggleLED(View view) {
        Log.i("UDP", Boolean.toString(isLEDOn));
        if (isLEDOn) {
            new UDPTask("led0", ip, port).execute();
            Toast.makeText(MainActivity.this, "LED ausgeschaltet.", Toast.LENGTH_SHORT).show();
            toggleLEDBtn.setText("LED an");
        } else {
            new UDPTask("led1", ip, port).execute();
            Toast.makeText(MainActivity.this, "LED angeschaltet.", Toast.LENGTH_SHORT).show();
            toggleLEDBtn.setText("LED aus");
        }
        isLEDOn = !isLEDOn;
    }

    public void startMotor1(View view) {
        startMotor(1);
        }

    public void startMotor2(View view) {
        startMotor(2);
    }

    public void startMotor(int motorNumber) {
        Log.i("Motor", "started Motor " + motorNumber);
        new UDPTask("motorOn", ip, port).execute();
        Toast.makeText(MainActivity.this, "Motor 1 angeschaltet.", Toast.LENGTH_SHORT).show();
    }

    private void checkIfLightSensorIsAvailable() {
        //get list of supported Sensors
        List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : list) {
            if (sensor.getName().contains("Light") || sensor.getName().contains("light")) {
                hasLightSensor = true;
                Log.i("light sensor", "exists!");
            }
            Log.i("sensor", sensor.getName() + sensor.toString());
        }
        if (!hasLightSensor) {
            Log.i("info", "Kein Lichtsensor gefunden...");
            for (Sensor sensor : list) {
                Log.i("sensor", sensor.getName() + sensor.toString());
            }
        }
        Log.i("light sensor", "" + hasLightSensor);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        sendMsgToServer("User closed App");
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        sendMsgToServer("User opened App");
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (!isStopped) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                currentLux = event.values[0];
                brightnessTextView.setText(Float.toString(currentLux));
                if (!isReferenceInitialized) {
                    referenceBrightness = currentLux;
                    isReferenceInitialized = true;
                }
                if ((currentLux - referenceBrightness) > sensitivity && signal < maxSignal) {
                    //startMotor(1);
                    signal += 1;
                    signalTextView.setText(Integer.toString(signal));
                    sendMotorSignal(signal);
                    isSearchingShadow = true;
                }
                if (((currentLux - referenceBrightness) <= sensitivity) && isSearchingShadow) {
                    signal = 0;
                    signalTextView.setText(Integer.toString(signal));
                    sendMotorSignal(signal);
                    isSearchingShadow = false;
                }
            }
        }

    }

    public void clickLeft(View view) {
        if (signal >= (minSignal + 10)) {
            Log.i("info", "Button links wurde geklickt!");
            signal -= 10;
            signalTextView.setText(Integer.toString(signal));
        }
        sendMotorSignal(signal);
    }

    public void clickRight(View view) {
        if (signal <= (maxSignal - 10)) {
            Log.i("info", "Button rechts wurde geklickt!");
            signal += 10;
            signalTextView.setText(Integer.toString(signal));
        }
        sendMotorSignal(signal);
    }

    public void setClicked(View view) {
        referenceBrightness = currentLux;
        isReferenceInitialized = true;
        Toast.makeText(MainActivity.this, "Helligkeit gesetzt.", Toast.LENGTH_SHORT).show();
    }

    public void stopClicked(View view) {
        if (!isStopped) {
            isStopped = true;
            stopStart.setText("Start");
            signal = 0;
            signalTextView.setText(Integer.toString(signal));
            sendMotorSignal(signal);
            Toast.makeText(MainActivity.this, "Lichtsteuerung ausgeschaltet.", Toast.LENGTH_SHORT).show();
        } else {
            isStopped = false;
            stopStart.setText("Stop");
            signal = 0;
            signalTextView.setText(Integer.toString(signal));
            sendMotorSignal(signal);
            Toast.makeText(MainActivity.this, "Lichtsteuerung eingeschaltet.", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendMotorSignal(int signal) {
        Log.d("OKHTTP", "Post signal function called");
        String url = "https://shielded-everglades-18448.herokuapp.com/postSignal"; // connection to Java server
        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        JSONObject actualdata = new JSONObject();
        try {
            actualdata.put("signal", Integer.toString(signal));
        } catch (JSONException e) {
            Log.d("OKHHTP", "JSON Exception");
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(JSON, actualdata.toString());
        Log.d("OKHTTP", "RequestBody created");
        Request newReq = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Log.d("OKHTTP", "Request build");

        postClient.newCall(newReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("OKHTTP", "FAILED");
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String newRes = response.body().string();
                    Log.d("OKHTTP", "onResponse() called");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            serverSignal.setText(newRes);
                            Log.d("OKHTTP", "Request done, got the response");
                            Log.d("OKHTTP", newRes);
                        }
                    });
                }
            }

        });
    }

    public void sendMsgToServer(String msg) {
        Log.d("OKHTTP", "Post message function called");
        String url = "https://shielded-everglades-18448.herokuapp.com/postMsg";
        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        JSONObject actualdata = new JSONObject();
        try {
            actualdata.put("msg", msg);
        } catch (JSONException e) {
            Log.d("OKHHTP", "JSON Exception");
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(JSON, actualdata.toString());
        Log.d("OKHTTP", "RequestBody created");
        Request newReq = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Log.d("OKHTTP", "Request build");

        postClient.newCall(newReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("OKHTTP", "FAILED");
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String newRes = response.body().string();
                    Log.d("OKHTTP", "onResponse() called");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("OKHTTP", "Request done, got the response");
                            Log.d("OKHTTP", newRes);
                        }
                    });
                }
            }
        });
    }
}
