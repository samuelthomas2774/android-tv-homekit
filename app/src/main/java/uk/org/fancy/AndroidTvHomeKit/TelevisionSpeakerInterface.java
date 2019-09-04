package uk.org.fancy.AndroidTvHomeKit;

import java.util.concurrent.CompletableFuture;
import io.github.hapjava.HomekitCharacteristicChangeCallback;

public interface TelevisionSpeakerInterface {
    public interface RelativeVolumeInterface extends TelevisionSpeakerInterface {
        public void incrementVolume();
        public void decrementVolume();
    }

    public interface AbsoluteVolumeInterface extends TelevisionSpeakerInterface {
        /**
         * Returns the volume as an integer between 0 and 100.
         */
        public CompletableFuture<Integer> getVolume();

        /**
         * Sets the volume to an integer between 0 and 100.
         */
        public void setVolume(int volume);

        public void onVolumeSubscribe(HomekitCharacteristicChangeCallback callback);
        public void onVolumeUnsubscribe();

        // default public void incrementVolume() {
        //     setVolume(getVolume() + 1);
        // }
        //
        // default public void decrementVolume() {
        //     setVolume(getVolume() + 1);
        // }
    }

    public CompletableFuture<Boolean> getMute();
    public void setMute(boolean mute);

    public void onMuteSubscribe(HomekitCharacteristicChangeCallback callback);
    public void onMuteUnsubscribe();

    public enum VolumeControlType {
        NONE,
        RELATIVE,
        RELATIVE_WITH_CURRENT,
        ABSOLUTE,
    }

    public VolumeControlType getVolumeControlType();
}
