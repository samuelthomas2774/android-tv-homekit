package uk.org.fancy.AndroidTvHomeKit.Philips;

import java.lang.reflect.Method;
import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.PowerStateInterface;
import uk.org.fancy.AndroidTvHomeKit.PollThread;
import uk.org.fancy.AndroidTvHomeKit.Philips.xtv.XTvHttp;
import io.github.hapjava.HomekitCharacteristicChangeCallback;

public class PowerState implements PowerStateInterface, PollThread.PollInterface {
    private static final String TAG = "HomeKit:PowerState";
    private final Television television;
    private HomekitCharacteristicChangeCallback callback = null;
    private boolean lastPowerState = false;

    public PowerState(Television _television) {
        television = _television;
    }

    public boolean getPowerState() {
        try {
            Class<?> contextClass = Class.forName("org.droidtv.tv.context.TvContext");
            Class<?> powerManagerInterface = Class.forName("org.droidtv.tv.tvpower.ITvPowerManager");

            Method getInterface = contextClass.getMethod("getInterface", Class.class);
            Object powerManagerInstance = getInterface.invoke(null, powerManagerInterface);
            Class<?> powerManagerClass = powerManagerInstance.getClass();

            Class<?> powerStatesEnum = Class.forName("org.droidtv.tv.tvpower.ITvPowerManager$PowerStates");

            Enum[] powerStates = (Enum[]) powerStatesEnum.getEnumConstants();
            Enum POWER_STATE_FULL_SYSTEM_START = null;

            for (Enum powerState: powerStates) {
                if (powerState.name() == "POWER_STATE_FULL_SYSTEM_START") {
                    POWER_STATE_FULL_SYSTEM_START = powerState;
                    break;
                }
            }

            if (POWER_STATE_FULL_SYSTEM_START == null) {
                throw new Exception("Didn\'t find PowerStates.POWER_STATE_FULL_SYSTEM_START");
            }

            Method getPowerState = powerManagerClass.getMethod("GetPowerState");
            Enum powerState = (Enum) getPowerState.invoke(powerManagerInstance);

            return POWER_STATE_FULL_SYSTEM_START.equals(powerState);
        } catch (Exception e) {
            Log.i(TAG, "Error getting power state " + e.toString());
            return false;
        }
    }

    public void setPowerState(boolean on) {
        Log.i(TAG, "Set power state " + on);

        television.xtvhttp.setPowerState(on ? XTvHttp.PowerState.ON : XTvHttp.PowerState.STANDBY);
    }

    public void onSubscribe(HomekitCharacteristicChangeCallback _callback) {
        callback = _callback;
        lastPowerState = getPowerState();
        television.service.pollThread.add(this);
    }

    public void onUnsubscribe() {
        television.service.pollThread.remove(this);
        callback = null;
    }

    public void poll() {
        boolean powerState = getPowerState();
        Log.i(TAG, "Polling power state " + powerState + "; was " + lastPowerState);

        if (powerState != lastPowerState) {
            callback.changed();

            lastPowerState = powerState;
        }
    }
}
