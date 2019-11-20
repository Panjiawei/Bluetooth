package com.example.bluetoothtest;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.example.bluetoothtest.model.entity.DeviceInfo;
import com.example.bluetoothtest.model.entity.DiscoveredDevice;
import com.example.bluetoothtest.model.entity.HistoryData;
import com.example.bluetoothtest.model.entity.SessionData;
import com.example.bluetoothtest.model.entity.UserInfo;
import com.example.bluetoothtest.model.enumerate.ComType;
import com.example.bluetoothtest.model.enumerate.Protocol;
import com.example.bluetoothtest.model.enumerate.SettingKey;
import com.example.bluetoothtest.model.system.AppConfig;
import com.example.bluetoothtest.util.AppLog;
import com.example.bluetoothtest.util.BluetoothPowerController;
import com.example.bluetoothtest.util.Common;
import com.example.bluetoothtest.util.ScanController;
import com.example.bluetoothtest.util.SessionController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import jp.co.ohq.androidcorebluetooth.CBConfig;
import jp.co.ohq.androidcorebluetooth.CBPeripheral;
import jp.co.ohq.ble.OHQConfig;
import jp.co.ohq.ble.OHQDeviceManager;
import jp.co.ohq.ble.advertising.EachUserData;
import jp.co.ohq.ble.enumerate.OHQCompletionReason;
import jp.co.ohq.ble.enumerate.OHQConnectionState;
import jp.co.ohq.ble.enumerate.OHQDetailedState;
import jp.co.ohq.ble.enumerate.OHQDeviceCategory;
import jp.co.ohq.ble.enumerate.OHQSessionOptionKey;
import jp.co.ohq.ble.enumerate.OHQUserDataKey;
import jp.co.ohq.utility.Bundler;
import jp.co.ohq.utility.Handler;
import jp.co.ohq.utility.Types;


