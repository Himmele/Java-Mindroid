/*
 * Copyright (C) 2006 The Android Open Source Project
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

package mindroid.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.ServiceConnection;
import mindroid.content.SharedPreferences;
import mindroid.content.pm.PackageManager;
import mindroid.os.Bundle;
import mindroid.os.Environment;
import mindroid.os.Handler;
import mindroid.os.HandlerThread;
import mindroid.os.IBinder;
import mindroid.os.IServiceManager;
import mindroid.os.Looper;
import mindroid.os.RemoteCallback;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.Log;

/**
 * Common implementation of Context API, which provides the base
 * context object for Activity and other application components.
 */
public class ContextImpl extends Context {
    private final static String LOG_TAG = "ContextImpl";
    private final IServiceManager mServiceManager;
    private final HandlerThread mMainThread;
    private final Handler mHandler;
    private ComponentName mComponent;
    private HashMap mServiceConnections = new HashMap();
    private PackageManager mPackageManager;

    public ContextImpl(HandlerThread mainThread, ComponentName component) {
        mServiceManager = ServiceManager.getServiceManager();
        mMainThread = mainThread;
        mHandler = new Handler(mainThread.getLooper());
        mComponent = component;
    }

    public PackageManager getPackageManager() {
        if (mPackageManager != null) {
            return mPackageManager;
        }

        return (mPackageManager = new PackageManager(this));
    }

    public Looper getMainLooper() {
        return mMainThread.getLooper();
    }

    public String getPackageName() {
        if (mComponent != null) {
            return mComponent.getPackageName();
        }
        return "mindroid";
    }

    public File getSharedPrefsFile(String name) {
        return makeFilename(getPreferencesDir(), name + ".xml");
    }

    public SharedPreferences getSharedPreferences(String name, int mode) {
        return Environment.getSharedPreferences(getSharedPrefsFile(name), mode);
    }

    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        File file = makeFilename(Environment.getDataDirectory(), name);
        return new FileInputStream(file);
    }

    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        final boolean append = (mode & MODE_APPEND) != 0;
        File file = makeFilename(Environment.getDataDirectory(), name);
        try {
            FileOutputStream fos = new FileOutputStream(file, append);
            return fos;
        } catch (FileNotFoundException e) {
        }

        return null;
    }

    public boolean deleteFile(String name) {
        File file = makeFilename(getFilesDir(), name);
        return file.delete();
    }

    public File getFilesDir() {
        return Environment.getDataDirectory();
    }

    public IBinder getSystemService(String name) {
        if (name != null) {
            return ServiceManager.getSystemService(name);
        } else {
            return null;
        }
    }

    public ComponentName startService(Intent service) {
        if (service != null) {
            try {
                return mServiceManager.startService(service);
            } catch (RemoteException e) {
                throw new RuntimeException("System failure");
            }
        } else {
            return null;
        }
    }

    public boolean stopService(Intent service) {
        if (service != null) {
            try {
                return mServiceManager.stopService(service);
            } catch (RemoteException e) {
                throw new RuntimeException("System failure");
            }
        } else {
            return false;
        }
    }

    public boolean bindService(final Intent service, final ServiceConnection conn, int flags) {
        if (service != null && conn != null) {
            if (mServiceConnections.containsKey(conn)) {
                return true;
            }
            mServiceConnections.put(conn, service);
            RemoteCallback callback = new RemoteCallback(new RemoteCallback.OnResultListener() {
                public void onResult(Bundle data) {
                    boolean result = data.getBoolean("result");
                    if (result) {
                        IBinder binder = data.getBinder("binder");
                        conn.onServiceConnected(service.getComponent(), binder);
                    } else {
                        Log.e(LOG_TAG, "Cannot bind to service " + service.getComponent().getPackageName() + "." + service.getComponent().getClassName());
                    }
                }
            }, mHandler);
            try {
                return mServiceManager.bindService(service, conn, flags, callback.asInterface());
            } catch (RemoteException e) {
                throw new RuntimeException("System failure");
            }
        } else {
            return false;
        }
    }

    public void unbindService(ServiceConnection conn) {
        if (conn != null) {
            if (mServiceConnections.containsKey(conn)) {
                Intent service = (Intent) mServiceConnections.get(conn);
                mServiceConnections.remove(conn);
                RemoteCallback callback = new RemoteCallback(new RemoteCallback.OnResultListener() {
                    public void onResult(Bundle data) {
                    }
                }, mHandler);
                try {
                    mServiceManager.unbindService(service, conn, callback.asInterface());
                } catch (RemoteException e) {
                }
            }
        }
    }

    private File getPreferencesDir() {
        if (!Environment.getPreferencesDirectory().exists()) {
            Environment.getPreferencesDirectory().mkdir();
        }
        return Environment.getPreferencesDirectory();
    }

    private File makeFilename(File baseDir, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(baseDir, name);
        }
        throw new IllegalArgumentException("File " + name + " contains a path separator");
    }

    public void cleanup() {
        Iterator itr = mServiceConnections.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry pair = (Map.Entry) itr.next();
            ServiceConnection conn = (ServiceConnection) pair.getKey();
            Intent service = (Intent) pair.getValue();
            itr.remove();
            try {
                mServiceManager.unbindService(service, conn);
            } catch (RemoteException e) {
            }
            Log.w(LOG_TAG, "Service " + mComponent + " is leaking a ServiceConnection to " + service.getComponent());
        }
    }
}
