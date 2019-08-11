package uk.org.fancy.AndroidTvHomeKit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.lang.Thread;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import io.github.hapjava.HomekitServer;
import io.github.hapjava.HomekitStandaloneAccessoryServer;

public class HomeKitService extends Service {
    public static final String HOMEKIT_VERSION = "0.1.0";
    private static final String TAG = "HomeKit:HomeKitService";
    private static final int PORT = 9123;
    private static WakeLock wakeLock = null;
    private HomekitServer homekit;
    private AuthInfo authInfo;
    private HomekitStandaloneAccessoryServer accessoryServer;
    private boolean started = false;
    public static HomeKitService instance;
    public TelevisionInterface implementation;

    private TelevisionInterface getImplementation() {
        return new uk.org.fancy.AndroidTvHomeKit.Philips.Television(this);
    }

    private Thread homekitThread = new Thread() {
        public void run() {
            try {
                InetAddress address = InetAddress.getByAddress(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
                homekit = new HomekitServer(address, PORT);
                TelevisionAccessory accessory = new TelevisionAccessory(HomeKitService.this);
                accessoryServer = homekit.createStandaloneAccessory(authInfo, accessory, 31 /* Television */);
            } catch (Exception e) {
                Log.e(TAG, "Error creating accessory???");
                e.printStackTrace();
            }

            Log.i(TAG, "Starting HAP server");

            accessoryServer.start();
            started = true;

            Log.i(TAG, "Started HAP server");
        }
    };

    public HomeKitService() throws IOException, UnknownHostException {}

    public void onCreate() {
        instance = this;

        Log.i(TAG, "onCreate");

        implementation = getImplementation();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();
        Log.i(TAG, "Acquired wake lock");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (started) {
            Log.i(TAG, "Not starting as the service has already been started");
            return 1;
        }

        // The service is starting, due to a call to startService()
        Log.i(TAG, "onStartCommand");

        String systemName = implementation.getName();
        String model = implementation.getModel();
        String serialNumber = implementation.getSerialNumber();
        String softwareVersion = implementation.getFirmwareRevision();

        Log.i(TAG, "System Name: " + systemName);
        Log.i(TAG, "Model: " + model);
        Log.i(TAG, "Serial Number: " + serialNumber);
        Log.i(TAG, "Software Version: " + softwareVersion);

        try {
            try {
                FileInputStream fileInputStream = getApplicationContext().openFileInput("auth-state.bin");
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

                try {
                    Log.i(TAG, "Using saved auth data");
                    AuthState authState = (AuthState) objectInputStream.readObject();
                    authInfo = new AuthInfo(authState);
                } finally {
                    objectInputStream.close();
                }
            } catch (FileNotFoundException e) {
                Log.i(TAG, "Creating new auth data");
                authInfo = new AuthInfo();
            } catch (IOException e) {
                Log.i(TAG, "Error reading auth data, creating new auth data");
                authInfo = new AuthInfo();
            } catch (ClassNotFoundException e) {
                Log.i(TAG, "ClassNotFoundException reading auth data, creating new auth data");
                authInfo = new AuthInfo();
            }

            authInfo.onChange(state -> {
                try {
                    Log.i(TAG, "State has changed! Writing");
                    FileOutputStream fileOutputStream = getApplicationContext().openFileOutput("auth-state.bin", 0);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(state);
                    objectOutputStream.flush();
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            homekitThread.start();
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG, "InvalidAlgorithmParameterException starting server???");
        }

        return 1;
    }

    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "onBind");
        return null;
    }

    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.i(TAG, "onDestroy");

        if (accessoryServer != null) {
            Log.i(TAG, "Stopping HAP server");
            accessoryServer.stop();
            accessoryServer = null;
            started = false;
        }

        wakeLock.release();
        Log.i(TAG, "Released wake lock");
        wakeLock = null;
    }
}
