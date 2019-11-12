package com.example.bluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

import static android.support.constraint.Constraints.TAG;

public class ConnectBlueTask extends AsyncTask<BluetoothDevice, Integer, BluetoothSocket> {

    private BluetoothDevice bluetoothDevice;
    private ConnectBlueCallBack callBack;

    public ConnectBlueTask(ConnectBlueCallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    protected BluetoothSocket doInBackground(BluetoothDevice... bluetoothDevices) {
        bluetoothDevice = bluetoothDevices[0];
        BluetoothSocket socket = null;
        try {
            Log.d(TAG, "开始连接socket,uuid:00001101-0000-1000-8000-00805F9B34FB");

            int sdk = Build.VERSION.SDK_INT;
            Log.d(TAG, sdk+"    ");

            if (sdk >= 10) {
                socket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001810-0000-1000-8000-00805f9b34fb"));
            } else {
                socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001810-0000-1000-8000-00805f9b34fb"));
            }

            //socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001810-0000-1000-8000-00805f9b34fb"));
            if (socket != null && !socket.isConnected()) {
                socket.connect();
            }
        } catch (IOException e) {
            Log.e(TAG, "socket连接失败");
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
                Log.e(TAG, "socket关闭失败");
            }
        }
        return socket;
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "开始连接");
        if (callBack != null) callBack.onStartConnect();
    }

    @Override
    protected void onPostExecute(BluetoothSocket bluetoothSocket) {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            Log.d(TAG, "连接成功");
            if (callBack != null) callBack.onConnectSuccess(bluetoothDevice, bluetoothSocket);
        } else {
            Log.d(TAG, "连接失败");
            if (callBack != null) callBack.onConnectFail(bluetoothDevice, "连接失败");
        }
    }

}
