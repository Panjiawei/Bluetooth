package com.example.bluetoothtest.model.enumerate;

import android.support.annotation.StringRes;

import com.example.bluetoothtest.R;


public enum ResultType {
    Success(R.string.success),
    Failure(R.string.failure);
    @StringRes
    int id;

    ResultType(@StringRes int id) {
        this.id = id;
    }

    @StringRes
    public int stringResId() {
        return this.id;
    }
}
