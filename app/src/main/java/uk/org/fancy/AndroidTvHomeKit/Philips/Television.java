package uk.org.fancy.AndroidTvHomeKit.Philips;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.HomeKitService;
import uk.org.fancy.AndroidTvHomeKit.TelevisionInterface;
import uk.org.fancy.AndroidTvHomeKit.Philips.xtv.XTvHttp;
// import org.droidtv.tv.persistentstorage.TvSettingsConstants;

public class Television implements TelevisionInterface {
    private static final String TAG = "HomeKit:Television";
    public final HomeKitService service;
    public final XTvHttp xtvhttp;
    private final PowerState powerStateManager = new PowerState(this);
    private final InputSourceManager inputSourceManager;
    private String name;
    private final String manufacturer = "Philips"; // Hard code for now
    private String model;
    private String serialNumber;
    private String firmwareRevision = getSystemProperty("ro.tpvision.product.swversion");

    // Temporary
    private final String username = "";
    private final String password = "";

    public Television(HomeKitService _service) {
        service = _service;
        xtvhttp = new XTvHttp(username, password);
        inputSourceManager = new InputSourceManager(this);

        loadSystemDetails();
    }

    private static Object getTvSettingsManager() {
        try {
            Class<?> contextClass = Class.forName("org.droidtv.tv.context.TvContext");
            Class<?> settingsManagerInterface = Class.forName("org.droidtv.tv.persistentstorage.ITvSettingsManager");

            Method getInterface = contextClass.getMethod("getInterface", Class.class);
            Object settingsManagerInstance = getInterface.invoke(null, settingsManagerInterface);

            return settingsManagerInstance;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getSettingsString(int property) {
        try {
            Object settingsManagerInstance = getTvSettingsManager();
            if (settingsManagerInstance == null) return null;
            Class<?> settingsManagerClass = settingsManagerInstance.getClass();

            Method getString = settingsManagerClass.getMethod("getString", Integer.class, Integer.class, String.class);
            return (String) getString.invoke(settingsManagerInstance, property, 0, null);
        } catch (Exception e) {
            return null;
        }
    }

    private void loadSystemDetails() {
        try {
            XTvHttp.System system = xtvhttp.getSystem().get();

            name = system.name;
            model = system.model;
            serialNumber = system.serialNumber;
            firmwareRevision = system.softwareVersion + "; prop: " + getSystemProperty("ro.tpvision.product.swversion");
        } catch (Exception err) {
            Log.e(TAG, "Error getting system details: " + err.toString());
            throw new RuntimeException("Error getting system details", err);
        }
    }

    public String getName() {
        return name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public static String getSystemProperty(String property) {
        return getSystemProperty(property, null);
    }

    public static String getSystemProperty(String property, String _default) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            return (String) get.invoke(c, property, _default);
        } catch (Exception e) {
            return _default;
        }
    }

    public String getFirmwareRevision() {
        return firmwareRevision;
    }

    public PowerState getPowerStateManager() {
        return powerStateManager;
    }

    public InputSourceManager getInputSourceManager() {
        return inputSourceManager;
    }

    public CompletableFuture<Object> keypress(String key) {
        return xtvhttp.keypress(key);
    }

    public void sendInputKey(int key) {
        Log.i(TAG, "Send input key " + Integer.toString(key));

        // TODO
    }
}
