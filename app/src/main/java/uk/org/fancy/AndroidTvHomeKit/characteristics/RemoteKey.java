package uk.org.fancy.AndroidTvHomeKit.characteristics;

import io.github.hapjava.characteristics.EnumCharacteristic;
import io.github.hapjava.impl.ExceptionalConsumer;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class RemoteKey extends EnumCharacteristic {
    public static final int REWIND = 0;
    public static final int FAST_FORWARD = 1;
    public static final int NEXT_TRACK = 2;
    public static final int PREVIOUS_TRACK = 3;
    public static final int ARROW_UP = 4;
    public static final int ARROW_DOWN = 5;
    public static final int ARROW_LEFT = 6;
    public static final int ARROW_RIGHT = 7;
    public static final int SELECT = 8;
    public static final int BACK = 9;
    public static final int EXIT = 10;
    public static final int PLAY_PAUSE = 11;
    public static final int INFORMATION = 15;

    private final ExceptionalConsumer<Integer> setter;

    public RemoteKey(ExceptionalConsumer<Integer> setter) {
        super("000000E1-0000-1000-8000-0026BB765291", true, false, "Remote Key", 16);

        this.setter = setter;
    }

    /** {@inheritDoc} */
    @Override
    protected CompletableFuture<JsonObjectBuilder> makeBuilder(int iid) {
        return super.makeBuilder(iid).thenApply(builder -> {
            return builder
                .add("format", "uint8")
                .add("minValue", 0)
                .add("minStep", 1)
                .add("validValues", Json.createArrayBuilder()
                    .add(0).add(1).add(2).add(3).add(4).add(5).add(6).add(7).add(8).add(9)
                    .add(10).add(11)
                    // .add(12).add(13).add(14)
                    .add(15)
                    // .add(16)
                    .build());
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
