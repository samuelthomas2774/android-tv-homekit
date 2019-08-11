package uk.org.fancy.AndroidTvHomeKit;

import java.util.Collection;
import io.github.hapjava.HomekitCharacteristicChangeCallback;

public interface InputSourceManagerInterface {
    public Collection<InputSourceInterface> getInputSources();

    public InputSourceInterface getActiveInput();
    public void setActiveInput(InputSourceInterface inputSource);

    public void onSubscribe(HomekitCharacteristicChangeCallback callback);
    public void onUnsubscribe();
}
