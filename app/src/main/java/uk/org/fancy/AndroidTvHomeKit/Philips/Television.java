package uk.org.fancy.AndroidTvHomeKit.Philips;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.HomeKitService;
import uk.org.fancy.AndroidTvHomeKit.TelevisionInterface;
import uk.org.fancy.AndroidTvHomeKit.Philips.xtv.XTvHttp;
import io.github.hapjava.Service;
// import org.droidtv.tv.persistentstorage.TvSettingsConstants;

public class Television implements TelevisionInterface,
        TelevisionInterface.TelevisionSpeakerInterface,
        TelevisionInterface.RemoteInterface,
        TelevisionInterface.AdditionalServices {
    private static final String TAG = "HomeKit:Television";
    public final HomeKitService service;
    public final XTvHttp xtvhttp;
    private final PowerState powerStateManager = new PowerState(this);
    private final InputSourceManager inputSourceManager;
    private final TelevisionSpeaker speaker;
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
        speaker = new TelevisionSpeaker(this);

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

    public List<Service> getAdditionalServices() {
        List<Service> services = new LinkedList<Service>();

        // Ambilight service
        Service ambilight = new uk.org.fancy.AndroidTvHomeKit.Philips.Ambilight.Service(this);
        services.add(ambilight);

		return Collections.unmodifiableList(services);
    }

    public PowerState getPowerStateManager() {
        return powerStateManager;
    }

    public InputSourceManager getInputSourceManager() {
        return inputSourceManager;
    }

    public TelevisionSpeaker getSpeaker() {
        return speaker;
    }

    public CompletableFuture<Object> keypress(String key) {
        return xtvhttp.keypress(key);
    }

    public void sendRemoteKey(RemoteKey hapkey) {
        Log.i(TAG, "Send remote key " + hapkey.toString());

        String key = getXTvRemoteKey(hapkey);
        if (key == null) {
            Log.e(TAG, "Invalid remote key " + hapkey.toString());
            return;
        }

        keypress(key);
    }

    private String getXTvRemoteKey(RemoteKey key) {
        switch (key) {
            default:
                return null;
            case REWIND:
                return XTvHttp.RemoteKey.REWIND;
            case FAST_FORWARD:
                return XTvHttp.RemoteKey.FAST_FORWARD;
            // case NEXT_TRACK:
            //     return XTvHttp.RemoteKey.
            // case PREVIOUS_TRACK:
            //     return XTvHttp.RemoteKey.
            case ARROW_UP:
                return XTvHttp.RemoteKey.CURSOR_UP;
            case ARROW_DOWN:
                return XTvHttp.RemoteKey.CURSOR_DOWN;
            case ARROW_LEFT:
                return XTvHttp.RemoteKey.CURSOR_LEFT;
            case ARROW_RIGHT:
                return XTvHttp.RemoteKey.CURSOR_RIGHT;
            case SELECT:
                return XTvHttp.RemoteKey.CONFIRM;
            case BACK:
                return XTvHttp.RemoteKey.BACK;
            case EXIT:
                return XTvHttp.RemoteKey.EXIT;
            case PLAY_PAUSE:
                return XTvHttp.RemoteKey.PLAY_PAUSE;
            case INFORMATION:
                return XTvHttp.RemoteKey.INFO;
        }
    }
}
