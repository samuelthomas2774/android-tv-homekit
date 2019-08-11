package uk.org.fancy.AndroidTvHomeKit;

import io.github.hapjava.HomekitCharacteristicChangeCallback;

public interface InputSourceInterface {
    public String getName();

    public enum InputSourceTypes {
        OTHER,
        HOME_SCREEN,
        TUNER,
        HDMI,
        COMPOSITE_VIDEO,
        S_VIDEO,
        COMPONENT_VIDEO,
        DVI,
        AIRPLAY,
        USB,
        APPLICATION,
    }

    public InputSourceTypes getInputSourceType();

    public boolean isConfigured();
    public boolean getCurrentVisibilityState();

    public String getConfiguredName();
    public void setConfiguredName(String name);

    public enum InputDeviceTypes {
        OTHER,
        TV,
        RECORDING,
        TUNER,
        PLAYBACK,
        AUDIO_SYSTEM,
    }

    /**
     * Set the current source to this.
     */
    public void activate();
}
