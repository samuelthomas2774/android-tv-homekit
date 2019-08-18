package uk.org.fancy.AndroidTvHomeKit.characteristics;

import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.IntegerCharacteristic;
import io.github.hapjava.characteristics.EventableCharacteristic;
import io.github.hapjava.impl.ExceptionalConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.json.JsonObjectBuilder;

public class ActiveIdentifier extends IntegerCharacteristic implements EventableCharacteristic {
    private final Supplier<CompletableFuture<Integer>> getter;
    private final ExceptionalConsumer<Integer> setter;
    private final Consumer<HomekitCharacteristicChangeCallback> subscriber;
    private final Runnable unsubscriber;

    public ActiveIdentifier(
        Supplier<CompletableFuture<Integer>> getter,
        ExceptionalConsumer<Integer> setter,
        Consumer<HomekitCharacteristicChangeCallback> subscriber,
        Runnable unsubscriber
    ) {
        super("000000E7-0000-1000-8000-0026BB765291", true, true, "Active Identifier", 0, 0, "");

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
                .add("format", "uint32")
                .add("minStep", 1);
        });
    }

    @Override
    protected CompletableFuture<Integer> getValue() {
        return getter.get();
    }

    @Override
    protected void setValue(Integer value) throws Exception {
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
