package com.google.android.gms.droidguard.internal;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Byte-compatible with microG's AutoSafeParcelable DroidGuardResultsRequest:
 * field 1 = versionCode (int), field 2 = Bundle.
 */
public class DroidGuardResultsRequest implements Parcelable {
    public Bundle bundle = new Bundle();

    public DroidGuardResultsRequest() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        writeHeader(dest, 1, 4);
        dest.writeInt(1);
        writeObjectField(dest, 2, bundle);
    }

    private static void writeHeader(Parcel parcel, int fieldId, int size) {
        if (size >= 0xFFFF) {
            parcel.writeInt(0xFFFF0000 | fieldId);
            parcel.writeInt(size);
        } else {
            parcel.writeInt(size << 16 | fieldId);
        }
    }

    private static void writeObjectField(Parcel parcel, int fieldId, Bundle val) {
        writeHeader(parcel, fieldId, 0xFFFF);
        int start = parcel.dataPosition();
        parcel.writeBundle(val);
        int end = parcel.dataPosition();
        parcel.setDataPosition(start - 4);
        parcel.writeInt(end - start);
        parcel.setDataPosition(end);
    }

    public static final Creator<DroidGuardResultsRequest> CREATOR = new Creator<DroidGuardResultsRequest>() {
        @Override
        public DroidGuardResultsRequest createFromParcel(Parcel in) {
            return new DroidGuardResultsRequest();
        }

        @Override
        public DroidGuardResultsRequest[] newArray(int size) {
            return new DroidGuardResultsRequest[size];
        }
    };
}