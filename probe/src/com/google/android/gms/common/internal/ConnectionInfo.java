package com.google.android.gms.common.internal;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/** Placeholder parcelable referenced by IGmsCallbacks.aidl (not used by microG DroidGuard). */
public class ConnectionInfo implements Parcelable {
    public Bundle params;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(params);
    }

    public static final Creator<ConnectionInfo> CREATOR = new Creator<ConnectionInfo>() {
        @Override
        public ConnectionInfo createFromParcel(Parcel in) {
            ConnectionInfo info = new ConnectionInfo();
            info.params = in.readBundle();
            return info;
        }

        @Override
        public ConnectionInfo[] newArray(int size) {
            return new ConnectionInfo[size];
        }
    };
}
