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

package mindroid.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import mindroid.content.pm.PackageManager;
import mindroid.os.IBinder;
import mindroid.os.Looper;
import mindroid.util.concurrent.Future;

/**
 * Proxying implementation of Context that simply delegates all of its calls to another Context. Can
 * be subclassed to modify behavior without changing the original Context.
 */
public class ContextWrapper extends Context {
    Context mBaseContext;

    public ContextWrapper(Context baseContext) {
        mBaseContext = baseContext;
    }

    /**
     * Set the base context for this ContextWrapper. All calls will then be delegated to the base
     * context. Throws IllegalStateException if a base context has already been set.
     * 
     * @param baseContext The new base context for this wrapper.
     */
    protected void attachBaseContext(Context baseContext) {
        if (mBaseContext != null) {
            throw new IllegalStateException("Base context already set");
        }
        mBaseContext = baseContext;
    }

    /**
     * @return the base context as set by the constructor or setBaseContext
     */
    public Context getBaseContext() {
        return mBaseContext;
    }

    @Override
    public PackageManager getPackageManager() {
        return mBaseContext.getPackageManager();
    }

    @Override
    public Looper getMainLooper() {
        return mBaseContext.getMainLooper();
    }

    @Override
    public String getPackageName() {
        return mBaseContext.getPackageName();
    }

    @Override
    public File getSharedPrefsFile(String name) {
        return mBaseContext.getSharedPrefsFile(name);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return mBaseContext.getSharedPreferences(name, mode);
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return mBaseContext.openFileInput(name);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return mBaseContext.openFileOutput(name, mode);
    }

    @Override
    public boolean deleteFile(String name) {
        return mBaseContext.deleteFile(name);
    }

    @Override
    public File getFilesDir() {
        return mBaseContext.getFilesDir();
    }

    @Override
    public IBinder getSystemService(URI name) {
        return mBaseContext.getSystemService(name);
    }

    @Override
    public Future<ComponentName> startService(Intent service) {
        return mBaseContext.startService(service);
    }

    @Override
    public Future<Boolean> stopService(Intent service) {
        return mBaseContext.stopService(service);
    }

    @Override
    public Future<Boolean> bindService(Intent service, ServiceConnection conn, int flags) {
        return mBaseContext.bindService(service, conn, flags);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        mBaseContext.unbindService(conn);
    }
}
