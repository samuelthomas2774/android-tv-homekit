package uk.org.fancy.AndroidTvHomeKit.Philips;

import java.lang.reflect.Method;
import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.HomeKitService;
import uk.org.fancy.AndroidTvHomeKit.TelevisionInterface;
// import org.droidtv.tv.persistentstorage.TvSettingsConstants;

public class Television implements TelevisionInterface {
    private static final String TAG = "HomeKit:Television";
    public final HomeKitService service;
    private final PowerState powerStateManager = new PowerState(this);
    private final InputSourceManager inputSourceManager;

    public Television(HomeKitService _service) {
        service = _service;
        inputSourceManager = new InputSourceManager(this);
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

    public String getName() {
        return getSettingsString(121); // TvSettingsConstants.CBTVNAME
    }

    public String getManufacturer() {
        // Hard code for now
        return "Philips";
    }

    public String getModel() {
        return getSettingsString(311); // TvSettingsConstants.SETTYPE
    }

    public String getSerialNumber() {
        return getSettingsString(312); // TvSettingsConstants.PRODUCTIONCODE
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
        return getSystemProperty("ro.tpvision.product.swversion");
    }

    public PowerState getPowerStateManager() {
        return powerStateManager;
    }

    public InputSourceManager getInputSourceManager() {
        return inputSourceManager;
    }

    public void sendInputKey(int key) {
        Log.i(TAG, "Send input key " + Integer.toString(key));

        // TODO
    }
}
