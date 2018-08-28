/*
 * Copyright (C) 2018 E.S.R.Labs
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

package mindroid.runtime.system.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.Parcel;
import mindroid.os.RemoteException;
import mindroid.util.Log;
import mindroid.util.concurrent.Promise;

public abstract class AbstractClient {
    private final String LOG_TAG;
    private static final int SHUTDOWN_TIMEOUT = 10000; //ms
    private static final int CONNECTION_ESTABLISHMENT_TIMEOUT = 10000;
    private static final boolean DEBUG = false;

    private final Socket mSocket;
    private String mHost;
    private int mPort;
    private final Connection mConnection;
    private final int mNodeId;

    public AbstractClient(int nodeId, String uri) throws IOException {
        LOG_TAG = "Client (" + uri + ")";
        mNodeId = nodeId;

        try {
            URI url = new URI(uri);
            if (!"tcp".equals(url.getScheme())) {
                throw new IllegalArgumentException("Invalid URI scheme: " + url.getScheme());
            }
            mHost = url.getHost();
            mPort = url.getPort();
            mSocket = new Socket();
            mConnection = new Connection();

            try {
                mSocket.connect(new InetSocketAddress(mHost, mPort), CONNECTION_ESTABLISHMENT_TIMEOUT);
                mConnection.start(mSocket);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                shutdown();
                throw e;
            }
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI: " + uri);
        }
    }

    protected void shutdown() {
        if (mConnection != null) {
            try {
                mConnection.close();
            } catch (IOException ignore) {
            }
        }
    }

    public int getNodeId() {
        return mNodeId;
    }

    public abstract Promise<Parcel> transact(IBinder binder, int what, Parcel data, int flags) throws RemoteException;

    public abstract void onTransact(Bundle context, InputStream inputStream, OutputStream outputStream) throws IOException;

    public Bundle getContext() {
        return mConnection.mContext;
    }

    public Connection getConnection() {
        return mConnection;
    }

    public class Connection extends Thread implements Closeable {
        private Bundle mContext = new Bundle();
        private Socket mSocket;
        private InputStream mInputStream;
        private OutputStream mOutputStream;

        public Connection() {
        }

        public Bundle getContext() {
            return mContext;
        }

        public InputStream getInputStream() {
            return mInputStream;
        }

        public OutputStream getOutputStream() {
            return mOutputStream;
        }

        public void start(Socket socket) {
            setName("Client: " + socket.getLocalSocketAddress() + " <<>> " + socket.getRemoteSocketAddress());
            mContext.putObject("connection", this);
            mSocket = socket;
            try {
                mInputStream = socket.getInputStream();
                mOutputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Failed to set up connection", e);
                shutdown();
            }
            super.start();
        }

        @Override
        public void close() throws IOException {
            if (DEBUG) {
                Log.d(LOG_TAG, "Closing connection");
            }
            interrupt();
            try {
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Cannot close socket", e);
                    }
                }
                if (mInputStream != null) {
                    try {
                        mInputStream.close();
                    } catch (IOException ignore) {
                    }
                }
                if (mOutputStream != null) {
                    try {
                        mOutputStream.close();
                    } catch (IOException ignore) {
                    }
                }
                join(SHUTDOWN_TIMEOUT);
                if (isAlive()) {
                    Log.e(LOG_TAG, "Cannot shutdown connection");
                }
            } catch (InterruptedException ignore) {
            }
            if (DEBUG) {
                Log.d(LOG_TAG, "Connection has been closed");
            }
        }

        public void run() {
            while (!isInterrupted()) {
                try {
                    AbstractClient.this.onTransact(mContext, mInputStream, mOutputStream);
                } catch (IOException e) {
                    if (DEBUG) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                    }
                    shutdown();
                    break;
                }
            }

            if (DEBUG) {
                Log.d(LOG_TAG, "Connection has been terminated");
            }
        }
    }
}
