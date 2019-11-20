package com.example.bluetoothtest;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class Main2Activity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_LOCATION = 4;
    BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    private static final String TAG = "MainActivity2";
    private int REQUEST_CODE_OPEN_GPS = 3;

    private boolean isScanning;//是否正在搜索
    private Handler mHandler;
    //15秒搜索时间
    private static final long SCAN_PERIOD = 15000;
    BluetoothGatt mBluetoothGatt;

    private static final UUID UUID_SERVICE = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_NOTIFY = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_NOTIFYS = UUID.fromString("00002A35-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_NOTIFYS1 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID UUID_NOTIFYS2= UUID.fromString("00002a9c-0000-1000-8000-00805f9b34fb");
    //写通道uuid
    private static final UUID writeCharactUuid = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");
    //通知通道 uuid
    private static final UUID notifyCharactUuid = UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb");

    TextView textView4;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        TextView textView = findViewById(R.id.tv_1);
        textView.setOnClickListener(this);
        TextView textView1 = findViewById(R.id.tv_2);
        textView1.setOnClickListener(this);
        TextView textView3 = findViewById(R.id.tv_3);
        textView3.setOnClickListener(this);
        textView4 = findViewById(R.id.tv_4);
        textView4.setOnClickListener(this);


        checkPermissions();
        /*Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, REQUEST_LOCATION);*/
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mHandler = new Handler();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("pan", requestCode + "  " + resultCode + "  ");


    }


    /**
     * 检查权限
     */
    private void checkPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }


    /**
     * 开启GPS
     *
     * @param permission
     */
    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle("提示")
                            .setMessage("当前手机扫描蓝牙需要打开定位功能。")
                            .setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton("前往设置",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    //GPS已经开启了

                }
                break;
        }
    }

    /**
     * 检查GPS是否打开
     *
     * @return
     */
    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);

    }


    /**
     * 权限回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {//true
            //15秒后停止搜索
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            isScanning = true;
           // mBluetoothAdapter.startLeScan(new UUID[]{UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")}, mLeScanCallback); //开始搜索

            mBluetoothAdapter.startLeScan(mLeScanCallback); //开始搜索
        } else {//false
            isScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);//停止搜索
        }
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            //这里是个子线程，下面把它转换成主线程处理
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //在这里可以把搜索到的设备保存起来
                    //device.getName();获取蓝牙设备名字
                    //device.getAddress();获取蓝牙设备mac地址
                    //这里的rssi即信号强度，即手机与设备之间的信号强度。
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
                    registerReceiver(mBondingBroadcastReceiver, filter);
                  //  device.createBond();

                    //Log.e(TAG,device.getName()+"       "+device.getAddress());
                    if(device.getName()!=null&&device.getName().equals("BLEsmart_0001030EEC21E56BFAC5")) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);//停止搜索
                        mBluetoothGatt = device.connectGatt(Main2Activity.this, false, mBluetoothGattCallback);
                    }

                }
            });
        }

    };


    /**
     * 蓝牙绑定监听
     */
    private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);

            final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
            Log.d(TAG, device.getName() + "Bond state changed for: "
                    + device.getAddress() + " new state: " + bondState
                    + " previous: " + previousBondState);
            if (bondState == BluetoothDevice.BOND_BONDING) {
                Log.i(TAG, "BluetoothDevice.BOND_BONDING");
            } else if (bondState == BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "BluetoothDevice.BOND_BONDED");
                // mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, gattCallback);
                // 这个方法需要三个参数：一个Context对象，自动连接（boolean值,表示只要BLE设备可用是否自动连接它），和BluetoothGattCallback调用。
                //mBluetoothGatt = device.connectGatt(Main2Activity.this, false, mBluetoothGattCallback);
                //unregisterReceiver(mBondingBroadcastReceiver);
            } else if (bondState == BluetoothDevice.BOND_NONE) {
                Log.i(TAG, "BluetoothDevice.BOND_NONE");
            }
        }
    };


    BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            Log.d(TAG, "onPhyUpdate...");

        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
            Log.d(TAG, "onPhyRead...");

        }

        //当连接状态发生改变
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "当连接状态发生改变...");
            Log.e(TAG, "zhangwenbing onConnectionStateChange status:" + status + ", new State:" + newState + ", deviceState:" + gatt);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("onConnectionStateChange", "连接成功");
                //搜索Service
                gatt.discoverServices();

            } else {
                Log.d("onConnectionStateChange", "连接断开");
            }

        }

        //发现新服务，即调用了mBluetoothGatt.discoverServices()后，返回的数据
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //根据UUID获取Service中的Characteristic,并传入Gatt中
            List<BluetoothGattService> servicesList;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //发现设备，遍历服务，初始化特征
                initBLE(gatt);

                //获取服务列表
                servicesList = gatt.getServices();
                Log.d(TAG, "发现新服务，即调用了mBluetoothGatt.discoverServices()后，返回的数据..." + servicesList.size());

            } else {
                Log.d("TAG", "onServicesDiscovered fail-->" + status);
            }

            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "发现新服务，即调用了mBluetoothGatt.discoverServices()后，返回的数据...");

        }

        //调用mBluetoothGatt.readCharacteristic(characteristic)读取数据回调，在这里面接收数据
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);


            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 收到的数据
                byte[] receiveByte = characteristic.getValue();
                Log.d(TAG, "调用mBluetoothGatt.readCharacteristic(characteristic)读取数据回调，在这里面接收数据..."+ receiveByte.toString());

            } else {
                Log.d("TAG", "onCharacteristicRead fail-->" + status);
            }

        }

        //发送数据后的回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "发送数据后的回调...");


            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 发送成功

            } else {
                // 发送失败
            }


        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "调用onCharacteristicChanged读取数据回调，在这里面接收数据...");
            if ("00002A35-0000-1000-8000-00805f9b34fb".toLowerCase().equals(characteristic.getUuid().toString().toLowerCase())) {

                byte[] characteristicValueBytes = characteristic.getValue();
                //如果这个特征返回的是一串字符串，那么可以直接获得其值
                String bytesToString = new String(characteristicValueBytes);


                for (int i = 0; i <characteristicValueBytes.length ; i++) {

                    Log.e(TAG,characteristicValueBytes[i]+"");
                }


                Log.e(TAG,Arrays.toString(characteristicValueBytes));
                Log.e(TAG,bytesToString);
                deal9200TData(characteristic);
            }


        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {//descriptor读
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead...");

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {//descriptor写
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite...");

        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d(TAG, "onReliableWriteCompleted...");

        }

        //调用mBluetoothGatt.readRemoteRssi()时的回调，rssi即信号强度
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {//读Rssi
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.d(TAG, "调用mBluetoothGatt.readRemoteRssi()时的回调，rssi即信号强度...");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d(TAG, "onMtuChanged...");

        }
    };


    //初始化特征
    public void initBLE(BluetoothGatt gatt) {
        if (gatt == null) {
            return;
        }
        //遍历所有服务
        for (BluetoothGattService bluetoothGattService : gatt.getServices()) {
            Log.e(TAG, "--->BluetoothGattService" + bluetoothGattService.getUuid().toString());
           // if (bluetoothGattService.getUuid().toString().toLowerCase().equals(UUID_SERVICE.toString().toLowerCase())) {
                //遍历所有特征
                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                    Log.e(TAG,"---->gattCharacteristic"+ bluetoothGattCharacteristic.getUuid().toString());

                    String str = bluetoothGattCharacteristic.getUuid().toString();
                /*if (str.equals(writeCharactUuid)) {
                    //根据写UUID找到写特征
                    BluetoothGattCharacteristic mBluetoothGattCharacteristic = bluetoothGattCharacteristic;
                } else*/
                   // if (UUID_NOTIFYS.toString().equals(bluetoothGattCharacteristic.getUuid().toString())) {
                     //   Log.e(TAG,bluetoothGattCharacteristic.getUuid().toString());

                  //  gatt.readCharacteristic(bluetoothGattCharacteristic);
                   // gatt.writeCharacteristic(bluetoothGattCharacteristic);



                    //根据通知UUID找到通知特征
                        BluetoothGattCharacteristic mBluetoothGattCharacteristicNotify = bluetoothGattCharacteristic;
                        //设置true为启用通知,false反之
                        gatt.setCharacteristicNotification(mBluetoothGattCharacteristicNotify, true);
                        if(mBluetoothGattCharacteristicNotify.getDescriptor(UUID_NOTIFYS1)!=null) {
                            BluetoothGattDescriptor descriptor = mBluetoothGattCharacteristicNotify.getDescriptor(UUID_NOTIFYS1);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                            Log.e(TAG, bluetoothGattCharacteristic.getUuid().toString());
                        }

                    }
                //}

           // }


        }
    }


    private void deal9200TData(BluetoothGattCharacteristic characteristic) {

        Log.i(TAG, "onCharacteristicChanged BP_MEASUREMENT_CHARACTERISTIC");
        int offset = 0;
        final int flags = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        offset += 1;
        boolean bloodPressureUnitsFlag = (flags & 0x01) > 0;
        boolean timeStampFlag = (flags & 0x02) > 0;
        boolean pulseRateFlag = (flags & 0x04) > 0;
        boolean userIdFlag = (flags & 0x08) > 0;
        boolean measurementStatusFlag = (flags & 0x10) > 0;
        Log.i(TAG, "bloodPressureUnitsFlag:" + bloodPressureUnitsFlag
                + ",timeStampFlag:" + timeStampFlag + ",pulseRateFlag:"
                + pulseRateFlag + ",userIdFlag:" + userIdFlag
                + ",measurementStatusFlag:" + measurementStatusFlag);
        int systolic;
        int diastolic;
        int meanArterialPressure;
        systolic = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_SINT16, offset);
        offset += 2;
        diastolic = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_SINT16, offset);
        offset += 2;
        meanArterialPressure = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_SINT16, offset);
        offset += 2;
        Log.i(TAG, "systolic:" + systolic + ",diastolic:" + diastolic
                + ",meanArterialPressure:" + meanArterialPressure);
        if (timeStampFlag) {
            int year = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT16, offset + 0);
            int month = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2);
            int day = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, offset + 3);
            int hours = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, offset + 4);
            int minutes = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, offset + 5);
            int seconds = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, offset + 6);
            offset += 7;
            final Calendar calendar = Calendar.getInstance();
            /*
             * modified by JunQing Tang 201/01/06 the format of month in
             * bluetooth is 1 - 12, but the month in calendar is 0 - 11.
             * month minus 1, and save to calendar is right.
             */
            Log.i(TAG, "year:" + year + ",month:" + month + ",day:" + day + ",hours:" +
                    hours + "minutes:" + minutes + ",seconds:" + seconds);
            //do for time not sync success

            Log.i(TAG, "calendar:" + calendar);
            Log.i(TAG, "calendar.getTimeInMillis():" + calendar.getTimeInMillis());
        }
        if (pulseRateFlag) {
            int pulseRate = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT16, offset);

            offset += 2;
            Log.i(TAG, "pulseRate:" + pulseRate);
        }
        if (userIdFlag) {
            int userId = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, offset);
            offset += 1;
            Log.i(TAG, "userId:" + userId);
        }

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.tv_1:
                scanLeDevice(true);

                break;
            case R.id.tv_2:


                break;

            case R.id.tv_3:
                mBluetoothGatt.disconnect(); //主动断开连接
                break;

            case R.id.tv_4:
                // cancelPinBule();

                break;
        }
    }
}
