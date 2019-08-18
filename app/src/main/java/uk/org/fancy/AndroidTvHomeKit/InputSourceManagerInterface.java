package uk.org.fancy.AndroidTvHomeKit;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import io.github.hapjava.HomekitCharacteristicChangeCallback;

public interface InputSourceManagerInterface {
    public Collection<InputSourceInterface> getInputSources();

    public CompletableFuture<InputSourceInterface> getActiveInput();
    public void setActiveInput(InputSourceInterface inputSource);

    public void onSubscribe(HomekitCharacteristicChangeCallback callback);
    public void onUnsubscribe();
}
