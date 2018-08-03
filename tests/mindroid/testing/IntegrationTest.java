/*
 * Copyright (C) 2018 Daniel Himmelein
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

package mindroid.testing;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.pm.PackageInfo;
import mindroid.content.pm.PackageInstaller;
import mindroid.content.pm.PackageManager;
import mindroid.content.pm.ServiceInfo;
import mindroid.os.Environment;
import mindroid.os.IServiceManager;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.Log;
import mindroid.util.Properties;
import mindroid.util.concurrent.CancellationException;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.TimeoutException;
import mindroid.util.logging.Logger;
import mindroid.runtime.system.Runtime;

public class IntegrationTest {
    private static final String LOG_TAG = "Mindroid";
    private static final ComponentName SERVICE_MANAGER = new ComponentName("mindroid.os", "ServiceManager");
    private static final ComponentName PACKAGE_MANAGER = new ComponentName("mindroid.content.pm", "PackageManagerService");
    private static final ComponentName LOGGER_SERVICE = new ComponentName("mindroid.util.logging", "LoggerService");
    private static final ComponentName CONSOLE_SERVICE = new ComponentName("mindroid.runtime.inspection", "ConsoleService");

    private static ServiceManager sServiceManager;

    @BeforeAll
    public static void setUpTests() {
        final int nodeId = 1;
        final String rootDir = ".";

        System.setProperty(Properties.INTEGRATION_TESTING, "true");
        Log.setIntegrationTesting(true);

        Environment.setRootDirectory(rootDir);

        File file = new File(Environment.getRootDirectory(), "res/MindroidRuntimeSystem.xml");
        Runtime.start(nodeId, file.exists() ? file : null);

        sServiceManager = new ServiceManager();
        sServiceManager.start();

        try {
            startSystemServices();
        } catch (Exception e) {
            throw new RuntimeException("System failure");
        }

        try {
            startServices();
        } catch (Exception e) {
            throw new RuntimeException("System failure");
        }
    }

    @AfterAll
    public static void tearDownTests() {
        try {
            shutdownServices();
        } catch (Exception e) {
            throw new RuntimeException("System failure");
        }

        try {
            shutdownSystemServices();
        } catch (Exception e) {
            throw new RuntimeException("System failure");
        }

        sServiceManager.shutdown();
        sServiceManager = null;

        Runtime.shutdown();
    }

    @BeforeEach
    public void setUp() {
        Logger logger = new Logger();
        logger.mark();
    }

    @AfterEach
    public void tearDown() {
        Logger logger = new Logger();
        logger.reset();
    }

    private static void startSystemServices() throws InterruptedException, CancellationException, ExecutionException, RemoteException {
        IServiceManager serviceManager = ServiceManager.getServiceManager();

        serviceManager.startSystemService(new Intent()
                .setComponent(LOGGER_SERVICE)
                .putExtra("name", Context.LOGGER_SERVICE.toString())
                .putExtra("process", "main"))
                .get();

        serviceManager.startSystemService(new Intent(Logger.ACTION_LOG)
                .setComponent(LOGGER_SERVICE)
                .putExtra("logBuffer", Log.LOG_ID_MAIN)
                .putExtra("logPriority", Log.DEBUG)
                .putExtra("logFlags", new String[] { "timestamp" })
                .putExtra("consoleLogging", true)
                .putExtra("fileLogging", false)
                .putExtra("logFileName", "Log-%g.log")
                .putExtra("logFileLimit", 262144)
                .putExtra("logFileCount", 4));

        serviceManager.startSystemService(new Intent()
                .setComponent(CONSOLE_SERVICE)
                .putExtra("name", Context.CONSOLE_SERVICE.toString())
                .putExtra("process", "main"))
                .get();

        serviceManager.startSystemService(new Intent()
                .setComponent(PACKAGE_MANAGER)
                .putExtra("name", Context.PACKAGE_MANAGER.toString())
                .putExtra("process", "main"))
                .get();
    }

    private static void startServices() throws InterruptedException, RemoteException {
        PackageManager packageManager = new PackageManager();
        PackageInstaller packageInstaller = new PackageInstaller();
        packageInstaller.install(Environment.getAppsDirectory());
        List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_SERVICES);
        if (packages != null) {
            IServiceManager serviceManager = ServiceManager.getServiceManager();
            for (Iterator<PackageInfo> itr = packages.iterator(); itr.hasNext();) {
                PackageInfo p = itr.next();
                if (p.services != null) {
                    ServiceInfo[] services = p.services;
                    for (int i = 0; i < services.length; i++) {
                        ServiceInfo serviceInfo = services[i];
                        if (serviceInfo.isEnabled() && serviceInfo.hasFlag(ServiceInfo.FLAG_AUTO_START)) {
                            Intent service = new Intent();
                            service.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));
                            try {
                                serviceManager.startService(service).get(10000);
                            } catch (CancellationException | ExecutionException | TimeoutException | RemoteException e) {
                                throw new RuntimeException("System failure");
                            }
                        }
                    }
                }
            }
        }
    }

    private static void shutdownServices() throws InterruptedException, RemoteException {
        PackageManager packageManager = new PackageManager();
        List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_SERVICES);
        if (packages != null) {
            IServiceManager serviceManager = ServiceManager.getServiceManager();
            for (Iterator<PackageInfo> itr = packages.iterator(); itr.hasNext();) {
                PackageInfo p = itr.next();
                if (p.services != null) {
                    ServiceInfo[] services = p.services;
                    for (int i = 0; i < services.length; i++) {
                        ServiceInfo serviceInfo = services[i];
                        if (serviceInfo.isEnabled()) {
                            Intent service = new Intent();
                            service.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));
                            try {
                                serviceManager.stopService(service).get(10000);
                            } catch (CancellationException | ExecutionException | TimeoutException | RemoteException ignore) {
                            }
                        }
                    }
                }
            }
        }
    }

    private static void shutdownSystemServices() throws InterruptedException, CancellationException, ExecutionException, RemoteException {
        IServiceManager serviceManager = ServiceManager.getServiceManager();

        serviceManager.stopSystemService(new Intent()
                .setComponent(PACKAGE_MANAGER))
                .get();

        serviceManager.stopSystemService(new Intent()
                .setComponent(CONSOLE_SERVICE))
                .get();

        serviceManager.stopSystemService(new Intent()
                .setComponent(LOGGER_SERVICE))
                .get();
    }
}
