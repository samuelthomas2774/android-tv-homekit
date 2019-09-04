package uk.org.fancy.AndroidTvHomeKit.characteristics;

import io.github.hapjava.characteristics.EnumCharacteristic;
import io.github.hapjava.impl.ExceptionalConsumer;
import java.util.concurrent.CompletableFuture;
import javax.json.JsonObjectBuilder;

public class VolumeSelector extends EnumCharacteristic {
    public static final int INCREMENT = 0;
    public static final int DECREMENT = 1;

    private final ExceptionalConsumer<Integer> setter;

    public VolumeSelector(ExceptionalConsumer<Integer> setter) {
        super("000000EA-0000-1000-8000-0026BB765291", true, false, "Volume Selector", 1);

        this.setter = setter;
    }

    /** {@inheritDoc} */
    @Override
    protected CompletableFuture<JsonObjectBuilder> makeBuilder(int iid) {
        return super.makeBuilder(iid).thenApply(builder -> {
            return builder
                .add("format", "uint8")
                .add("minValue", 0)
                .add("minStep", 1);
        });
    }

    @Override
    protected CompletableFuture<Integer> getValue() {
        throw new RuntimeException("Write only characteristic");
    }

    @Override
    protected void setValue(Integer value) throws Exception {
        setter.accept(value);
    }
}
