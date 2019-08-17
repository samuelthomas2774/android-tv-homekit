package uk.org.fancy.AndroidTvHomeKit.Philips;

import android.content.Intent;
import android.media.tv.TvInputInfo;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.TIFInputSourceInterface;

public class TIFInputSource extends InputSource implements TIFInputSourceInterface {
    private static final String TAG = "HomeKit:TIFInputSource";
    private final InputSourceManager inputSourceManager;
    public final TvInputInfo tvInputInfo;

    public TIFInputSource(InputSourceManager _inputSourceManager, TvInputInfo _tvInputInfo) {
        super(_inputSourceManager);

        inputSourceManager = _inputSourceManager;
        tvInputInfo = _tvInputInfo;
    }

    public String getId() {
        return "TIF:" + getTifId();
    }

    public String getTifId() {
        return tvInputInfo.getId();
    }

    public String getName() {
        return tvInputInfo.loadLabel(inputSourceManager.television.service).toString();
    }

    public InputSourceTypes getInputSourceType() {
        switch (tvInputInfo.getType()) {
            case TvInputInfo.TYPE_COMPONENT:
            case TvInputInfo.TYPE_SCART:
                return InputSourceTypes.COMPONENT_VIDEO;
            case TvInputInfo.TYPE_COMPOSITE:
            case TvInputInfo.TYPE_VGA:
                return InputSourceTypes.COMPOSITE_VIDEO;
            case TvInputInfo.TYPE_DISPLAY_PORT:
            case TvInputInfo.TYPE_DVI:
                return InputSourceTypes.DVI;
            case TvInputInfo.TYPE_HDMI:
                return InputSourceTypes.HDMI;
            case TvInputInfo.TYPE_OTHER:
            default:
                return InputSourceTypes.OTHER;
            case TvInputInfo.TYPE_SVIDEO:
                return InputSourceTypes.S_VIDEO;
            case TvInputInfo.TYPE_TUNER:
                return InputSourceTypes.TUNER;

            // OTHER,
            // HOME_SCREEN,
            // AIRPLAY,
            // USB,
            // APPLICATION,
        }
    }

    public boolean isConfigured() {
        // TODO: return false for tuners with no configured channels (and inputs with nothing connected?)
        return true;
    }

    public boolean getCurrentVisibilityState() {
        return true;
    }

    public String getConfiguredName() {
        CharSequence customLabel = tvInputInfo.loadCustomLabel(inputSourceManager.television.service);

        if (customLabel == null) return null;

        return customLabel.toString();
    }

    public void setConfiguredName(String name) {
        Log.i(TAG, "Set configured name " + getName() + " to " + name);
    }

    /**
     * Set the current source to this.
     */
    public void activate() {
        Log.i(TAG, "Activate TIF " + getName());

        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");

        if (tvInputInfo.getType() == TvInputInfo.TYPE_TUNER) {
            Log.i(TAG, "Switching to tuner source " + tvInputInfo.getId());

            Uri sourceUri3 = TvContract.buildChannelsUriForInput(tvInputInfo.getId());
            Log.i(TAG, "TYPE_TUNER sourceUri:" + sourceUri3);
            intent.setData(sourceUri3);
        } else {
            Log.i(TAG, "Switching to non-tuner source " + tvInputInfo.getId());

            Uri sourceUri4 = TvContract.buildChannelUriForPassthroughInput(tvInputInfo.getId());
            Log.i(TAG, "PASSTHROUGH sourceUri:" + sourceUri4);
            intent.setData(sourceUri4);
        }

        inputSourceManager.television.service.startActivity(intent);

        // TODO: tell the source/channel list (org.droidtv.channels) the source has changed
    }
}
