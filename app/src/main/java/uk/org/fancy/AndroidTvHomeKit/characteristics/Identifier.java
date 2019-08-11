package uk.org.fancy.AndroidTvHomeKit.characteristics;

import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.IntegerCharacteristic;
import io.github.hapjava.impl.ExceptionalConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.json.JsonObjectBuilder;

public class Identifier extends IntegerCharacteristic {
    private final int value;

    public Identifier(int _value) {
        super("000000E6-0000-1000-8000-0026BB765291", false, true, "Identifier", 0, 0, "");

        value = _value;
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
        return CompletableFuture.completedFuture(value);
    }

    @Override
    protected void setValue(Integer value) throws Exception {
        throw new Exception("Read only characteristic");
    }
}
