package com.google.android.gms.droidguard.internal;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Hand-written AIDL stub/proxy pair for IDroidGuardService.
 * Transaction codes (byte-confirmed from the installed microG build's
 * client proxy IDroidGuardService$Stub$Proxy):
 * guard=1, getHandle=2, getClientTimeoutMillis=3, guardWithRequest=4.
 */
public interface IDroidGuardService extends IInterface {

    IDroidGuardHandle getHandle() throws RemoteException;

    abstract class Stub extends android.os.Binder implements IDroidGuardService {
        public static final String DESCRIPTOR = "com.google.android.gms.droidguard.internal.IDroidGuardService";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDroidGuardService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IDroidGuardService) {
                return (IDroidGuardService) iin;
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
                case 2: {
                    data.enforceInterface(DESCRIPTOR);
                    IDroidGuardHandle result = getHandle();
                    reply.writeNoException();
                    reply.writeStrongBinder(result != null ? result.asBinder() : null);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    class Proxy implements IDroidGuardService {
        private final IBinder mRemote;

        Proxy(IBinder remote) {
            mRemote = remote;
        }

        @Override
        public IBinder asBinder() {
            return mRemote;
        }

        @Override
        public IDroidGuardHandle getHandle() throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(Stub.DESCRIPTOR);
                mRemote.transact(2, data, reply, 0);
                reply.readException();
                return IDroidGuardHandle.Stub.asInterface(reply.readStrongBinder());
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
    }
}