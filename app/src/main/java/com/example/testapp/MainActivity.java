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
import android.os.CountDownTimer;
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
import java.util.ArrayList;
import java.util.List;


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
    private Button              autonomic_record;
    private Button              autonomic_stop;
    private Button              settings;
    private Button              update_settings;

    public ImageView            frontSensor;
    public ImageView            leftSensor;
    public ImageView            rightSensor;
    public ImageView            backSensor;

    private TextView            leftMotorPower;
    private TextView            rightMotorPower;
    private TextView            turnLeftInterval;
    private TextView            turnRightInterval;
    private TextView            leftMotorPowerAtLeftTurn;
    private TextView            rightMotorPowerAtLeftTurn;
    private TextView            leftMotorPowerAtRightTurn;
    private TextView            rightMotorPowerAtRightTurn;


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
    private static final String     AUT_GO_2 =          "A2GO";
    private static final String     AUT_GO_3 =          "A3GO";
    private static final String     AUT_STOP =          "ASTOP";

    public  boolean     wifiConnection =        false;
    public  boolean     wsConnection =          false;
    public  boolean     autonomicIsRunning =    false;
    public  boolean     recordingMove =         false;
    private String      robotMODE =             REALTIME;
    public List trackData = new ArrayList();
    public CountDownTimer timer;
    public double         time = 0.0d;

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
        timer = new CountDownTimer(3600000, 10) {
            public void onTick(long millisUntilFinished) {
                time+=0.01d;
            }

            public void onFinish() {

            }
        };
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
        turnLeftInterval    =   findViewById(R.id.turnLeftInterval);
        turnRightInterval   =   findViewById(R.id.turnRightInterval);
        leftMotorPowerAtLeftTurn    =   findViewById(R.id.leftMotorPowerTurnLeft);
        rightMotorPowerAtLeftTurn   =   findViewById(R.id.rightMotorPowerTurnLeft);
        leftMotorPowerAtRightTurn   =   findViewById(R.id.leftMotorPowerTurnRight);
        rightMotorPowerAtRightTurn  =   findViewById(R.id.rightMotorPowerTurnRight);
        autonomic_keepgoing =   findViewById(R.id.keepgoing);
        autonomic_record = findViewById(R.id.record_move);

        autonomic_forward.setVisibility(View.INVISIBLE);
        autonomic_keepgoing.setVisibility(View.INVISIBLE);
        autonomic_record.setVisibility(View.INVISIBLE);
        autonomic_stop.setVisibility(View.INVISIBLE);

        leftMotorPower.setVisibility(View.INVISIBLE);
        rightMotorPower.setVisibility(View.INVISIBLE);
        turnLeftInterval.setVisibility(View.INVISIBLE);
        turnRightInterval.setVisibility(View.INVISIBLE);
        leftMotorPowerAtLeftTurn.setVisibility(View.INVISIBLE);
        rightMotorPowerAtLeftTurn.setVisibility(View.INVISIBLE);
        leftMotorPowerAtRightTurn.setVisibility(View.INVISIBLE);
        rightMotorPowerAtRightTurn.setVisibility(View.INVISIBLE);
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
                        if(recordingMove){
                            time = 0.0d;
                            timer.start();
                        }
                        sendMessage(FRONT);
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        sendMessage(STOP);
                        timer.cancel();
                        trackData.add(0,"back:"+Double.toString(time));
                        time = 0.0d;
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
                        if(recordingMove){
                            time = 0.0d;
                            timer.start();
                        }
                        sendMessage(LEFT);
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        sendMessage(STOP);
                        timer.cancel();
                        trackData.add(0,"right:"+Double.toString(time));
                        time = 0.0d;
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
                        if(recordingMove){
                            time = 0.0d;
                            timer.start();
                        }
                        sendMessage(RIGHT);
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        sendMessage(STOP);
                        timer.cancel();
                        trackData.add(0,"left:"+Double.toString(time));;
                        time = 0.0d;
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
                        if(recordingMove){
                            time = 0.0d;
                            timer.start();
                        }
                        sendMessage(BACK);
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        sendMessage(STOP);
                        timer.cancel();
                        trackData.add(0,"front:"+Double.toString(time));;
                        time = 0.0d;
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
        autonomic_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(recordingMove){
                    recordingMove = false;
                    if(wsConnection) {
                        String track = "";
                        for(int i=0; i<trackData.size(); i++) track += trackData.get(i).toString()+";";
                        autonomicIsRunning = sendMessageWithResult(AUT_GO_3+"="+track);
                    }
                }else{
                    recordingMove = true;
                    trackData.clear();
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
                autonomic_record.setVisibility(View.INVISIBLE);
                autonomic_stop.setVisibility(View.INVISIBLE);
                leftMotorPower.setVisibility(View.VISIBLE);
                rightMotorPower.setVisibility(View.VISIBLE);
                turnLeftInterval.setVisibility(View.VISIBLE);
                turnRightInterval.setVisibility(View.VISIBLE);
                leftMotorPowerAtLeftTurn.setVisibility(View.VISIBLE);
                rightMotorPowerAtLeftTurn.setVisibility(View.VISIBLE);
                leftMotorPowerAtRightTurn.setVisibility(View.VISIBLE);
                rightMotorPowerAtRightTurn.setVisibility(View.VISIBLE);
                update_settings.setVisibility(View.VISIBLE);
            }
        });

        update_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wsConnection){
                    String leftMotorForward = leftMotorPower.getText().toString();
                    String rightMotorForward = rightMotorPower.getText().toString();
                    String turnLeftTime = turnLeftInterval.getText().toString();
                    String turnRightTime = turnRightInterval.getText().toString();
                    String leftMpAtLeftTurn = leftMotorPowerAtLeftTurn.getText().toString();
                    String rightMpAtLeftTurn = rightMotorPowerAtLeftTurn.getText().toString();
                    String leftMpAtRightTurn = leftMotorPowerAtRightTurn.getText().toString();
                    String rightMpAtRightTurn = rightMotorPowerAtRightTurn.getText().toString();
                    sendMessage("LP:"+leftMotorForward+" RP:"+rightMotorForward+" TLT:"+turnLeftTime+" TRT:"+turnRightTime+" LMPL:"+leftMpAtLeftTurn+" RMPL:"+rightMpAtLeftTurn+" LMPR:"+leftMpAtRightTurn+" RMPR:"+rightMpAtRightTurn );
                }
            }
        });

    }

    private void switchButtons(){
        if(robotMODE==AUTONOMIC){
            realTime.setBackgroundResource(R.drawable.realtime_off);
            autonomic.setBackgroundResource(R.drawable.autonomic_on);
            rearBtn.setVisibility(View.INVISIBLE);
            leftBtn.setVisibility(View.INVISIBLE);
            rightBtn.setVisibility(View.INVISIBLE);
            forwardBtn.setVisibility(View.INVISIBLE);
            turnLeftBtn.setVisibility(View.INVISIBLE);
            turnRightBtn.setVisibility(View.INVISIBLE);
            leftMotorPower.setVisibility(View.INVISIBLE);
            rightMotorPower.setVisibility(View.INVISIBLE);
            turnLeftInterval.setVisibility(View.INVISIBLE);
            turnRightInterval.setVisibility(View.INVISIBLE);
            leftMotorPowerAtLeftTurn.setVisibility(View.INVISIBLE);
            rightMotorPowerAtLeftTurn.setVisibility(View.INVISIBLE);
            leftMotorPowerAtRightTurn.setVisibility(View.INVISIBLE);
            rightMotorPowerAtRightTurn.setVisibility(View.INVISIBLE);
            update_settings.setVisibility(View.INVISIBLE);
            autonomic_forward.setVisibility(View.VISIBLE);
            autonomic_keepgoing.setVisibility(View.VISIBLE);
            autonomic_record.setVisibility(View.VISIBLE);
            autonomic_stop.setVisibility(View.VISIBLE);
        }

        if(robotMODE==REALTIME){
            realTime.setBackgroundResource(R.drawable.realtime_on);
            autonomic.setBackgroundResource(R.drawable.autonomic_off);
            rearBtn.setVisibility(View.VISIBLE);
            leftBtn.setVisibility(View.VISIBLE);
            rightBtn.setVisibility(View.VISIBLE);
            forwardBtn.setVisibility(View.VISIBLE);
            turnLeftBtn.setVisibility(View.VISIBLE);
            turnRightBtn.setVisibility(View.VISIBLE);
            autonomic_forward.setVisibility(View.INVISIBLE);
            autonomic_keepgoing.setVisibility(View.INVISIBLE);
            autonomic_record.setVisibility(View.INVISIBLE);
            autonomic_stop.setVisibility(View.INVISIBLE);
            leftMotorPower.setVisibility(View.INVISIBLE);
            rightMotorPower.setVisibility(View.INVISIBLE);
            turnLeftInterval.setVisibility(View.INVISIBLE);
            turnRightInterval.setVisibility(View.INVISIBLE);
            leftMotorPowerAtLeftTurn.setVisibility(View.INVISIBLE);
            rightMotorPowerAtLeftTurn.setVisibility(View.INVISIBLE);
            leftMotorPowerAtRightTurn.setVisibility(View.INVISIBLE);
            rightMotorPowerAtRightTurn.setVisibility(View.INVISIBLE);
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
        connectBtn.setBackgroundResource((wifiConnection) ? R.drawable.wifi_on : R.drawable.wifi_off);
    }
    public void updateWebSButton(){
        websocketBtn.setBackgroundResource((wsConnection) ? R.drawable.server_on : R.drawable.server_off);
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
