package uk.org.fancy.AndroidTvHomeKit.Philips;

// import java.lang.reflect.Method;
import java.util.Collection;
// import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputInfo;
import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.InputSourceManagerInterface;
import uk.org.fancy.AndroidTvHomeKit.InputSourceInterface;
import io.github.hapjava.HomekitCharacteristicChangeCallback;

public class InputSourceManager implements InputSourceManagerInterface {
    private static final String TAG = "HomeKit:InputSourceManager";
    public final Television television;
    private final Collection<InputSourceInterface> inputSources = new LinkedList<InputSourceInterface>();
    private final Collection<TIFInputSource> tifInputSources = new LinkedList<TIFInputSource>();
    private HomekitCharacteristicChangeCallback callback = null;

    public InputSourceManager(Television _television) {
        television = _television;

		// Television
		// InputSource inputSource = new InputSource(this);
		// inputSources.add(inputSource);

        TvInputManager tvInputManager = (TvInputManager) television.service.getSystemService("tv_input");
        List<TvInputInfo> tifInputs = tvInputManager.getTvInputList();

        for (TvInputInfo tvInputInfo: tifInputs) {
            CharSequence customLabel = tvInputInfo.loadCustomLabel(television.service);

            Log.i(TAG, "Input ID " + tvInputInfo.getId() + "; parent ID " + tvInputInfo.getParentId());
            Log.i(TAG, "Name/Label " + tvInputInfo.loadLabel(television.service).toString() +
                "; Custom Label " + (customLabel != null ? customLabel.toString() : "null"));
            Log.i(TAG, "Type " + tvInputInfo.getType() + "; tuner count " + tvInputInfo.getTunerCount());
            Log.i(TAG, "Hidden? " + tvInputInfo.isHidden(television.service) + " Passthrough? " + tvInputInfo.isPassthroughInput());

            // if (tvInputInfo.getType() == TvInputInfo.TYPE_HDMI && tvInputInfo.getParentId() != null) {
            //     Log.i(TAG, "Not registering child HDMI input " + tvInputInfo.getId());
            //     continue;
            // }

            Log.i(TAG, "Registering input source " + tvInputInfo.getId());

            TIFInputSource tifInputSource = new TIFInputSource(this, tvInputInfo);
            tifInputSources.add(tifInputSource);
            inputSources.add(tifInputSource);
        }
    }

    public Collection<InputSourceInterface> getInputSources() {
        return inputSources;
    }

    private String getCurrentInputSourceId() {
        return television.getSystemProperty("persist.sys.inputid");
    }

    public InputSourceInterface getActiveInput() {
        // TODO: check the current activity
        // If the current activity is org.droidtv.playtv, use the TvInputInfo with the inputid from SystemProperties

        String inputid = getCurrentInputSourceId();
        Log.i(TAG, "Current input source property " + inputid);

        for (TIFInputSource inputSource: tifInputSources) {
            // Log.i(TAG, "Checking input source ID " + inputSource.tvInputInfo.getId());
            if (!inputSource.tvInputInfo.getId().equals(inputid)) continue;

            Log.i(TAG, "Current input source name " + inputSource.getName());
            return inputSource;
        }

        Log.i(TAG, "Didn't find active input from ID " + inputid);

        for (InputSourceInterface inputSource: inputSources) {
            // Just return the first InputSource for now
            return inputSource;
        }

        return null;
    }

    public void setActiveInput(InputSourceInterface inputSource) {
        inputSource.activate();
    }

    public void onSubscribe(HomekitCharacteristicChangeCallback _callback) {
        callback = _callback;

        // TODO
    }

    public void onUnsubscribe() {
        callback = null;

        // TODO
    }
}
