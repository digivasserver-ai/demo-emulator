package com.google.android.gms.common.internal;

import android.os.Parcel;
import android.os.Parcelable;

/** Placeholder parcelable referenced by IGmsServiceBroker.aidl (never invoked by the demo). */
public class ValidateAccountRequest implements Parcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public static final Creator<ValidateAccountRequest> CREATOR = new Creator<ValidateAccountRequest>() {
        @Override
        public ValidateAccountRequest createFromParcel(Parcel in) {
            return new ValidateAccountRequest();
        }

        @Override
        public ValidateAccountRequest[] newArray(int size) {
            return new ValidateAccountRequest[size];
        }
    };
}
