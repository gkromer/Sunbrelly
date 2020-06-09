package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private RelativeLayout myLayout = null;
    private SensorManager sensorManager;
    private Sensor light;
    private float referenceBrightness;
    private boolean isReferenceInitialized = false;
    private int signal1 = 0;
    private int signal2 = 0;
    private final int MAX_SIGNAL = 255;  //maximum der Arduino PWM
    private final int MIN_SIGNAL = -255;
    private final int MIN_MOTOR_SPEED = 150;
    private final int MIN_MOTOR_SPEED_BACKWARDS = -150;
    private final int sensitivity = 100; //how many lux the light needs to increase to start the motor
    TextView brightnessTextView;
    TextView signal1TextView;
    TextView signal2TextView;
    TextView stopStart;
    TextView toggleLEDBtn;
    private float currentLux;
    private boolean hasLightSensor = false;
    private TextView serverSignal;

    OkHttpClient postClient = new OkHttpClient();
    Boolean isSearchingShadow = false;
    Boolean isStopped = false;
    private Button startMotor1;
    private Button startMotor2;
    private Button backMotor1;
    private Button backMotor2;
    private Handler mHandler;
    private int holdingtime;
    private final int MIN_HOLDING_TIME = 1; // wieviele Schleifendurchläufe ein Button gedrückt werden muss, bis er als gehalten statt geklickt gilt.

    private String ip = "192.168.4.1"; //default values for NanoESP
    private int port = 55057;
    boolean isLEDOn;

    private long timeStartedSearchingShadow;
    private long now;
    private long timeElapsed;
    int guessedShadeDirection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startMotor1 = (Button) findViewById(R.id.startMotor1);
        startMotor2 = (Button) findViewById(R.id.startMotor2);
        backMotor1 = (Button) findViewById(R.id.backMotor1);
        backMotor2 = (Button) findViewById(R.id.backMotor2);
        brightnessTextView = (TextView) findViewById(R.id.lightSignal);
        signal1TextView = (TextView) findViewById(R.id.textViewSignalMotor1);
        signal2TextView = (TextView) findViewById(R.id.textViewSignalMotor2);
        serverSignal = (TextView) findViewById(R.id.serverSignal);
        stopStart = (TextView) findViewById(R.id.stop);
        toggleLEDBtn = (TextView) findViewById(R.id.toggleLEDBtn);


        // new UDPReceive().execute();

        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        checkIfLightSensorIsAvailable();

        //sendMotorSignal(signal);

        startMotor1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;

                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        signal1 = 0;
                        stopMotor(1);
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    holdingtime++;
                    signal1++;
                    motorForwardWithSignal(1, signal1);
                    mHandler.postDelayed(this, 500);
                }
            };
        });

        backMotor1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;

                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        signal1 = 0;
                        stopMotor(1);
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    signal1--;
                    holdingtime++;
                    motorBackwardWithSignal(1, signal1);
                    mHandler.postDelayed(this, 500);
                }
            };
        });

        startMotor2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;

                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        signal2 = 0;
                        stopMotor(2);
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    holdingtime++;
                    signal2++;
                    motorForwardWithSignal(2, signal2);
                    mHandler.postDelayed(this, 500);
                }
            };
        });

        backMotor2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 500);
                        break;

                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        signal2 = 0;
                        stopMotor(2);
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    signal2--;
                    holdingtime++;
                    motorBackwardWithSignal(2, signal2);
                    mHandler.postDelayed(this, 500);
                }
            };
        });
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
        if (holdingtime < MIN_HOLDING_TIME) startMotor(1);
        holdingtime = 0;
    }

    public void startMotor2(View view) {
        if (holdingtime < MIN_HOLDING_TIME) startMotor(2);
        holdingtime = 0;
    }

    public void backMotor1(View view) {
        if (holdingtime < MIN_HOLDING_TIME) backMotor(1);
        holdingtime = 0;
    }

    public void backMotor2(View view) {
        if (holdingtime < MIN_HOLDING_TIME) backMotor(2);
        holdingtime = 0;
    }

    public void startMotor(int motorNumber) {
        Log.i("Motor", "started Motor " + motorNumber);
        new UDPTask("motor" + motorNumber + "Forwards", ip, port).execute();
        if (motorNumber == 1) signal1TextView.setText(Integer.toString(MAX_SIGNAL));
        if (motorNumber == 2) signal2TextView.setText(Integer.toString(MAX_SIGNAL));
    }

    public void stopMotor(int motorNumber) {
        Log.i("Motor", "stopped Motor " + motorNumber);
        new UDPTask("motor" + motorNumber + "Stop", ip, port).execute();
        if (motorNumber == 1) signal1TextView.setText(Integer.toString(0));
        if (motorNumber == 2) signal2TextView.setText(Integer.toString(0));
    }

    public void backMotor(int motorNumber) {
        Log.i("Motor", "started Motor " + motorNumber + " backwards");
        new UDPTask("motor" + motorNumber + "Backwards", ip, port).execute();
        if (motorNumber == 1) signal1TextView.setText(Integer.toString(-MAX_SIGNAL));
        if (motorNumber == 2) signal2TextView.setText(Integer.toString(-MAX_SIGNAL));
    }

    public void motorForwardWithSignal(int motorNumber, int signal) {
        String formattedSignal = null;
        if (motorNumber == 1) {
            if (signal < MIN_MOTOR_SPEED) this.signal1 = MIN_MOTOR_SPEED;
            formattedSignal = String.format("%03d", this.signal1);  // erzeugt aus Signal immer eine 3 Stellige Zahl mit vorne aufgefüllten Nullen (signal geht von 001 bis 255)
        } else if (motorNumber == 2) {
            if (signal < MIN_MOTOR_SPEED) this.signal2 = MIN_MOTOR_SPEED;
            formattedSignal = String.format("%03d", this.signal2);  // erzeugt aus Signal immer eine 3 Stellige Zahl mit vorne aufgefüllten Nullen (signal geht von 001 bis 255)
        }
        Log.i("Motor", "started Motor " + motorNumber + " vorwärts mit Geschwindigkeit: " + formattedSignal);
        new UDPTask("motor" + motorNumber + "ForwardWithSignal" + formattedSignal, ip, port).execute();
        if (motorNumber == 1) signal1TextView.setText(Integer.toString(this.signal1));
        if (motorNumber == 2) signal2TextView.setText(Integer.toString(this.signal2));
    }

    public void motorBackwardWithSignal(int motorNumber, int signal) {
        String formattedSignal = null;
        int absSignal;
        if (motorNumber == 1) {
            if (signal > MIN_MOTOR_SPEED_BACKWARDS) this.signal1 = MIN_MOTOR_SPEED_BACKWARDS;
            absSignal = Math.abs(this.signal1);
            formattedSignal = String.format("%03d", absSignal);  // erzeugt aus Signal immer eine 3 Stellige Zahl mit vorne aufgefüllten Nullen (signal geht von 001 bis 255)
        } else if (motorNumber == 2) {
            if (signal > MIN_MOTOR_SPEED_BACKWARDS) this.signal2 = -MIN_MOTOR_SPEED;
            absSignal = Math.abs(this.signal2);
            formattedSignal = String.format("%03d", absSignal);  // erzeugt aus Signal immer eine 3 Stellige Zahl mit vorne aufgefüllten Nullen (signal geht von 001 bis 255)
        }
        Log.i("Motor", "started Motor " + motorNumber + " rückwärts mit Geschwindigkeit: " + formattedSignal);
        new UDPTask("motor" + motorNumber + "BackwardWithSignal" + formattedSignal, ip, port).execute();
        if (motorNumber == 1) signal1TextView.setText(Integer.toString(this.signal1));
        if (motorNumber == 2) signal2TextView.setText(Integer.toString(this.signal2));
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
        this.signal1 = 0;
        this.signal2 = 0;
        stopMotor(1);
        stopMotor(2);
        // sendMsgToServer("User closed App");

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        // sendMsgToServer("User opened App");
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {  // TODO:???
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
                if ((currentLux - referenceBrightness) > sensitivity) {
                    if (!isSearchingShadow) timeStartedSearchingShadow = System.currentTimeMillis();
                    guessedShadeDirection = searchShadow();
                    isSearchingShadow = true;
                }
                if (((currentLux - referenceBrightness) <= sensitivity) && isSearchingShadow) {
                    signal1 = 0;
                    signal2 = 0;
                    stopMotor(1);
                    stopMotor(2);
                    //          sendMotorSignal(signal);
                    isSearchingShadow = false;
                }
            }
        }
        signal1TextView.setText(Integer.toString(this.signal1));
        signal2TextView.setText(Integer.toString(this.signal2));
    }

    public void searchInDirection(int approxDirectionShadow) {
        switch (approxDirectionShadow) {
            case 1:
                signal2 = 0;
                stopMotor(2);
                increaseSignal(1);
                motorForwardWithSignal(1, signal1);
                break;
            case 2:
                signal1 = 0;
                stopMotor(1);
                increaseSignal(2);
                motorForwardWithSignal(2, signal2);
                break;
            case 3:
                signal2 = 0;
                stopMotor(2);
                decreaseSignal(1);
                motorBackwardWithSignal(1, signal1);
                break;
            case 4:
                signal1 = 0;
                stopMotor(1);
                decreaseSignal(2);
                motorBackwardWithSignal(2, signal2);
                break;
        }
    }

    public int searchShadow() {
        now = System.currentTimeMillis();
        timeElapsed = now - timeStartedSearchingShadow;
        Log.i("info", "timeElapsed: " + timeElapsed);
        if (now - timeStartedSearchingShadow >= 0 && now - timeStartedSearchingShadow < 3000) {
            guessedShadeDirection = 1;
            searchInDirection(guessedShadeDirection);
        } else if (now - timeStartedSearchingShadow >= 3000 && now - timeStartedSearchingShadow < 6000) {
            guessedShadeDirection = 2;
            searchInDirection(guessedShadeDirection);
        } else if (now - timeStartedSearchingShadow >= 6000 && now - timeStartedSearchingShadow < 12000) {
            guessedShadeDirection = 3;
            searchInDirection(guessedShadeDirection);
        } else if (now - timeStartedSearchingShadow >= 12000 && now - timeStartedSearchingShadow < 18000) {
            guessedShadeDirection = 4;
            searchInDirection(guessedShadeDirection);
        } else if (now - timeStartedSearchingShadow >= 18000 && now - timeStartedSearchingShadow < 27000) {
            guessedShadeDirection = 1;
            searchInDirection(guessedShadeDirection);
        } else if (now - timeStartedSearchingShadow >= 27000 && now - timeStartedSearchingShadow < 36000) {
            guessedShadeDirection = 2;
            searchInDirection(guessedShadeDirection);
        } else if (now - timeStartedSearchingShadow >= 36000 && now - timeStartedSearchingShadow < 48000) {
            guessedShadeDirection = 3;
            searchInDirection(guessedShadeDirection);
        } else if (now - timeStartedSearchingShadow >= 48000 && now - timeStartedSearchingShadow < 60000) {
            guessedShadeDirection = 4;
            searchInDirection(guessedShadeDirection);
        }
        return guessedShadeDirection;
    }

    public void increaseSignal(int motorNumber) {
        if (motorNumber == 1) {
            if (signal1 < MAX_SIGNAL) signal1 += 1;
            if (signal1 < MIN_MOTOR_SPEED) signal1 = MIN_MOTOR_SPEED;
        } else if (motorNumber == 2) {
            if (signal2 < MAX_SIGNAL) signal2 += 1;
            if (signal2 < MIN_MOTOR_SPEED) signal2 = MIN_MOTOR_SPEED;
        }
    }

    public void decreaseSignal(int motorNumber) {
        if (motorNumber == 1) {
            if (signal1 > MIN_SIGNAL) signal1 -= 1;
            if (signal1 > MIN_MOTOR_SPEED_BACKWARDS) signal1 = MIN_MOTOR_SPEED_BACKWARDS;
        } else if (motorNumber == 2) {
            if (signal2 > MIN_SIGNAL) signal2 -= 1;
            if (signal2 > MIN_MOTOR_SPEED_BACKWARDS) signal2 = MIN_MOTOR_SPEED_BACKWARDS;
        }
    }

    public void clickLeft(View view) {
        Log.i("info", "Button links wurde geklickt!");
        if (signal1 >= (MIN_SIGNAL + 10)) {
            signal1 -= 10;
            motorBackwardWithSignal(1, signal1);
        }
        if (signal2 >= (MIN_SIGNAL + 10)) {
            signal2 -= 10;
            motorBackwardWithSignal(2, signal2);
        }
        //sendMotorSignal(signal);
    }

    public void clickRight(View view) {
        Log.i("info", "Button rechts wurde geklickt!");
        if (signal1 <= (MAX_SIGNAL - 10)) {
            signal1 += 10;
            motorForwardWithSignal(1, signal1);
        }
        if (signal2 <= (MAX_SIGNAL - 10)) {
            signal2 += 10;
            motorForwardWithSignal(2, signal2);
        }
        //sendMotorSignal(signal);
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
            signal1 = 0;
            signal2 = 0;
            signal1TextView.setText(Integer.toString(signal1));
            signal2TextView.setText(Integer.toString(signal1));
            stopMotor(1);
            stopMotor(2);
            // sendMotorSignal(signal);
            Toast.makeText(MainActivity.this, "Lichtsteuerung ausgeschaltet.", Toast.LENGTH_SHORT).show();
        } else {
            isStopped = false;
            stopStart.setText("Stop");
            Toast.makeText(MainActivity.this, "Lichtsteuerung eingeschaltet.", Toast.LENGTH_SHORT).show();
        }
    }
}

