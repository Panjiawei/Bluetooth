package com.example.bluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public interface ConnectBlueCallBack {

    void onStartConnect();
    void onConnectSuccess(BluetoothDevice device, BluetoothSocket bluetoothSocket);
    void onConnectFail(BluetoothDevice device,String string);
}
