package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.appinventor.components.runtime.Form;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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

        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        checkIfLightSensorIsAvailable();

        sendMotorSignal(signal);
    }

    public void toggleLED(View view) {
        //UDP Client erstellen
        Log.i("UDP", Boolean.toString(isLEDOn));
        new ToggleLED(isLEDOn, ip, port).execute();
        isLEDOn = !isLEDOn;
    }


    private void checkIfLightSensorIsAvailable() {
        //get list of supported Sensors
        List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : list) {
            if (sensor.getName().contains("Light") || sensor.getName().contains("light")) {
                // maxLux = light.getMaximumRange();
                hasLightSensor = true;
                Log.i("light sensor", "exists!");

                //Log.i("light sensor version", Integer.toString(sensor.getVersion()));
            }
            Log.i("sensor", sensor.getName() + sensor.toString());
        }
        //error message, if no light sensor was found
        if (!hasLightSensor) {
            Log.i("info", "Mensch Kerle, es konnte kein Lichtsensor gefunden werden...");
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
                if ((currentLux - referenceBrightness) > 300 && signal < maxSignal) {
                    signal += 1;
                    signalTextView.setText(Integer.toString(signal));
                    sendMotorSignal(signal);
                    isSearchingShadow = true;

                }
                if (((currentLux - referenceBrightness) <= 300) && isSearchingShadow) {
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

    public void sendMotorSignal(int signal) {


        Log.d("OKHTTP", "Post signal function called");
        String url = "https://shielded-everglades-18448.herokuapp.com/postSignal";
        // String url = "https://reqres.in/api/users";
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

    public void setClicked(View view) {
        referenceBrightness = currentLux;
        isReferenceInitialized = true;
    }

    public void stopClicked(View view) {
        if (!isStopped) {
            isStopped = true;
            stopStart.setText("Start");
            signal = 0;
            signalTextView.setText(Integer.toString(signal));
            sendMotorSignal(signal);
        } else {
            isStopped = false;
            stopStart.setText("Stop");
            signal = 0;
            signalTextView.setText(Integer.toString(signal));
            sendMotorSignal(signal);
        }

    }

    //Http get request
//    public void getRequest() throws MalformedURLException {
//        URL url = new URL("https://jsonplaceholder.typicode.com/posts/1");
//        HttpURLConnection client = null;
//        try {
//            client = (HttpURLConnection) url.openConnection();
//            client.setRequestMethod("POST");
//            client.setRequestProperty("Key", "Value");
//            client.setDoOutput(true);
//            OutputStream outputPost = new BufferedOutputStream(client.getOutputStream());
//            // writeStream(outputPost);
//            outputPost.flush();
//            outputPost.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.i("fail", "Get request failed");
//        }  finally {
//            if(client != null) // Stelle sicher, dass die Verbindung nicht null ist.
//                client.disconnect();
//        }
//    }

//    //Image download
//    public void downloadImage(View view) {
//        ImageDownloader task = new ImageDownloader();
//        Bitmap myImage;
//        try {
//            myImage = task.execute("https://upload.wikimedia.org/wikipedia/en/0/02/Homer_Simpson_2006.png").get();
//            Log.i("info", "Bild geladen");
//            imageView.setImageBitmap(myImage);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
//
//        @Override
//        protected Bitmap doInBackground(String... urls) {
//
//            try {
//                URL url = new URL(urls[0]);
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.connect();
//                InputStream inputStream = connection.getInputStream();
//                Bitmap myBitmap = BitmapFactory.decodeStream(inputStream);
//
//                return myBitmap;
//            } catch (MalformedURLException e) {
//                e.printStackTrace();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//    }
}