public class Main3Activity extends AppCompatActivity implements
        BluetoothPowerController.Listener,
        ScanController.Listener,
        View.OnClickListener,
        SessionController.Listener,
        OHQDeviceManager.DebugMonitor {

    private static final String ARG_ONLY_PAIRING_MODE = "ARG_ONLY_PAIRING_MODE";
    private static final String ARG_INFO_BUTTON_VISIBILITY = "ARG_INFO_BUTTON_VISIBILITY";
    private static final String ARG_INFORM_OF_REGISTERED_DEVICE = "ARG_INFORM_OF_REGISTERED_DEVICE";
    private static final int DIALOG_REQ_CODE_FILTER_LIST = 0;
    private BluetoothPowerController mBluetoothPowerController;
    private ScanController mScanController;
    private boolean mIsOnlyPairingMode;
    private OHQDeviceCategory mFilteringDeviceCategory;
    // private DiscoveredDeviceListAdapter mDiscoveredDeviceListAdapter;
    //  private EventListener mListener;
    private TextView mEmptyTextView;
    private Realm mRealm;
    private SessionController mSessionController;
    private static final long CONNECTION_WAIT_TIME = 60000;
    private static final int CONSENT_CODE_OHQ = 0x020E;
    private final String mCurrentUserName = AppConfig.sharedInstance().getNameOfCurrentUser();
    DiscoveredDevice discoveredDevice;

    String mAddress;

    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    private static final String TAG = "MainActivity3";
    private int REQUEST_CODE_OPEN_GPS = 3;



    @NonNull
    private Map<OHQSessionOptionKey, Object> mOption = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        checkPermissions();

        Button button = findViewById(R.id.btn_1);
        button.setOnClickListener(this);
        mRealm = Realm.getDefaultInstance();
        mBluetoothPowerController = new BluetoothPowerController(this);
        mScanController = new ScanController(this);
        mSessionController = new SessionController(this, this);


        mIsOnlyPairingMode = false;


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


    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }


    @Override
    public void onResume() {
        AppLog.vMethodIn();
        super.onResume();
        mBluetoothPowerController.onResume();
        mScanController.onResume();
        mSessionController.onResume();

        if (mBluetoothPowerController.state()) {
            mScanController.startScan();

        }
    }


    @Override
    public void onPause() {
        AppLog.vMethodIn();
        super.onPause();
        mBluetoothPowerController.onPause();
        mScanController.onPause();
        mSessionController.onPause();

    }


    @Override
    public void onScan(@NonNull List<DiscoveredDevice> discoveredDevices) {

        Log.e("pan", discoveredDevices.size() + "");

        if (mIsOnlyPairingMode) {
            for (DiscoveredDevice device : discoveredDevices) {
                Log.e("pan", device.getAddress() + "  " + device.getLocalName());

            }
        } else {

            for (DiscoveredDevice device : discoveredDevices) {
                discoveredDevice = device;
                Log.e("pan", device.getAddress() + "  " + device.getLocalName());
                DeviceInfo deviceInfo = mRealm.where(DeviceInfo.class).equalTo(
                        "users.name", AppConfig.sharedInstance().getNameOfCurrentUser()).equalTo("address", device.getAddress()).findFirst();
                if (null != deviceInfo) {
                    break;
                }
                //EC:21:E5:BC:8E:D1  BLEsmart_0001000CEC21E5BC8ED1
                if (device.getAddress().equals("EC:21:E5:BC:8E:D1")) {
                    mAddress=device.getAddress();
                    startSession();
                }
            }
        }
        mScanController.stopScan();
    }

    @NonNull
    public void newInstanceForRegister(
            @NonNull DiscoveredDevice discoverDevice,
            @NonNull Protocol protocol,
            @Nullable Map<OHQUserDataKey, Object> userData,
            @Nullable Integer userIndex) {
        final Map<OHQSessionOptionKey, Object> option = new HashMap<>();
        boolean specifiedUserControl = false;
        if (OHQDeviceCategory.WeightScale == discoverDevice.getDeviceCategory()) {
            specifiedUserControl = true;
        }
        if (OHQDeviceCategory.BodyCompositionMonitor == discoverDevice.getDeviceCategory()) {
            specifiedUserControl = true;
        }
        if (Protocol.OmronExtension == protocol) {
            specifiedUserControl = true;
        }
        if (specifiedUserControl) {
            option.put(OHQSessionOptionKey.RegisterNewUserKey, true);
            option.put(OHQSessionOptionKey.ConsentCodeKey, CONSENT_CODE_OHQ);
            if (null != userIndex) {
                option.put(OHQSessionOptionKey.UserIndexKey, userIndex);
            }
            if (null != userData) {
                option.put(OHQSessionOptionKey.UserDataKey, userData);
            }
            option.put(OHQSessionOptionKey.DatabaseChangeIncrementValueKey, (long) 0);
            option.put(OHQSessionOptionKey.UserDataUpdateFlagKey, true);
        }
        if (Protocol.OmronExtension == protocol) {
            option.put(OHQSessionOptionKey.AllowAccessToOmronExtendedMeasurementRecordsKey, true);
            option.put(OHQSessionOptionKey.AllowControlOfReadingPositionToMeasurementRecordsKey, true);
        }
        option.put(OHQSessionOptionKey.ReadMeasurementRecordsKey, true);

        final HistoryData historyData = new HistoryData();
        historyData.setComType(ComType.Register);
        historyData.setAddress(discoverDevice.getAddress());
        historyData.setLocalName(discoverDevice.getLocalName());
        historyData.setCompleteLocalName(discoverDevice.getCompleteLocalName());
        historyData.setDeviceCategory(discoverDevice.getDeviceCategory());
        historyData.setProtocol(protocol);

        mOption=option;
//        return newInstance(Mode.Normal, discoverDevice.getAddress(),
//                option, historyData);
    }


    private void startSession() {

        UserInfo userInfo = mRealm.where(UserInfo.class).equalTo(
                "name", mCurrentUserName).findFirst();
        Map<OHQUserDataKey, Object> userData = new HashMap<>();
        userData.put(OHQUserDataKey.DateOfBirthKey, userInfo.getDateOfBirth());
        userData.put(OHQUserDataKey.HeightKey, userInfo.getHeight());
        userData.put(OHQUserDataKey.GenderKey, userInfo.getGender());
        newInstanceForRegister(discoveredDevice, Protocol.OmronExtension, userData, 1);
        Log.e("pan", userInfo.getDateOfBirth());

//        if (mSessionController.isInSession()) {
//            AppLog.i("Already started session.");
//            return;
//        }


        onStarted();
    }

    private void onStarted() {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                Common.outputDeviceInfo(getApplicationContext());
                mSessionController.setConfig(getConfig(getApplicationContext()));
                mOption.put(OHQSessionOptionKey.ConnectionWaitTimeKey, CONNECTION_WAIT_TIME);
                Log.e("pan", JSON.toJSONString(mOption));

                mSessionController.startSession(mAddress, mOption);

                Log.e("pan", "onStarted++++++++++");

            }
        });
    }


    @NonNull
    private Bundle getConfig(@NonNull Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String s;
        CBConfig.CreateBondOption cOption;
        s = pref.getString(SettingKey.create_bond_option.name(), null);
        Log.e("pan", s);
        if (getString(R.string.create_bond_before_catt_connection).equals(s)) {
            cOption = CBConfig.CreateBondOption.UsedBeforeGattConnection;
        } else if (getString(R.string.create_bond_after_services_discovered).equals(s)) {
            cOption = CBConfig.CreateBondOption.UsedAfterServicesDiscovered;
        } else {
            cOption = CBConfig.CreateBondOption.NotUse;
        }
        CBConfig.RemoveBondOption rOption;
        s = pref.getString(SettingKey.remove_bond_option.name(), null);
        Log.e("pan", s);
        if (getString(R.string.remove_bond_use).equals(s)) {
            rOption = CBConfig.RemoveBondOption.UsedBeforeConnectionProcessEveryTime;
        } else {
            rOption = CBConfig.RemoveBondOption.NotUse;
        }


        return Bundler.bundle(
                OHQConfig.Key.CreateBondOption.name(), cOption,
                OHQConfig.Key.RemoveBondOption.name(), rOption,
                OHQConfig.Key.AssistPairingDialogEnabled.name(), pref.getBoolean(SettingKey.assist_pairing_dialog.name(), false),
                OHQConfig.Key.AutoPairingEnabled.name(), pref.getBoolean(SettingKey.auto_pairing.name(), false),
                OHQConfig.Key.AutoEnterThePinCodeEnabled.name(), pref.getBoolean(SettingKey.auto_enter_the_pin_code.name(), false),
                OHQConfig.Key.PinCode.name(), pref.getString(SettingKey.pin_code.name(), "123456"),
                OHQConfig.Key.StableConnectionEnabled.name(), pref.getBoolean(SettingKey.stable_connection.name(), false),
                OHQConfig.Key.StableConnectionWaitTime.name(), Long.valueOf(pref.getString(SettingKey.stable_connection_wait_time.name(), "123456")),
                OHQConfig.Key.ConnectionRetryEnabled.name(), pref.getBoolean(SettingKey.connection_retry.name(), false),
                OHQConfig.Key.ConnectionRetryDelayTime.name(), Long.valueOf(pref.getString(SettingKey.connection_retry_delay_time.name(), "123456")),
                OHQConfig.Key.ConnectionRetryCount.name(), Integer.valueOf(pref.getString(SettingKey.connection_retry_count.name(), "123456")),
                OHQConfig.Key.UseRefreshWhenDisconnect.name(), pref.getBoolean(SettingKey.refresh_use.name(), false)
        );


    }

    @Override
    public void onScanCompletion(@NonNull OHQCompletionReason reason) {
        Log.e("pan", "onScanCompletion++++++++++");

    }

    @Override
    public void onBluetoothStateChanged(boolean enable) {
        Log.e("pan", "onBluetoothStateChanged++++++++++");

        if (enable) {
            mScanController.startScan();
            startSession();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {


            case R.id.btn_1:

                mFilteringDeviceCategory = OHQDeviceCategory.BodyCompositionMonitor;
                mScanController.setFilteringDeviceCategory(mFilteringDeviceCategory);
                mScanController.startScan();
                break;


        }

    }

    @Override
    public void onConnectionStateChanged(@NonNull OHQConnectionState connectionState) {
        Log.e("pan", "onConnectionStateChanged:   " + connectionState.name());

    }

    @Override
    public void onSessionComplete(@NonNull SessionData sessionData) {
        Log.e("pan", "onConnectionStateChanged++++++++++");
        Log.e("pan", JSON.toJSONString(sessionData));

    }

    @Override
    public void onDetailedStateChanged(@NonNull OHQDetailedState ƒ) {
        Log.e("pan", "onConnectionStateChanged++++++++++");

    }

    @Override
    public void onPairingRequest() {
        Log.e("pan", "onPairingRequest++++++++++");

    }

    @Override
    public void onGattConnectionStateChanged(@NonNull CBPeripheral.GattConnectionState gattConnectionState) {
        Log.e("pan", "onGattConnectionStateChanged++++++++++    "+gattConnectionState.name());

    }

    @Override
    public void onAclConnectionStateChanged(@NonNull CBPeripheral.AclConnectionState aclConnectionState) {
        Log.e("pan", "onAclConnectionStateChanged++++++++++");

    }

    @Override
    public void onBondStateChanged(@NonNull CBPeripheral.BondState bondState) {
        Log.e("pan", "onBondStateChanged++++++++++");

    }


    private enum Mode {
        Normal, UnregisteredUser
    }

}
