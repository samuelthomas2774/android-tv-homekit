package uk.org.fancy.AndroidTvHomeKit;

import io.github.hapjava.HomekitCharacteristicChangeCallback;

public interface PowerStateInterface {
    public boolean getPowerState();
    public void setPowerState(boolean on);

    public void onSubscribe(HomekitCharacteristicChangeCallback callback);
    public void onUnsubscribe();
}
