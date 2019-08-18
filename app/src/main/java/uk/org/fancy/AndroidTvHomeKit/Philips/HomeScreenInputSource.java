package uk.org.fancy.AndroidTvHomeKit.Philips;

import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.Philips.xtv.XTvHttp;

public class HomeScreenInputSource extends InputSource {
    private static final String TAG = "HomeKit:HomeScreenInputSource";
    private final InputSourceManager inputSourceManager;

    public HomeScreenInputSource(InputSourceManager _inputSourceManager) {
        super(_inputSourceManager);

        inputSourceManager = _inputSourceManager;
    }

    public String getId() {
        return "HomeScreen";
    }

    public String getName() {
        return "Home";
    }

    public InputSourceTypes getInputSourceType() {
        return InputSourceTypes.HOME_SCREEN;
    }

    public boolean isConfigured() {
        // TODO: return false for tuners with no configured channels (and inputs with nothing connected?)
        return true;
    }

    public boolean getCurrentVisibilityState() {
        return true;
    }

    public String getConfiguredName() {
        return "";
    }

    public void setConfiguredName(String name) {
        Log.i(TAG, "Set configured name " + getName() + " to " + name);
    }

    /**
     * Set the current source to this.
     */
    public void activate() {
        inputSourceManager.television.keypress(XTvHttp.RemoteKey.HOME);
    }
}
