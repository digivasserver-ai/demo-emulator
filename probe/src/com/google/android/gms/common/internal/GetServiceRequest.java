package com.google.android.gms.common.internal;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Byte-compatible re-implementation of microG's GetServiceRequest parcel
 * format (safeparcel v6, fields 1-15). Only the fields the DroidGuard broker
 * needs are written; the reader tolerates absent optional fields.
 */
public class GetServiceRequest implements Parcelable {
    public int serviceId;
    public int gmsVersion;
    public String packageName;
    public Bundle extras;

    public GetServiceRequest(int serviceId) {
        this.serviceId = serviceId;
        this.gmsVersion = 0;
        this.extras = new Bundle();
    }

    /**
     * Tolerant parser used only on the (never-exercised) in-process stub path:
     * reads field 2 (serviceId) from the safeparcel stream if present.
     */
    public GetServiceRequest(Parcel in) {
        this(0);
        try {
            int start = in.dataPosition();
            int header = in.readInt();
            int field = header & 0xFFFF;
            int size = header >>> 16;
            if (field == 1) {
                in.readInt();
                header = in.readInt();
                field = header & 0xFFFF;
                size = header >>> 16;
            }
            if (field == 2 && size == 4) {
                this.serviceId = in.readInt();
            } else if (field == 2) {
                in.setDataPosition(in.dataPosition() + size);
            } else {
                in.setDataPosition(start);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // field 1: versionCode = 6
        writeHeader(dest, 1, 4);
        dest.writeInt(6);
        // field 2: serviceId
        writeHeader(dest, 2, 4);
        dest.writeInt(serviceId);
        // field 3: gmsVersion
        writeHeader(dest, 3, 4);
        dest.writeInt(gmsVersion);
        // field 4: packageName
        writeStringField(dest, 4, packageName);
        // field 7: extras
        writeBundleField(dest, 7, extras);
        // field 12: supportsConnectionInfo (0 => callback uses basic onPostInitComplete)
        writeHeader(dest, 12, 4);
        dest.writeInt(0);
    }

    private static void writeHeader(Parcel parcel, int fieldId, int size) {
        if (size >= 0xFFFF) {
            parcel.writeInt(0xFFFF0000 | fieldId);
            parcel.writeInt(size);
        } else {
            parcel.writeInt(size << 16 | fieldId);
        }
    }

    private static void writeStringField(Parcel parcel, int fieldId, String val) {
        writeHeader(parcel, fieldId, 0xFFFF);
        int start = parcel.dataPosition();
        parcel.writeString(val);
        int end = parcel.dataPosition();
        parcel.setDataPosition(start - 4);
        parcel.writeInt(end - start);
        parcel.setDataPosition(end);
    }

    private static void writeBundleField(Parcel parcel, int fieldId, Bundle val) {
        writeHeader(parcel, fieldId, 0xFFFF);
        int start = parcel.dataPosition();
        parcel.writeBundle(val);
        int end = parcel.dataPosition();
        parcel.setDataPosition(start - 4);
        parcel.writeInt(end - start);
        parcel.setDataPosition(end);
    }

    public static final Creator<GetServiceRequest> CREATOR = new Creator<GetServiceRequest>() {
        @Override
        public GetServiceRequest createFromParcel(Parcel in) {
            return new GetServiceRequest(25);
        }

        @Override
        public GetServiceRequest[] newArray(int size) {
            return new GetServiceRequest[size];
        }
    };
}