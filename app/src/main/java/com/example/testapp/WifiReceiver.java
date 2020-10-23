package com.example.testapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

public class WifiReceiver extends BroadcastReceiver {
    public String ROBOT_WIFI_NAME = "RPI_motor";
    MainActivity main = null;
    void setMainActivityHandler(MainActivity main){
        this.main = main;
    }
    @Override
    public void onReceive(Context context, Intent intent) {

        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if(info != null && info.isConnected()) {
            WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            if(ssid.equals("\""+ROBOT_WIFI_NAME+"\"")){
                main.wifiConnection = true;
                main.updateWifiButton();
               // MainActivity.confirmConnection();
                //main.connectWebSocket();
            }else{
                main.wifiConnection = false;
                main.updateWifiButton();
            }
        }
    }
}