//    public void sendMotorSignal(int signal) {
//        Log.d("OKHTTP", "Post signal function called");
//        String url = "https://shielded-everglades-18448.herokuapp.com/postSignal"; // connection to Java server
//        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
//        JSONObject actualdata = new JSONObject();
//        try {
//            actualdata.put("signal", Integer.toString(signal));
//        } catch (JSONException e) {
//            Log.d("OKHHTP", "JSON Exception");
//            e.printStackTrace();
//        }
//        RequestBody body = RequestBody.create(JSON, actualdata.toString());
//        Log.d("OKHTTP", "RequestBody created");
//        Request newReq = new Request.Builder()
//                .url(url)
//                .post(body)
//                .build();
//        Log.d("OKHTTP", "Request build");
//
//        postClient.newCall(newReq).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.d("OKHTTP", "FAILED");
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if (response.isSuccessful()) {
//                    final String newRes = response.body().string();
//                    Log.d("OKHTTP", "onResponse() called");
//                    MainActivity.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            serverSignal.setText(newRes);
//                            Log.d("OKHTTP", "Request done, got the response");
//                            Log.d("OKHTTP", newRes);
//                        }
//                    });
//                }
//            }
//
//        });
//    }
//
//    public void sendMsgToServer(String msg) {
//        Log.d("OKHTTP", "Post message function called");
//        String url = "https://shielded-everglades-18448.herokuapp.com/postMsg";
//        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
//        JSONObject actualdata = new JSONObject();
//        try {
//            actualdata.put("msg", msg);
//        } catch (JSONException e) {
//            Log.d("OKHHTP", "JSON Exception");
//            e.printStackTrace();
//        }
//        RequestBody body = RequestBody.create(JSON, actualdata.toString());
//        Log.d("OKHTTP", "RequestBody created");
//        Request newReq = new Request.Builder()
//                .url(url)
//                .post(body)
//                .build();
//        Log.d("OKHTTP", "Request build");
//
//        postClient.newCall(newReq).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.d("OKHTTP", "FAILED");
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if (response.isSuccessful()) {
//                    final String newRes = response.body().string();
//                    Log.d("OKHTTP", "onResponse() called");
//                    MainActivity.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Log.d("OKHTTP", "Request done, got the response");
//                            Log.d("OKHTTP", newRes);
//                        }
//                    });
//                }
//            }
//        });
//    }
//}
