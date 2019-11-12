package com.example.bluetoothtest;

import android.bluetooth.BluetoothDevice;

public interface PinBlueCallBack {

    void onBondRequest();
    void onBondFail(BluetoothDevice bluetoothDevice);
    void onBonding(BluetoothDevice bluetoothDevice);
    void onBondSuccess(BluetoothDevice bluetoothDevice);



}
