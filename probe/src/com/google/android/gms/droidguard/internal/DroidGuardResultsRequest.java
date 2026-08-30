package com.google.android.gms.droidguard.internal;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Byte-compatible with microG's AutoSafeParcelable DroidGuardResultsRequest.
 *
 * microG declares this class with a single {@code @Field(2) Bundle bundle}.
 * SafeParcelWriter emits (SAFE_PARCEL_OBJECT_MAGIC == 0x4F45):
 *   - outer object header:  (0xFFFF0000 | 0x4F45) then total object length
 *   - field 2 header:       (0xFFFF0000 | 2) then bundle byte length
 *   - the Bundle payload
 *
 * There is NO field 1 (no versionCode int). Writing an extra field-1 header
 * first is what produced "Expected object header. Got 0x40001" on the server
 * reader, aborting initWithRequest and leaving flow/request null.
 */
public class DroidGuardResultsRequest implements Parcelable {
    private static final int SAFE_PARCEL_OBJECT_MAGIC = 0x4F45;

    public Bundle bundle = new Bundle();

    public DroidGuardResultsRequest() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Outer AutoSafeParcelable object header: (0xFFFF0000 | MAGIC) + placeholder size.
        int objStart = writeObjectHeader(dest, SAFE_PARCEL_OBJECT_MAGIC);

        // Single field: @Field(2) Bundle.
        int fieldStart = writeObjectHeader(dest, 2);
        dest.writeBundle(bundle);
        finishObjectHeader(dest, fieldStart);

        finishObjectHeader(dest, objStart);
    }

    /**
     * writeHeader(fieldId, 0xFFFF): since size >= 0xFFFF, writes (0xFFFF0000 | fieldId)
     * then a placeholder size int. Returns the position of that placeholder.
     */
    private static int writeObjectHeader(Parcel parcel, int fieldId) {
        parcel.writeInt(0xFFFF0000 | fieldId);
        parcel.writeInt(0xFFFF);
        return parcel.dataPosition();
    }

    /** Backpatch the byte-length into the placeholder written by writeObjectHeader. */
    private static void finishObjectHeader(Parcel parcel, int start) {
        int end = parcel.dataPosition();
        int length = end - start;
        parcel.setDataPosition(start - 4);
        parcel.writeInt(length);
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
