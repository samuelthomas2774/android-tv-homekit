package uk.org.fancy.AndroidTvHomeKit.characteristics;

import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.StringCharacteristic;
import io.github.hapjava.characteristics.EventableCharacteristic;
import io.github.hapjava.impl.ExceptionalConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfiguredName extends StringCharacteristic implements EventableCharacteristic {
    private final Supplier<CompletableFuture<String>> getter;
    private final ExceptionalConsumer<String> setter;
    private final Consumer<HomekitCharacteristicChangeCallback> subscriber;
    private final Runnable unsubscriber;

    public ConfiguredName(
        Supplier<CompletableFuture<String>> getter,
        ExceptionalConsumer<String> setter,
        Consumer<HomekitCharacteristicChangeCallback> subscriber,
        Runnable unsubscriber
    ) {
        super("000000E3-0000-1000-8000-0026BB765291", "Configured Name");

        this.getter = getter;
        this.setter = setter;
        this.subscriber = subscriber;
        this.unsubscriber = unsubscriber;
    }

    @Override
    protected CompletableFuture<String> getValue() {
        return getter.get();
    }

    @Override
    protected void setValue(String value) throws Exception {
        setter.accept(value);
    }

    @Override
    public void subscribe(HomekitCharacteristicChangeCallback callback) {
        subscriber.accept(callback);
    }

    @Override
    public void unsubscribe() {
        unsubscriber.run();
    }
}
