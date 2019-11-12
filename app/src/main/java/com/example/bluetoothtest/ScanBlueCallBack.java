package com.example.bluetoothtest;

import android.bluetooth.BluetoothDevice;

public interface ScanBlueCallBack {

    void onScanStarted();

    void onScanFinished();

    void onScanning(BluetoothDevice e);

    void onStateChanged(BluetoothDevice e);



    void onBondRequest();
    void onBondFail(BluetoothDevice bluetoothDevice);
    void onBonding(BluetoothDevice bluetoothDevice);
    void onBondSuccess(BluetoothDevice bluetoothDevice);

}
