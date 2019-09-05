package uk.org.fancy.AndroidTvHomeKit.Philips;

// import java.lang.reflect.Method;
import java.util.Collection;
// import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.LinkedList;
import java.util.List;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputInfo;
import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.PollThread;
import uk.org.fancy.AndroidTvHomeKit.InputSourceManagerInterface;
import uk.org.fancy.AndroidTvHomeKit.InputSourceInterface;
import uk.org.fancy.AndroidTvHomeKit.Philips.xtv.XTvHttp;
import io.github.hapjava.HomekitCharacteristicChangeCallback;

public class InputSourceManager implements InputSourceManagerInterface, PollThread.PollInterface {
    private static final String TAG = "HomeKit:InputSourceManager";
    public final Television television;
    private final Collection<InputSourceInterface> inputSources = new LinkedList<InputSourceInterface>();
    private final HomeScreenInputSource homeScreenInputSource;
    private final Collection<XTvApplicationInputSource> xtvApplicationInputSources = new LinkedList<XTvApplicationInputSource>();
    private final Collection<TIFInputSource> tifInputSources = new LinkedList<TIFInputSource>();
    private HomekitCharacteristicChangeCallback callback = null;
    private InputSourceInterface lastInputSource = null;

    public InputSourceManager(Television _television) {
        television = _television;

		// Television
		// InputSource inputSource = new InputSource(this);
		// inputSources.add(inputSource);

        homeScreenInputSource = new HomeScreenInputSource(this);
        inputSources.add(homeScreenInputSource);

        TvInputManager tvInputManager = (TvInputManager) television.service.getSystemService("tv_input");
        List<TvInputInfo> tifInputs = tvInputManager.getTvInputList();

        for (TvInputInfo tvInputInfo: tifInputs) {
            CharSequence customLabel = tvInputInfo.loadCustomLabel(television.service);

            Log.i(TAG, "Input ID " + tvInputInfo.getId() + "; parent ID " + tvInputInfo.getParentId());
            Log.i(TAG, "Name/Label " + tvInputInfo.loadLabel(television.service).toString() +
                "; Custom Label " + (customLabel != null ? customLabel.toString() : "null"));
            Log.i(TAG, "Type " + tvInputInfo.getType() + "; tuner count " + tvInputInfo.getTunerCount());
            Log.i(TAG, "Hidden? " + tvInputInfo.isHidden(television.service) + " Passthrough? " + tvInputInfo.isPassthroughInput());

            if (tvInputInfo.getType() == TvInputInfo.TYPE_HDMI && tvInputInfo.getParentId() != null) {
                Log.i(TAG, "Not registering child HDMI input " + tvInputInfo.getId());
                continue;
            }

            Log.i(TAG, "Registering TIF input source " + tvInputInfo.getId());

            TIFInputSource tifInputSource = new TIFInputSource(this, tvInputInfo);
            tifInputSources.add(tifInputSource);
            inputSources.add(tifInputSource);
        }

        // try {
        //     List<XTvHttp.Application> xtvApplicationInputs = television.xtvhttp.getApplications().get();
        //
        //     for (XTvHttp.Application application: xtvApplicationInputs) {
        //         Log.i(TAG, "Application ID " + application.id + "; Name/Label " + application.label);
        //         Log.i(TAG, "Type " + (application.type == XTvHttp.ApplicationType.GAME ? "GAME" : "APP"));
        //         Log.i(TAG, "Activity " + application.intent.activity.packageName + "/" + application.intent.activity.className +
        //             "; intent " + application.intent.action);
        //
        //         if (application.intent.activity.packageName.contentEquals("org.droidtv.playtv")) continue;
        //         if (application.intent.activity.packageName.contentEquals("org.droidtv.eum")) {
        //             if (!application.intent.activity.className.contentEquals("org.droidtv.eum.onehelp.menu.HowToLauncherActivity")) continue;
        //         }
        //         if (application.intent.activity.packageName.contentEquals("org.droidtv.settings")) {
        //             // if (application.intent.activity.className != "org.droidtv.settings.setupmenu.SetupMenuActivity") continue;
        //             continue;
        //         }
        //         if (application.intent.activity.packageName.contentEquals("com.android.tv.settings")) continue;
        //
        //         Log.i(TAG, "Registering XTv Application input source " + application.id);
        //
        //         XTvApplicationInputSource xtvApplicationInputSource = new XTvApplicationInputSource(this, application);
        //         xtvApplicationInputSources.add(xtvApplicationInputSource);
        //         inputSources.add(xtvApplicationInputSource);
        //     }
        // } catch (Exception err) {
        //     Log.e(TAG, "Error getting applications: " + err.toString());
        //     throw new RuntimeException("Error getting applications", err);
        // }
    }

    public Collection<InputSourceInterface> getInputSources() {
        return inputSources;
    }

    private String getCurrentInputSourceId() {
        return television.getSystemProperty("persist.sys.inputid");
    }

    public CompletableFuture<InputSourceInterface> getActiveInput() {
        return television.xtvhttp.getCurrentActivity().thenApply(activity -> {
            Log.i(TAG, "Current activity " + activity.packageName + "/" + activity.className);

            if (activity.packageName.contentEquals("com.google.android.tvlauncher")) {
                return homeScreenInputSource;
            }

            if (activity.packageName.contentEquals("org.droidtv.playtv") &&
                activity.className.contentEquals("org.droidtv.playtv.PlayTvActivity")
            ) {
                String inputid = getCurrentInputSourceId();
                Log.i(TAG, "Current input source property " + inputid);

                for (TIFInputSource inputSource: tifInputSources) {
                    if (!inputSource.tvInputInfo.getId().equals(inputid)) continue;

                    Log.i(TAG, "Current input source name " + inputSource.getName());
                    return inputSource;
                }

                Log.i(TAG, "Didn't find active input from ID " + inputid);
            }

            List<XTvApplicationInputSource> matchingApplicationInputSources = new LinkedList<XTvApplicationInputSource>();
            for (XTvApplicationInputSource inputSource: xtvApplicationInputSources) {
                if (inputSource.application.intent.activity.packageName == activity.packageName) {
                    if (inputSource.application.intent.activity.className == activity.className) return inputSource;

                    matchingApplicationInputSources.add(inputSource);
                }
            }

            // If there's only one ActivityInputSource for the current activity's package, use that (even if it doesn't match exactly)
            // if (package_activity_input_sources.length === 1) return package_activity_input_sources[0];
            if (matchingApplicationInputSources.size() == 1) return matchingApplicationInputSources.get(0);

            Log.w(TAG, "No input sources match the current activity " + activity.packageName);

            return homeScreenInputSource;
        }).thenApply(inputSource -> {
            lastInputSource = inputSource;

            return inputSource;
        });
    }

    public void setActiveInput(InputSourceInterface inputSource) {
        inputSource.activate();
    }

    public void onSubscribe(HomekitCharacteristicChangeCallback _callback) {
        callback = _callback;
        television.service.pollThread.add(this);
    }

    public void onUnsubscribe() {
        television.service.pollThread.remove(this);
        callback = null;
    }

    public void poll() throws Exception {
        Log.i(TAG, "Polling input source; was " + (this.lastInputSource == null ? "unknown" : this.lastInputSource.getId()));
        InputSourceInterface lastInputSource = this.lastInputSource;
        InputSourceInterface inputSource = getActiveInput().get();

        if (inputSource != lastInputSource) {
            Log.i(TAG, "Input source changed; now " + inputSource.getId());

            callback.changed();

            this.lastInputSource = inputSource;
        }
    }
}
