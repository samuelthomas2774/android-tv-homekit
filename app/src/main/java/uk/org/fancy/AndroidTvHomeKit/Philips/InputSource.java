package uk.org.fancy.AndroidTvHomeKit.Philips;

import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.InputSourceInterface;

public abstract class InputSource implements InputSourceInterface {
    private final InputSourceManager inputSourceManager;

    public InputSource(InputSourceManager _inputSourceManager) {
        inputSourceManager = _inputSourceManager;
    }

    public String getName() {
        return "TV";
    }

    public InputSourceTypes getInputSourceType() {
        return InputSourceTypes.TUNER;
    }

    public boolean isConfigured() {
        return true;
    }

    public boolean getCurrentVisibilityState() {
        return true;
    }

    public String getConfiguredName() {
        return getName();
    }

    public void setConfiguredName(String name) {
        Log.i("HomeKit:InputSource", "Set configured name " + getName() + " to " + name);
    }

    /**
     * Set the current source to this.
     */
    public void activate() {
        Log.i("HomeKit:InputSource", "Activate " + getName());
    }
}
