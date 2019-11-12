package com.example.bluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.Method;

public class PinBlueReceiver extends BroadcastReceiver {

    private String pin = "0000";  //此处为你要连接的蓝牙设备的初始密钥，一般为1234或0000
    private static final String TAG = PinBlueReceiver.class.getName();
    private PinBlueCallBack callBack;

    public PinBlueReceiver(PinBlueCallBack callBack){
        this.callBack = callBack;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "action:" + action);
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)){
            try {
                callBack.onBondRequest();
                //1.确认配对
//                ClsUtils.setPairingConfirmation(device.getClass(), device, true);
                Method setPairingConfirmation = device.getClass().getDeclaredMethod("setPairingConfirmation",boolean.class);
                setPairingConfirmation.invoke(device,true);
                //2.终止有序广播
                Log.d("order...", "isOrderedBroadcast:"+isOrderedBroadcast()+",isInitialStickyBroadcast:"+isInitialStickyBroadcast());
                abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                //3.调用setPin方法进行配对...
//                boolean ret = ClsUtils.setPin(device.getClass(), device, pin);
                Method removeBondMethod = device.getClass().getDeclaredMethod("setPin", new Class[]{byte[].class});
                Boolean returnValue = (Boolean) removeBondMethod.invoke(device, new Object[]{pin.getBytes()});
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
            switch (device.getBondState()) {
                case BluetoothDevice.BOND_NONE:
                    Log.d(TAG, "取消配对");
                    callBack.onBondFail(device);
                    break;
                case BluetoothDevice.BOND_BONDING:
                    Log.d(TAG, "配对中");
                    callBack.onBonding(device);
                    break;
                case BluetoothDevice.BOND_BONDED:
                    Log.d(TAG, "配对成功");
                    callBack.onBondSuccess(device);
                    break;
            }
        }

    }
}
