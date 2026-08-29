package com.google.android.gms.droidguard.internal;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Hand-written AIDL stub/proxy pair for IDroidGuardHandle.
 * Transaction codes: init=0 (oneway), snapshot=1, close=2 (oneway),
 * initWithRequest=4 (declaration order).
 */
public interface IDroidGuardHandle extends IInterface {

    void init(String flow) throws RemoteException;

    byte[] snapshot(Bundle map) throws RemoteException;

    void close() throws RemoteException;

    DroidGuardInitReply initWithRequest(String flow, DroidGuardResultsRequest request) throws RemoteException;

    abstract class Stub extends android.os.Binder implements IDroidGuardHandle {
        public static final String DESCRIPTOR = "com.google.android.gms.droidguard.internal.IDroidGuardHandle";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDroidGuardHandle asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IDroidGuardHandle) {
                return (IDroidGuardHandle) iin;
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
                    String flow = data.readString();
                    init(flow);
                    reply.writeNoException();
                    return true;
                }
                case 1: {
                    data.enforceInterface(DESCRIPTOR);
                    Bundle map = data.readBundle(getClass().getClassLoader());
                    byte[] result = snapshot(map);
                    reply.writeNoException();
                    reply.writeByteArray(result);
                    return true;
                }
                case 2: {
                    data.enforceInterface(DESCRIPTOR);
                    close();
                    reply.writeNoException();
                    return true;
                }
                case 4: {
                    data.enforceInterface(DESCRIPTOR);
                    String flow = data.readString();
                    DroidGuardResultsRequest request = DroidGuardResultsRequest.CREATOR.createFromParcel(data);
                    DroidGuardInitReply result = initWithRequest(flow, request);
                    reply.writeNoException();
                    if (result != null) {
                        result.writeToParcel(reply, 0);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    class Proxy implements IDroidGuardHandle {
        private final IBinder mRemote;

        Proxy(IBinder remote) {
            mRemote = remote;
        }

        @Override
        public IBinder asBinder() {
            return mRemote;
        }

        @Override
        public void init(String flow) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                data.writeString(flow);
                mRemote.transact(0, data, reply, 0);
                reply.readException();
            } finally {
                reply.recycle();
                data.recycle();
            }
        }

        @Override
        public byte[] snapshot(Bundle map) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                data.writeBundle(map);
                mRemote.transact(1, data, reply, 0);
                reply.readException();
                return reply.createByteArray();
            } finally {
                reply.recycle();
                data.recycle();
            }
        }

        @Override
        public void close() throws RemoteException {
            Parcel data = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                mRemote.transact(2, data, null, android.os.IBinder.FLAG_ONEWAY);
            } finally {
                data.recycle();
            }
        }

        @Override
        public DroidGuardInitReply initWithRequest(String flow, DroidGuardResultsRequest request) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                data.writeString(flow);
                request.writeToParcel(data, 0);
                mRemote.transact(4, data, reply, 0);
                reply.readException();
                return DroidGuardInitReply.CREATOR.createFromParcel(reply);
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
    }
}