/*
 * Copyright (C) 2013 Daniel Himmelein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mindroid.content.pm;

import java.util.List;
import mindroid.content.Intent;
import mindroid.os.Binder;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.IInterface;
import mindroid.os.RemoteException;

public interface IPackageManager extends IInterface {
    public static abstract class Stub extends Binder implements IPackageManager {
        private static final java.lang.String DESCRIPTOR = "mindroid.content.pm.IPackageManager";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IPackageManager asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IPackageManager.Stub.SmartProxy(binder);
        }

        public IBinder asBinder() {
            return this;
        }

        protected Object onTransact(int what, int arg1, int arg2, Object obj, Bundle data) throws RemoteException {
            switch (what) {
            case MSG_GET_INSTALLED_PACKAGES: {
                List packages = getInstalledPackages(arg1);
                return packages;
            }
            case MSG_QUERY_INTENT_SERVICES: {
                Intent intent = (Intent) obj;
                List services = queryIntentServices(intent, arg1);
                return services;
            }
            case MSG_RESOLVE_SERVICE: {
                Intent intent = (Intent) obj;
                ResolveInfo resolveInfo = resolveService(intent, arg1);
                return resolveInfo;
            }
            case MSG_CHECK_PERMISSION: {
                int permission = checkPermission((String) obj, arg1);
                return new Integer(permission);
            }
            case MSG_GET_PERMISSIONS: {
                String[] permissions = getPermissions(arg1);
                return permissions;
            }
            case MSG_ADD_LISTENER: {
                addListener(IPackageManagerListener.Stub.asInterface((IBinder) obj));
                return null;
            }
            case MSG_REMOVE_LISTENER: {
                removeListener(IPackageManagerListener.Stub.asInterface((IBinder) obj));
                return null;
            }
            default:
                return super.onTransact(what, arg1, arg2, obj, data);
            }
        }

        private static class Proxy implements IPackageManager {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            public IBinder asBinder() {
                return mRemote;
            }

            public boolean equals(final Object obj) {
                if (obj == null) return false;
                if (obj == this) return true;
                if (obj instanceof Proxy) {
                    final Proxy that = (Proxy) obj;
                    return this.mRemote.equals(that.mRemote);
                }
                return false;
            }

            public int hashCode() {
                return mRemote.hashCode();
            }

            public List getInstalledPackages(int flags) throws RemoteException {
                List packages = (List) mRemote.transact(MSG_GET_INSTALLED_PACKAGES, flags, 0, 0);
                return packages;
            }

            public List queryIntentServices(Intent intent, int flags) throws RemoteException {
                List services = (List) mRemote.transact(MSG_QUERY_INTENT_SERVICES, flags, 0, intent, 0);
                return services;
            }

            public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException {
                ResolveInfo resolveInfo = (ResolveInfo) mRemote.transact(MSG_RESOLVE_SERVICE, flags, 0, intent, 0);
                return resolveInfo;
            }

            public int checkPermission(String permissionName, int pid) throws RemoteException {
                Integer permission = (Integer) mRemote.transact(MSG_CHECK_PERMISSION, pid, 0, permissionName, 0);
                return permission.intValue();
            }

            public String[] getPermissions(int pid) throws RemoteException {
                String[] permissions = (String[]) mRemote.transact(MSG_GET_PERMISSIONS, pid, 0, 0);
                return permissions;
            }

            public void addListener(IPackageManagerListener listener) throws RemoteException {
                mRemote.transact(MSG_ADD_LISTENER, listener.asBinder(), FLAG_ONEWAY);
            }

            public void removeListener(IPackageManagerListener listener) throws RemoteException {
                mRemote.transact(MSG_REMOVE_LISTENER, listener.asBinder(), FLAG_ONEWAY);
            }
        }

        private static class SmartProxy implements IPackageManager {
            private final IBinder mRemote;
            private final IPackageManager mStub;
            private final IPackageManager mProxy;

            SmartProxy(IBinder remote) {
                mRemote = remote;
                mStub = (IPackageManager) remote.queryLocalInterface(DESCRIPTOR);
                mProxy = new IPackageManager.Stub.Proxy(remote);
            }

            public IBinder asBinder() {
                return mRemote;
            }

            public boolean equals(final Object obj) {
                if (obj == null) return false;
                if (obj == this) return true;
                if (obj instanceof SmartProxy) {
                    final SmartProxy that = (SmartProxy) obj;
                    return this.mRemote.equals(that.mRemote);
                }
                return false;
            }

            public int hashCode() {
                return mRemote.hashCode();
            }

            public List getInstalledPackages(int flags) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.getInstalledPackages(flags);
                } else {
                    return mProxy.getInstalledPackages(flags);
                }
            }

            public List queryIntentServices(Intent intent, int flags) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.queryIntentServices(intent, flags);
                } else {
                    return mProxy.queryIntentServices(intent, flags);
                }
            }

            public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.resolveService(intent, flags);
                } else {
                    return mProxy.resolveService(intent, flags);
                }
            }

            public int checkPermission(String permissionName, int pid) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.checkPermission(permissionName, pid);
                } else {
                    return mProxy.checkPermission(permissionName, pid);
                }
            }

            public String[] getPermissions(int pid) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.getPermissions(pid);
                } else {
                    return mProxy.getPermissions(pid);
                }
            }

            public void addListener(IPackageManagerListener listener) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.addListener(IPackageManagerListener.Stub.asInterface(listener.asBinder()));
                } else {
                    mProxy.addListener(listener);
                }
            }

            public void removeListener(IPackageManagerListener listener) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.removeListener(IPackageManagerListener.Stub.asInterface(listener.asBinder()));
                } else {
                    mProxy.removeListener(listener);
                }
            }
        }

        static final int MSG_GET_INSTALLED_PACKAGES = 1;
        static final int MSG_QUERY_INTENT_SERVICES = 2;
        static final int MSG_RESOLVE_SERVICE = 3;
        static final int MSG_CHECK_PERMISSION = 4;
        static final int MSG_GET_PERMISSIONS = 5;
        static final int MSG_ADD_LISTENER = 6;
        static final int MSG_REMOVE_LISTENER = 7;
    }

    public List getInstalledPackages(int flags) throws RemoteException;

    public List queryIntentServices(Intent intent, int flags) throws RemoteException;

    public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException;

    public int checkPermission(String permissionName, int pid) throws RemoteException;

    public String[] getPermissions(int pid) throws RemoteException;

    public void addListener(IPackageManagerListener listener) throws RemoteException;

    public void removeListener(IPackageManagerListener listener) throws RemoteException;
}
