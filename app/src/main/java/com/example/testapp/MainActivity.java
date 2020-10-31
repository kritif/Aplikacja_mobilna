package com.example.testapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketState;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private WifiManager         wifiManager;

    public  Button              connectBtn;
    public  Button              websocketBtn;

    private Button              forwardBtn;
    private Button              rearBtn;
    private Button              leftBtn;
    private Button              rightBtn;
    private Button              turnLeftBtn;
    private Button              turnRightBtn;
    private Button              realTime;
    private Button              autonomic;
    private Button              autonomic_forward;
    private Button              autonomic_keepgoing;
    private Button              autonomic_stop;
    private Button              settings;
    private Button              update_settings;

    public ImageView            frontSensor;
    public ImageView            leftSensor;
    public ImageView            rightSensor;
    public ImageView            backSensor;

    private TextView            leftMotorPower;
    private TextView            rightMotorPower;
    private TextView            turnInterval;


    public  static final String     REALTIME =          "REALTIME";
    public  static final String     AUTONOMIC =         "AUTONOMIC";
    public  static final String     ROBOT_WIFI_NAME =   "RPI_motor";
    private static final String     ROBOT_WIFI_PASS =   "rpimotorrpi";
    private static final String     FRONT =             "FRONT";
    private static final String     TURN_LEFT =         "TLEFT";
    private static final String     TURN_RIGHT =        "TRIGHT";
    private static final String     LEFT =              "LEFT";
    private static final String     RIGHT =             "RIGHT";
    private static final String     BACK =              "BACK";
    private static final String     STOP =              "STOP";
    private static final String     AUT_GO_1 =          "A1GO";
    private static final String     AUT_GO_2 =          "A1GO";
    private static final String     AUT_STOP =          "ASTOP";

    public  boolean     wifiConnection =        false;
    public  boolean     wsConnection =          false;
    public  boolean     autonomicIsRunning =    false;
    private String      robotMODE =             REALTIME;

    WifiReceiver        wifiReceiver;
    WebSocket           ws = null;


    @SuppressLint("WifiManagerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifiReceiver = null;
        wifiReceiver = new WifiReceiver();
        wifiReceiver.setMainActivityHandler(this);
        IntentFilter intent = new IntentFilter("android.net.wifi.STATE_CHANGE");
        registerReceiver(wifiReceiver,intent);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 0);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 0);
        }
        //
        connectBtn          =   findViewById(R.id.scanBtn);
        websocketBtn        =   findViewById(R.id.wsBtn);
        forwardBtn          =   findViewById(R.id.forwardBtn);
        leftBtn             =   findViewById(R.id.leftBtn);
        rightBtn            =   findViewById(R.id.rightBtn);
        rearBtn             =   findViewById(R.id.rearBtn);
        turnLeftBtn         =   findViewById(R.id.turnLeft);
        turnRightBtn        =   findViewById(R.id.turnRight);
        autonomic           =   findViewById(R.id.autonomic);
        realTime            =   findViewById(R.id.realTime);
        frontSensor         =   findViewById(R.id.front_sensor);
        leftSensor          =   findViewById(R.id.left_sensor);
        rightSensor         =   findViewById(R.id.right_sensor);
        backSensor          =   findViewById(R.id.back_sensor);
        autonomic_forward   =   findViewById(R.id.autonomic_forward);
        autonomic_stop      =   findViewById(R.id.autonomic_stop);
        settings            =   findViewById(R.id.settings);
        update_settings     =   findViewById(R.id.update_set);
        leftMotorPower      =   findViewById(R.id.leftMotorPower);
        rightMotorPower     =   findViewById(R.id.rightMotorPower);
        turnInterval        =   findViewById(R.id.turnInterval);
        autonomic_keepgoing =   findViewById(R.id.keepgoing);

        realTime.setBackgroundColor(Color.GREEN);
        autonomic_forward.setVisibility(View.INVISIBLE);
        autonomic_keepgoing.setVisibility(View.INVISIBLE);
        autonomic_stop.setVisibility(View.INVISIBLE);

        leftMotorPower.setVisibility(View.INVISIBLE);
        rightMotorPower.setVisibility(View.INVISIBLE);
        turnInterval.setVisibility(View.INVISIBLE);
        update_settings.setVisibility(View.INVISIBLE);

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToWifi();
            }
        });
        websocketBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(wsConnection){
                    ws.disconnect();
                    wsConnection = false;
                    updateWebSButton();
                }else{
                    connectWebSocket();
                }

            }
        });

        forwardBtn.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(robotMODE==REALTIME && wsConnection) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        sendMessage(FRONT);
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        sendMessage(STOP);
                    }
                }
                return false;
            }
        });
        turnLeftBtn.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(robotMODE==REALTIME && wsConnection) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        sendMessage(TURN_LEFT);
                    }
                }
                return false;
            }
        });
        turnRightBtn.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(robotMODE==REALTIME && wsConnection) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        sendMessage(TURN_RIGHT);
                    }
                }
                return false;
            }
        });
        leftBtn.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(robotMODE==REALTIME && wsConnection) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        sendMessage(LEFT);
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        sendMessage(STOP);
                    }
                }
                return false;
            }
        });
        rightBtn.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(robotMODE==REALTIME && wsConnection) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        sendMessage(RIGHT);
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        sendMessage(STOP);
                    }
                }
                return false;
            }
        });
        rearBtn.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(robotMODE==REALTIME && wsConnection) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        sendMessage(BACK);
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        sendMessage(STOP);
                    }
                }
                return false;
            }
        });

        realTime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(robotMODE == AUTONOMIC) {
                    robotMODE = REALTIME;
                    switchButtons();
                    autonomic_forward.setBackgroundColor(Color.GRAY);
                    autonomic_stop.setBackgroundColor(Color.GRAY);
                    autonomicIsRunning = false;
                    sendMessage(AUT_STOP);
                }else{
                    switchButtons();
                }

            }
        });
        autonomic.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(robotMODE == REALTIME) {
                    robotMODE = AUTONOMIC;
                }
                switchButtons();
            }
        });

        autonomic_forward.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if(!autonomicIsRunning && wsConnection){
                    autonomicIsRunning = sendMessageWithResult(AUT_GO_1);
                    if(autonomicIsRunning) {
                        autonomic_keepgoing.setBackgroundColor(Color.RED);
                        autonomic_forward.setBackgroundColor(Color.GREEN);
                        autonomic_stop.setBackgroundColor(Color.RED);
                    }
                }
            }
        });

        autonomic_keepgoing.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if(!autonomicIsRunning && wsConnection){
                    autonomicIsRunning = sendMessageWithResult(AUT_GO_2);
                    if(autonomicIsRunning) {
                        autonomic_forward.setBackgroundColor(Color.RED);
                        autonomic_keepgoing.setBackgroundColor(Color.GREEN);
                        autonomic_stop.setBackgroundColor(Color.RED);
                    }
                }
            }
        });

        autonomic_stop.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if(autonomicIsRunning && wsConnection){
                    autonomicIsRunning = !sendMessageWithResult(AUT_STOP);
                    if(!autonomicIsRunning) {
                        autonomic_forward.setBackgroundColor(Color.RED);
                        autonomic_keepgoing.setBackgroundColor(Color.RED);
                        autonomic_stop.setBackgroundColor(Color.GREEN);
                    }
                }
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rearBtn.setVisibility(View.INVISIBLE);
                leftBtn.setVisibility(View.INVISIBLE);
                rightBtn.setVisibility(View.INVISIBLE);
                forwardBtn.setVisibility(View.INVISIBLE);
                turnLeftBtn.setVisibility(View.INVISIBLE);
                turnRightBtn.setVisibility(View.INVISIBLE);
                autonomic_forward.setVisibility(View.INVISIBLE);
                autonomic_keepgoing.setVisibility(View.INVISIBLE);
                autonomic_stop.setVisibility(View.INVISIBLE);
                leftMotorPower.setVisibility(View.VISIBLE);
                rightMotorPower.setVisibility(View.VISIBLE);
                turnInterval.setVisibility(View.VISIBLE);
                update_settings.setVisibility(View.VISIBLE);
            }
        });

        update_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wsConnection){
                    String leftMotor = leftMotorPower.getText().toString();
                    String rightMotor = rightMotorPower.getText().toString();
                    String turnTime = turnInterval.getText().toString();
                    sendMessage("LP:"+leftMotor+" RP:"+rightMotor+" TT:"+turnTime);
                }
            }
        });

    }

    private void switchButtons(){
        if(robotMODE==AUTONOMIC){
            realTime.setBackgroundColor(Color.RED);
            autonomic.setBackgroundColor(Color.GREEN);
            rearBtn.setVisibility(View.INVISIBLE);
            leftBtn.setVisibility(View.INVISIBLE);
            rightBtn.setVisibility(View.INVISIBLE);
            forwardBtn.setVisibility(View.INVISIBLE);
            turnLeftBtn.setVisibility(View.INVISIBLE);
            turnRightBtn.setVisibility(View.INVISIBLE);
            leftMotorPower.setVisibility(View.INVISIBLE);
            rightMotorPower.setVisibility(View.INVISIBLE);
            turnInterval.setVisibility(View.INVISIBLE);
            update_settings.setVisibility(View.INVISIBLE);
            autonomic_forward.setVisibility(View.VISIBLE);
            autonomic_keepgoing.setVisibility(View.VISIBLE);
            autonomic_stop.setVisibility(View.VISIBLE);
        }

        if(robotMODE==REALTIME){
            realTime.setBackgroundColor(Color.GREEN);
            autonomic.setBackgroundColor(Color.RED);
            rearBtn.setVisibility(View.VISIBLE);
            leftBtn.setVisibility(View.VISIBLE);
            rightBtn.setVisibility(View.VISIBLE);
            forwardBtn.setVisibility(View.VISIBLE);
            turnLeftBtn.setVisibility(View.VISIBLE);
            turnRightBtn.setVisibility(View.VISIBLE);
            autonomic_forward.setVisibility(View.INVISIBLE);
            autonomic_keepgoing.setVisibility(View.INVISIBLE);
            autonomic_stop.setVisibility(View.INVISIBLE);
            leftMotorPower.setVisibility(View.INVISIBLE);
            rightMotorPower.setVisibility(View.INVISIBLE);
            turnInterval.setVisibility(View.INVISIBLE);
            update_settings.setVisibility(View.INVISIBLE);
        }
    }

    private void connectToWifi(){
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + ROBOT_WIFI_NAME + "\"";
        conf.preSharedKey = "\""+ ROBOT_WIFI_PASS +"\"";
        conf.status = WifiConfiguration.Status.ENABLED;
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        int netID = wifiManager.addNetwork(conf);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netID, true);
        wifiManager.reconnect();


    };
    public void connectWebSocket() {
        WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(5000);

        try {
            ws = factory.createSocket("ws://192.168.4.1:8888/ws/");

            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    //Log.d("TAG", "onTextMessage: " + message);
                    if(message.equals("update")){
                        wsConnection = true;
                        updateWebSButton();
                    }else{
                        String[] data = message.split(" ");
                        String frontDist = data[0].split(":")[1];
                        String leftDist = data[1].split(":")[1];
                        String rightDist = data[2].split(":")[1];
                        String backDist = data[3].split(":")[1];
                        updateFrontSensorView(frontDist);
                        updateLeftSensorView(leftDist);
                        updateRightSensorView(rightDist);
                        updateBackSensorView(backDist);
                    }

                }
            });

            ws.connectAsynchronously();
        } catch (IOException e) {
            Log.d("TAG", "onTextMessage: error" );
            e.printStackTrace();
        }



    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (ws != null) {
            ws.disconnect();
            ws = null;
        }
    }

    public void sendMessage(String message){
        if (ws != null) {
            if (ws.getState() == WebSocketState.OPEN) {
                if (ws.isOpen()) {
                    ws.sendText(message);
                } else {
                    Log.i("websocket", "error");
                }
            } else {
                wsConnection = false;
                updateWebSButton();
                resetSensorImage();
            }
        }
    }

    public boolean sendMessageWithResult(String message){
        if (ws != null) {
            if (ws.getState() == WebSocketState.OPEN) {
                if (ws.isOpen()) {
                    ws.sendText(message);
                    return true;
                } else {
                    Log.i("websocket", "error");
                    return false;
                }
            } else {
                wsConnection = false;
                updateWebSButton();
                resetSensorImage();
                return false;
            }
        }else{
            return false;
        }
    }

    public void updateWifiButton(){
        connectBtn.setText((wifiConnection) ? "Connected to Robot WiFi" : "Not Connected to Robot WiFi");
        connectBtn.setBackgroundColor((wifiConnection) ? Color.parseColor("#00ff00") : Color.parseColor("#ff0000"));
    }
    public void updateWebSButton(){
        websocketBtn.setText((wsConnection) ? "Connected to Robot" : "Not Connected to Robot");
        websocketBtn.setBackgroundColor((wsConnection) ? Color.parseColor("#00ff00") : Color.parseColor("#ff0000"));
        if(!wsConnection){
            resetSensorImage();
        }

    }
    public void updateFrontSensorView(String dist){
        Double distance = Double.valueOf(dist);
        if(distance<=10.0) frontSensor.setImageResource(R.drawable.czujnik_4);
        if(distance>10.0)  frontSensor.setImageResource(R.drawable.czujnik_3);
        if(distance>20.0)  frontSensor.setImageResource(R.drawable.czujnik_2);
        if(distance>30.0)  frontSensor.setImageResource(R.drawable.czujnik_1);
        if(distance>40.0)  frontSensor.setImageResource(R.drawable.czujnik_0);
    }
    public void updateLeftSensorView(String dist){
        Double distance = Double.valueOf(dist);
        if(distance<=10.0) leftSensor.setImageResource(R.drawable.czujnik_4);
        if(distance>10.0) leftSensor.setImageResource(R.drawable.czujnik_3);
        if(distance>20.0) leftSensor.setImageResource(R.drawable.czujnik_2);
        if(distance>30.0) leftSensor.setImageResource(R.drawable.czujnik_1);
        if(distance>40.0) leftSensor.setImageResource(R.drawable.czujnik_0);
    }
    public void updateRightSensorView(String dist){
        Double distance = Double.valueOf(dist);
        if(distance<=10.0) rightSensor.setImageResource(R.drawable.czujnik_4);
        if(distance>10.0) rightSensor.setImageResource(R.drawable.czujnik_3);
        if(distance>20.0) rightSensor.setImageResource(R.drawable.czujnik_2);
        if(distance>30.0) rightSensor.setImageResource(R.drawable.czujnik_1);
        if(distance>40.0) rightSensor.setImageResource(R.drawable.czujnik_0);
    }
    public void updateBackSensorView(String dist){
        Double distance = Double.valueOf(dist);
        if(distance<=10.0) backSensor.setImageResource(R.drawable.czujnik_4);
        if(distance>10.0) backSensor.setImageResource(R.drawable.czujnik_3);
        if(distance>20.0) backSensor.setImageResource(R.drawable.czujnik_2);
        if(distance>30.0) backSensor.setImageResource(R.drawable.czujnik_1);
        if(distance>40.0) backSensor.setImageResource(R.drawable.czujnik_0);
    }

    private void resetSensorImage(){
        frontSensor.setImageResource(R.drawable.czujnik_0);
        leftSensor.setImageResource(R.drawable.czujnik_0);
        rightSensor.setImageResource(R.drawable.czujnik_0);
        backSensor.setImageResource(R.drawable.czujnik_0);
    }
}
