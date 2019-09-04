package uk.org.fancy.AndroidTvHomeKit.characteristics;

import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.BooleanCharacteristic;
import io.github.hapjava.characteristics.EventableCharacteristic;
import io.github.hapjava.impl.ExceptionalConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.json.JsonObjectBuilder;

public class Mute extends BooleanCharacteristic implements EventableCharacteristic {
    private final Supplier<CompletableFuture<Boolean>> getter;
    private final ExceptionalConsumer<Boolean> setter;
    private final Consumer<HomekitCharacteristicChangeCallback> subscriber;
    private final Runnable unsubscriber;

    public Mute(
        Supplier<CompletableFuture<Boolean>> getter,
        ExceptionalConsumer<Boolean> setter,
        Consumer<HomekitCharacteristicChangeCallback> subscriber,
        Runnable unsubscriber
    ) {
        super("0000011A-0000-1000-8000-0026BB765291", true, true, "Mute");

        this.getter = getter;
        this.setter = setter;
        this.subscriber = subscriber;
        this.unsubscriber = unsubscriber;
    }

    /** {@inheritDoc} */
    @Override
    protected CompletableFuture<JsonObjectBuilder> makeBuilder(int iid) {
        return super.makeBuilder(iid).thenApply(builder -> {
            return builder
                .add("minValue", 0)
                .add("minStep", 1);
        });
    }

    @Override
    protected CompletableFuture<Boolean> getValue() {
        return getter.get();
    }

    // @Override
    protected void setValue(Boolean value) throws Exception {
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
