package com.google.android.gms.droidguard.internal;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.Map;

/**
 * Hand-written AIDL stub/proxy pair for IDroidGuardHandle.
 * Transaction codes (byte-confirmed from the installed microG build's
 * client proxy IDroidGuardHandle$Stub$Proxy):
 * init=1, snapshot=2 (reply byte[]), close=3, initWithRequest=5 (reply
 * DroidGuardInitReply). The server fires init (1) and close (3) without a
 * reply, so the proxy sends them oneway. snapshot/initWithRequest are
 * two-way. Parcelable args are framed microG-style: writeInt(1) present flag
 * before writeToParcel; readTypedObject consumes it before CREATOR.
 */
public interface IDroidGuardHandle extends IInterface {

    void init(String flow) throws RemoteException;

    byte[] snapshot(Map map) throws RemoteException;

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
        @SuppressWarnings("unchecked")
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1: {
                    data.enforceInterface(DESCRIPTOR);
                    String flow = data.readString();
                    init(flow);
                    return true;
                }
                case 2: {
                    data.enforceInterface(DESCRIPTOR);
                    Map map = data.readHashMap(getClass().getClassLoader());
                    byte[] result = snapshot(map);
                    reply.writeNoException();
                    reply.writeByteArray(result);
                    return true;
                }
                case 3: {
                    data.enforceInterface(DESCRIPTOR);
                    close();
                    return true;
                }
                case 5: {
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
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                data.writeString(flow);
                mRemote.transact(1, data, null, android.os.IBinder.FLAG_ONEWAY);
            } finally {
                data.recycle();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public byte[] snapshot(Map map) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                data.writeMap(map);
                mRemote.transact(2, data, reply, 0);
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
                mRemote.transact(3, data, null, android.os.IBinder.FLAG_ONEWAY);
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
                data.writeInt(1);
                request.writeToParcel(data, 0);
                mRemote.transact(5, data, reply, 0);
                reply.readException();
                // readTypedObject semantics: present-flag 0 => null, 1 => object.
                // RemoteHandleImpl.initWithRequest returns null, so expect 0 here.
                return reply.readInt() == 0 ? null
                        : DroidGuardInitReply.CREATOR.createFromParcel(reply);
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
    }
}
