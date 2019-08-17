package uk.org.fancy.AndroidTvHomeKit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import io.github.hapjava.HomekitAdvertiser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Advertiser implements HomekitAdvertiser {
    private static final String TAG = "HomeKit:Advertiser";

    private static final String SERVICE_TYPE = "_hap._tcp";

    private boolean discoverable = true;
    private boolean isAdvertising = false;
    private NsdServiceInfo serviceInfo;

    private final NsdManager nsdManager;
    public int category = 1;
    private String label;
    private String mac;
    private int port;
    private int configurationIndex;

    private final RegistrationListener registrationListener = new RegistrationListener() {
        @Override
        public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            String serviceName = NsdServiceInfo.getServiceName();
            Log.i(TAG, "Service registered with name " + serviceName);
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Registration failed! Put debugging code here to determine why.
            Log.i(TAG, "Service registration failed with code " + Integer.toString(errorCode));
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo arg0) {
            // Service has been unregistered. This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
            Log.i(TAG, "Service unregistered");

            if (isAdvertising) {
                try {
                    registerService();
                } catch (IOException e) {
                    Log.e(TAG, "IOException registering service");
                }
            }
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Unregistration failed. Put debugging code here to determine why.
            Log.i(TAG, "Service unregistration failed with code " + Integer.toString(errorCode));
        }
    };

    public Advertiser(NsdManager _nsdManager, int _category) {
        nsdManager = _nsdManager;
        category = _category;
    }

    public Advertiser(NsdManager nsdManager) {
        this(nsdManager, 1);
    }

    public synchronized void advertise(String label, String mac, int port, int configurationIndex) throws Exception {
        if (isAdvertising) {
            throw new IllegalStateException("Homekit advertiser is already running");
        }

        this.label = label;
        this.mac = mac;
        this.port = port;
        this.configurationIndex = configurationIndex;

        Log.i(TAG, "Advertising accessory " + label);

        registerService();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log.i(TAG, "Stopping advertising in response to shutdown.");
            stop();
        }));

        isAdvertising = true;
    }

    public synchronized void stop() {
        isAdvertising = false;
        nsdManager.unregisterService(registrationListener);
    }

    public synchronized void setDiscoverable(boolean discoverable) throws IOException {
        if (this.discoverable == discoverable) return;

        this.discoverable = discoverable;

        if (isAdvertising) {
            Log.i(TAG, "Re-creating service due to change in discoverability to " + discoverable);
            nsdManager.unregisterService(registrationListener);
        }
    }

    public synchronized void setConfigurationIndex(int revision) throws IOException {
        if (this.configurationIndex == revision) return;

        this.configurationIndex = revision;

        if (isAdvertising) {
            Log.i(TAG, "Re-creating service due to change in configuration index to " + revision);
            nsdManager.unregisterService(registrationListener);
        }
    }

    private void registerService() throws IOException {
        Log.i(TAG, "Registering " + SERVICE_TYPE + " on port " + port);

        // Create the NsdServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName(label);
        serviceInfo.setServiceType(SERVICE_TYPE);
        // serviceInfo.setHost(InetAddress.getByName("192.168.3.254")); // Hard code for now
        serviceInfo.setPort(port);
        serviceInfo.setAttribute("sf", discoverable ? "1" : "0");
        serviceInfo.setAttribute("id", mac);
        serviceInfo.setAttribute("md", label);
        serviceInfo.setAttribute("c#", Integer.toString(configurationIndex));
        serviceInfo.setAttribute("s#", "1");
        serviceInfo.setAttribute("ff", "0");
        serviceInfo.setAttribute("ci", Integer.toString(category));

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }
}
