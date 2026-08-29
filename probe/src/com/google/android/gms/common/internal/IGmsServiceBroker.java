package com.google.android.gms.common.internal;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Hand-written AIDL stub/proxy pair for IGmsServiceBroker.
 * Only the methods the DroidGuard probe uses are implemented
 * (getService = 45, validateAccount = 46). Transaction codes match
 * com.google.android.gms.common.internal.IGmsServiceBroker.aidl.
 */
public interface IGmsServiceBroker extends IInterface {

    void getService(IGmsCallbacks callback, GetServiceRequest request) throws RemoteException;

    void validateAccount(IGmsCallbacks callback, ValidateAccountRequest request) throws RemoteException;

    abstract class Stub extends android.os.Binder implements IGmsServiceBroker {
        public static final String DESCRIPTOR = "com.google.android.gms.common.internal.IGmsServiceBroker";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGmsServiceBroker asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IGmsServiceBroker) {
                return (IGmsServiceBroker) iin;
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
                case 45: {
                    data.enforceInterface(DESCRIPTOR);
                    IGmsCallbacks cb = IGmsCallbacks.Stub.asInterface(data.readStrongBinder());
                    GetServiceRequest request = new GetServiceRequest(data);
                    getService(cb, request);
                    reply.writeNoException();
                    return true;
                }
                case 46: {
                    data.enforceInterface(DESCRIPTOR);
                    IGmsCallbacks cb = IGmsCallbacks.Stub.asInterface(data.readStrongBinder());
                    ValidateAccountRequest request = ValidateAccountRequest.CREATOR.createFromParcel(data);
                    validateAccount(cb, request);
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    class Proxy implements IGmsServiceBroker {
        private final IBinder mRemote;

        Proxy(IBinder remote) {
            mRemote = remote;
        }

        @Override
        public IBinder asBinder() {
            return mRemote;
        }

        @Override
        public void getService(IGmsCallbacks callback, GetServiceRequest request) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                request.writeToParcel(data, 0);
                mRemote.transact(45, data, reply, 0);
                reply.readException();
            } finally {
                reply.recycle();
                data.recycle();
            }
        }

        @Override
        public void validateAccount(IGmsCallbacks callback, ValidateAccountRequest request) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                request.writeToParcel(data, 0);
                mRemote.transact(46, data, reply, 0);
                reply.readException();
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
    }
}