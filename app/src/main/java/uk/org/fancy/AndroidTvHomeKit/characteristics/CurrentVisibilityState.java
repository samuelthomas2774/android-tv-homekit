package uk.org.fancy.AndroidTvHomeKit.characteristics;

import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.EnumCharacteristic;
import io.github.hapjava.characteristics.EventableCharacteristic;
import io.github.hapjava.impl.ExceptionalConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.json.JsonObjectBuilder;

public class CurrentVisibilityState extends EnumCharacteristic implements EventableCharacteristic {
    public static final int SHOWN = 0;
    public static final int HIDDEN = 1;

    private final Supplier<CompletableFuture<Integer>> getter;
    private final Consumer<HomekitCharacteristicChangeCallback> subscriber;
    private final Runnable unsubscriber;

    public CurrentVisibilityState(
        Supplier<CompletableFuture<Integer>> getter,
        Consumer<HomekitCharacteristicChangeCallback> subscriber,
        Runnable unsubscriber
    ) {
        super("00000135-0000-1000-8000-0026BB765291", false, true, "Current Visibility State", 1);

        this.getter = getter;
        this.subscriber = subscriber;
        this.unsubscriber = unsubscriber;
    }

    public CurrentVisibilityState(int value) {
        this(() -> CompletableFuture.completedFuture(value), (s) -> {}, () -> {});
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
    protected CompletableFuture<Integer> getValue() {
        return getter.get();
    }

    @Override
    protected void setValue(Integer value) throws Exception {
        throw new Exception("Read only characteristic");
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
