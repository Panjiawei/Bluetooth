package com.example.bluetoothtest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ScanBlueCallBack, View.OnClickListener, ConnectBlueCallBack {
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    private static final String TAG = "MainActivity ";
    BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_CODE_OPEN_GPS = 3;
    List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    ScanBlueReceiver scanBlueReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.tv_1);
        textView.setOnClickListener(this);
        TextView textView1 = findViewById(R.id.tv_2);
        textView1.setOnClickListener(this);
        TextView textView3 = findViewById(R.id.tv_3);
        textView3.setOnClickListener(this);
        TextView textView4 = findViewById(R.id.tv_4);
        textView4.setOnClickListener(this);

        checkPermissions();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        scanBlueReceiver = new ScanBlueReceiver(this);

        //搜索开始的过滤器
        IntentFilter filter1 = new IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        //搜索结束的过滤器
        IntentFilter filter2 = new IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //寻找到设备的过滤器
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //绑定状态改变
        IntentFilter filter4 = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //配对请求
        IntentFilter filter5 = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);


        registerReceiver(scanBlueReceiver, filter1);
        registerReceiver(scanBlueReceiver, filter2);
        registerReceiver(scanBlueReceiver, filter3);
        registerReceiver(scanBlueReceiver, filter4);
        registerReceiver(scanBlueReceiver, filter5);


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


                }
            });
        }

    };


    private boolean isScanning;//是否正在搜索
    private Handler mHandler;
    //15秒搜索时间
    private static final long SCAN_PERIOD = 15000;

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
            mBluetoothAdapter.startLeScan(mLeScanCallback); //开始搜索
        } else {//false
            isScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);//停止搜索
        }
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


    /**
     * 连接 （在配对之后调用）
     *
     * @param device
     */
    public void connect(BluetoothDevice device, ConnectBlueCallBack callBack) {
        //连接之前把扫描关闭
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        new ConnectBlueTask(callBack).execute(device);
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
     * 配对蓝牙设备
     */
    private void pinTargetDevice(int position) {
        //在配对之前，停止搜索
        mBluetoothAdapter.cancelDiscovery();
        //获取要匹配的BluetoothDevice对象，后边的deviceList是你本地存的所有对象
        BluetoothDevice device = deviceList.get(position);
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {//没配对才配对
            try {
                Log.d(TAG, "开始配对...");

                Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                Boolean returnValue = (Boolean) createBondMethod.invoke(device);

                if (returnValue) {
                    Log.d(TAG, "配对成功...");
                    showToast("配对成功");
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 取消配对（取消配对成功与失败通过广播返回 也就是配对失败）
     *
     * @param device
     */
    public void cancelPinBule(BluetoothDevice device) {
        Log.d(TAG, "attemp to cancel bond:" + device.getName());
        try {
            Method removeBondMethod = device.getClass().getMethod("removeBond");
            Boolean returnValue = (Boolean) removeBondMethod.invoke(device);
            returnValue.booleanValue();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG, "attemp to cancel bond fail!");
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
     * 蓝牙是否打开   true为打开
     *
     * @return
     */
    public boolean isBlueEnable() {
        return isSupportBlue() && mBluetoothAdapter.isEnabled();
    }

    /**
     * 设备是否支持蓝牙  true为支持
     *
     * @return
     */
    public boolean isSupportBlue() {
        return mBluetoothAdapter != null;
    }

    /**
     * 自动打开蓝牙（异步：蓝牙不会立刻就处于开启状态）
     * 这个方法打开蓝牙不会弹出提示
     */
    @SuppressLint("MissingPermission")
    public void openBlueAsyn() {
        if (isSupportBlue()) {
            mBluetoothAdapter.enable();
        }

    }

    /**
     * 自动打开蓝牙（同步）
     * 这个方法打开蓝牙会弹出提示
     * 需要在onActivityResult 方法中判断resultCode == RESULT_OK  true为成功
     */
    public void openBlueSync(Activity activity, int requestCode) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, requestCode);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(TAG, requestCode + "   " + resultCode);


    }

    /**
     * 扫描的方法 返回true 扫描成功
     * 通过接收广播获取扫描到的设备
     *
     * @return
     */
    public boolean scanBlue() {
        if (!isBlueEnable()) {
            Log.e(TAG, "Bluetooth not enable!");
            return false;
        }

        //当前是否在扫描，如果是就取消当前的扫描，重新扫描
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        //此方法是个异步操作，一般搜索12秒
        return mBluetoothAdapter.startDiscovery();
    }


    /**
     * 取消扫描蓝牙
     *
     * @return true 为取消成功
     */
    public boolean cancelScanBule() {
        if (isSupportBlue()) {
            return mBluetoothAdapter.cancelDiscovery();
        }
        return true;
    }


    @Override
    public void onScanStarted() {
        Log.e(TAG, "开始扫描");

    }

    @Override
    public void onScanFinished() {
        Log.e(TAG, "扫描结束");
    }

    @Override
    public void onScanning(BluetoothDevice e) {
        deviceList.add(e);

        Log.e(TAG, e.toString());

    }

    @Override
    public void onStateChanged(BluetoothDevice e) {
        Log.e(TAG, e.toString() + "    onStateChanged");
    }

    @Override
    public void onBondRequest() {

    }

    @Override
    public void onBondFail(BluetoothDevice bluetoothDevice) {

    }

    @Override
    public void onBonding(BluetoothDevice bluetoothDevice) {

    }

    @Override
    public void onBondSuccess(BluetoothDevice bluetoothDevice) {

        connect(bluetoothDevice, this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.tv_1:
                openBlueSync(this, 100);

                break;
            case R.id.tv_2:
                scanBlue();
                break;

            case R.id.tv_3:
                for (int i = 0; i < deviceList.size(); i++) {
                    Log.e(TAG, deviceList.get(i).toString() + "    ________________");
                    if (deviceList.get(i).toString().equals("B0:49:5F:03:0E:50")) {
                        Log.e(TAG, deviceList.get(i).toString() + "    2________________");

                        pinTargetDevice(i);
                    }
                }

                break;

            case R.id.tv_4:
                // cancelPinBule();
                startActivity(new Intent(this, Main2Activity.class));
                finish();
                break;
        }

    }


    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStartConnect() {
        Log.w(TAG, "开始连接");
    }

    @Override
    public void onConnectSuccess(BluetoothDevice device, BluetoothSocket bluetoothSocket) {
        Log.w(TAG, "连接成功" + bluetoothSocket.toString());

    }

    @Override
    public void onConnectFail(BluetoothDevice device, String string) {
        Log.w(TAG, "连接失败");
        connect(device, this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scanBlueReceiver);
    }
}


/**
 * 扫描广播接收类
 * Created by zqf on 2018/7/6.
 */
class ScanBlueReceiver extends BroadcastReceiver {
    private static final String TAG = ScanBlueReceiver.class.getName();
    private ScanBlueCallBack callBack;

    public ScanBlueReceiver(ScanBlueCallBack callBack) {
        this.callBack = callBack;
    }

    //广播接收器，当远程蓝牙设备被发现时，回调函数onReceiver()会被执行
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "action:" + action);
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        switch (action) {
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                Log.d(TAG, "开始扫描...");
                callBack.onScanStarted();
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Log.d(TAG, "结束扫描...");
                callBack.onScanFinished();
                break;
            case BluetoothDevice.ACTION_FOUND:
                Log.d(TAG, "发现设备...");
                callBack.onScanning(device);
                break;
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                Log.d(TAG, "设备绑定状态改变...");
                callBack.onStateChanged(device);

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
                break;

        }


    }
}
