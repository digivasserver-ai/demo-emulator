package com.google.android.gms.droidguard.internal;

import android.os.Parcel;
import android.os.Parcelable;

/** Placeholder parcelable returned by IDroidGuardHandle.initWithRequest (unused by the demo). */
public class DroidGuardInitReply implements Parcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public static final Creator<DroidGuardInitReply> CREATOR = new Creator<DroidGuardInitReply>() {
        @Override
        public DroidGuardInitReply createFromParcel(Parcel in) {
            return new DroidGuardInitReply();
        }

        @Override
        public DroidGuardInitReply[] newArray(int size) {
            return new DroidGuardInitReply[size];
        }
    };
}