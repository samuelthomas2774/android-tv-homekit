package uk.org.fancy.AndroidTvHomeKit.Philips;

import java.util.concurrent.CompletableFuture;
import android.util.Log;
import uk.org.fancy.AndroidTvHomeKit.TelevisionSpeakerInterface;
import uk.org.fancy.AndroidTvHomeKit.Philips.xtv.XTvHttp;
import io.github.hapjava.HomekitCharacteristicChangeCallback;

public class TelevisionSpeaker implements TelevisionSpeakerInterface,
        TelevisionSpeakerInterface.RelativeVolumeInterface,
        TelevisionSpeakerInterface.AbsoluteVolumeInterface {
    private static final String TAG = "HomeKit:TelevisionSpeaker";
    private final Television television;
    private HomekitCharacteristicChangeCallback muteCallback = null;
    private HomekitCharacteristicChangeCallback volumeCallback = null;
    private int MINIMUM_VOLUME = 0;
    private int MAXIMUM_VOLUME = 60;

    public TelevisionSpeaker(Television _television) {
        television = _television;
    }

    public CompletableFuture<Boolean> getMute() {
        return television.xtvhttp.getMute();
    }

    public void setMute(boolean mute) {
        Log.i(TAG, "Set mute: " + (mute ? "true" : "false"));
        television.xtvhttp.setMute(mute);
    }

    public void onMuteSubscribe(HomekitCharacteristicChangeCallback callback) {
        muteCallback = callback;
    }

    public void onMuteUnsubscribe() {
        muteCallback = null;
    }

    public VolumeControlType getVolumeControlType() {
        return VolumeControlType.ABSOLUTE;
    }

    public void incrementVolume() {
        television.keypress(XTvHttp.RemoteKey.VOLUME_UP);
    }

    public void decrementVolume() {
        television.keypress(XTvHttp.RemoteKey.VOLUME_DOWN);
    }

    /**
     * Returns the volume as an integer between 0 and 100.
     */
    public CompletableFuture<Integer> getVolume() {
        return television.xtvhttp.getAudioSettings().thenApply(audio -> {
            MINIMUM_VOLUME = audio.MINIMUM_VOLUME;
            MAXIMUM_VOLUME = audio.MAXIMUM_VOLUME;

            int v = (int) (((((float) audio.volume - audio.MINIMUM_VOLUME) / audio.MAXIMUM_VOLUME) * 100) + audio.MINIMUM_VOLUME);

            Log.i(TAG, "Current volume: " + Integer.toString(audio.volume) + "; " + Integer.toString(v) +
                "; min " + Integer.toString(audio.MINIMUM_VOLUME) + "; max " + Integer.toString(audio.MAXIMUM_VOLUME));
            return v;
        });
    }

    /**
     * Sets the volume to an integer between 0 and 100.
     */
    public void setVolume(int volume) {
        int v = (int) (((((float) volume - MINIMUM_VOLUME) / 100) * MAXIMUM_VOLUME) + MINIMUM_VOLUME);
        Log.i(TAG, "Set volume: " + Integer.toString(volume) + "; " + Integer.toString(v));
        television.xtvhttp.setVolume(v);
    }

    public void onVolumeSubscribe(HomekitCharacteristicChangeCallback callback) {
        volumeCallback = callback;
    }

    public void onVolumeUnsubscribe() {
        volumeCallback = null;
    }
}
