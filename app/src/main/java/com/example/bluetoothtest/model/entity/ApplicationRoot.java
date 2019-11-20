package com.example.bluetoothtest.model.entity;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;


import com.example.bluetoothtest.R;
import com.example.bluetoothtest.model.service.LogViewService;
import com.example.bluetoothtest.model.system.AppConfig;
import com.example.bluetoothtest.model.system.ExportResultFileManager;
import com.example.bluetoothtest.model.system.HistoryManager;
import com.example.bluetoothtest.util.AppLog;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import io.realm.Realm;
import jp.co.ohq.ble.OHQDeviceManager;
import jp.co.ohq.ble.enumerate.OHQGender;


public class ApplicationRoot extends android.app.Application {

    @Override
    public void onCreate() {
        AppLog.vMethodIn();
        super.onCreate();

        Realm.init(getApplicationContext());

        AppConfig.init(getApplicationContext());

        HistoryManager.init(getApplicationContext());

        ExportResultFileManager.init(getApplicationContext());

        OHQDeviceManager.init(getApplicationContext());

        PreferenceManager.setDefaultValues(getApplicationContext(),
                R.xml.connectivity_settings, true);

        Realm realm = Realm.getDefaultInstance();
        if (0 == realm.where(UserInfo.class).findAll().size()) {
            registerPresetUsers(realm);
        }
        AppConfig.sharedInstance().setNameOfCurrentUser(
                realm.where(UserInfo.class).findFirst().getName());
        realm.close();
        startService(new Intent(this, LogViewService.class));
    }

    @Override
    public void onTerminate() {
        AppLog.vMethodIn();
        super.onTerminate();
        stopService(new Intent(this, LogViewService.class));
    }

    @NonNull
    private List<UserInfo> registerPresetUsers(Realm realm) {
        UserInfo userInfo;
        final List<UserInfo> userInfoList = new LinkedList<>();

        userInfo = new UserInfo();
        userInfo.setName("User A");
        userInfo.setDateOfBirth("2000-04-01");
        userInfo.setHeight(new BigDecimal("180.1"));
        userInfo.setGender(OHQGender.Male);
        userInfoList.add(userInfo);

        userInfo = new UserInfo();
        userInfo.setName("User B");
        userInfo.setDateOfBirth("2001-05-01");
        userInfo.setHeight(new BigDecimal("175.2"));
        userInfo.setGender(OHQGender.Male);
        userInfoList.add(userInfo);

        userInfo = new UserInfo();
        userInfo.setName("User C");
        userInfo.setDateOfBirth("2002-06-01");
        userInfo.setHeight(new BigDecimal("170.3"));
        userInfo.setGender(OHQGender.Female);
        userInfoList.add(userInfo);

        userInfo = new UserInfo();
        userInfo.setName("User D");
        userInfo.setDateOfBirth("2003-07-01");
        userInfo.setHeight(new BigDecimal("165.4"));
        userInfo.setGender(OHQGender.Female);
        userInfoList.add(userInfo);

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (UserInfo userInfo : userInfoList) {
                    realm.copyToRealm(userInfo);
                }
            }
        });

        return userInfoList;
    }
}
