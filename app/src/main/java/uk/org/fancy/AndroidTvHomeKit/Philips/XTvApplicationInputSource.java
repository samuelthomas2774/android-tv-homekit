package uk.org.fancy.AndroidTvHomeKit.Philips;

import android.content.Intent;
import android.media.tv.TvInputInfo;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.ApplicationInputSourceInterface;
import uk.org.fancy.AndroidTvHomeKit.Philips.xtv.XTvHttp;

public class XTvApplicationInputSource extends InputSource implements ApplicationInputSourceInterface {
    private static final String TAG = "HomeKit:XTvApplicationInputSource";
    private final InputSourceManager inputSourceManager;
    public final XTvHttp.Application application;

    public XTvApplicationInputSource(InputSourceManager _inputSourceManager, XTvHttp.Application _application) {
        super(_inputSourceManager);

        inputSourceManager = _inputSourceManager;
        application = _application;
    }

    public String getId() {
        return "XTvApplication:" + getApplicationId();
    }

    public String getApplicationId() {
        return application.id;
    }

    public String getName() {
        return application.label;
    }

    public InputSourceTypes getInputSourceType() {
        if (application.intent.activity.packageName == "org.droidtv.channels") {
            return InputSourceTypes.OTHER;
        } else if (application.intent.activity.packageName == "org.droidtv.contentexplorer") {
            return InputSourceTypes.USB;
        } else {
            return InputSourceTypes.APPLICATION;
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
        return "";
    }

    public void setConfiguredName(String name) {
        Log.i(TAG, "Set configured name " + getName() + " to " + name);
    }

    /**
     * Set the current source to this.
     */
    public void activate() {
        Log.i(TAG, "Activate XTvApplication " + getName());

        inputSourceManager.television.xtvhttp.launch(application.intent);
    }
}
