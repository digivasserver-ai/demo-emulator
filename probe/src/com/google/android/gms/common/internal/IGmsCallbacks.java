package com.google.android.gms.common.internal;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Hand-written AIDL stub for IGmsCallbacks.
 * Transaction codes: onPostInitComplete=0, onAccountValidationComplete=1,
 * onPostInitCompleteWithConnectionInfo=2 (declaration order).
 */
public interface IGmsCallbacks extends IInterface {

    void onPostInitComplete(int statusCode, IBinder binder, Bundle params) throws RemoteException;

    void onAccountValidationComplete(int statusCode, Bundle params) throws RemoteException;

    void onPostInitCompleteWithConnectionInfo(int statusCode, IBinder binder, ConnectionInfo info) throws RemoteException;

    abstract class Stub extends android.os.Binder implements IGmsCallbacks {
        public static final String DESCRIPTOR = "com.google.android.gms.common.internal.IGmsCallbacks";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGmsCallbacks asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IGmsCallbacks) {
                return (IGmsCallbacks) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 0: {
                    data.enforceInterface(DESCRIPTOR);
                    int statusCode = data.readInt();
                    IBinder binder = data.readStrongBinder();
                    Bundle params = data.readBundle(getClass().getClassLoader());
                    onPostInitComplete(statusCode, binder, params);
                    reply.writeNoException();
                    return true;
                }
                case 1: {
                    data.enforceInterface(DESCRIPTOR);
                    int statusCode = data.readInt();
                    Bundle params = data.readBundle(getClass().getClassLoader());
                    onAccountValidationComplete(statusCode, params);
                    reply.writeNoException();
                    return true;
                }
                case 2: {
                    data.enforceInterface(DESCRIPTOR);
                    int statusCode = data.readInt();
                    IBinder binder = data.readStrongBinder();
                    ConnectionInfo info;
                    try {
                        info = ConnectionInfo.CREATOR.createFromParcel(data);
                    } catch (Exception e) {
                        info = null;
                    }
                    onPostInitCompleteWithConnectionInfo(statusCode, binder, info);
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    class Proxy implements IGmsCallbacks {
        private final IBinder mRemote;

        Proxy(IBinder remote) {
            mRemote = remote;
        }

        @Override
        public IBinder asBinder() {
            return mRemote;
        }

        @Override
        public void onPostInitComplete(int statusCode, IBinder binder, Bundle params) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                data.writeInt(statusCode);
                data.writeStrongBinder(binder);
                data.writeBundle(params);
                mRemote.transact(0, data, reply, 0);
                reply.readException();
            } finally {
                reply.recycle();
                data.recycle();
            }
        }

        @Override
        public void onAccountValidationComplete(int statusCode, Bundle params) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                data.writeInt(statusCode);
                data.writeBundle(params);
                mRemote.transact(1, data, reply, 0);
                reply.readException();
            } finally {
                reply.recycle();
                data.recycle();
            }
        }

        @Override
        public void onPostInitCompleteWithConnectionInfo(int statusCode, IBinder binder, ConnectionInfo info) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                data.writeInt(statusCode);
                data.writeStrongBinder(binder);
                if (info != null) {
                    info.writeToParcel(data, 0);
                } else {
                    data.writeInt(0);
                }
                mRemote.transact(2, data, reply, 0);
                reply.readException();
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
    }
}