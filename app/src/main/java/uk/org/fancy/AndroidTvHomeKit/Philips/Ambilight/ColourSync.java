package uk.org.fancy.AndroidTvHomeKit.Philips.Ambilight;

import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.EnumCharacteristic;
import io.github.hapjava.characteristics.EventableCharacteristic;
import io.github.hapjava.impl.ExceptionalConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.json.JsonObjectBuilder;

public class ColourSync extends EnumCharacteristic implements EventableCharacteristic {
    public static final int OFF = 0; // Not syncing

    public static final int VIDEO_STANDARD = 1; // Follow video, Standard
    public static final int VIDEO_NATURAL = 2; // Follow video, Natural
    public static final int VIDEO_IMMERSIVE = 3; // Follow video, Immersive/Football
    public static final int VIDEO_VIVID = 4; // Follow video, Vivid
    public static final int VIDEO_GAME = 5; // Follow video, Standard
    public static final int VIDEO_COMFORT = 6; // Follow video, Comfort
    public static final int VIDEO_RELAX = 7; // Follow video, Relax

    public static final int AUDIO_ENERGY_ADAPTIVE_BRIGHTNESS = 8; // Follow audio, Lumina
    public static final int AUDIO_ENERGY_ADAPTIVE_COLORS = 9; // Follow audio, Colora
    public static final int AUDIO_VU_METER = 10; // Follow audio, Retro
    public static final int AUDIO_SPECTRUM_ANALYSER = 11; // Follow audio, Spectrum
    public static final int AUDIO_KNIGHT_RIDER_ALTERNATING = 12; // Follow audio, Scanner
    public static final int AUDIO_RANDOM_PIXEL_FLASH = 13; // Follow audio, Rhythm
    public static final int AUDIO_RANDOM = 14; // Follow audio, Party

    public static final int COLOUR_HOT_LAVA = 15; // Follow colour, Hot lava
    public static final int COLOUR_DEEP_WATER = 16; // Follow colour, Deep water
    public static final int COLOUR_FRESH_NATURE = 17; // Follow colour, Fresh nature
    public static final int COLOUR_ISF = 18; // Follow colour, Warm white
    public static final int COLOUR_PTA_LOUNGE = 19; // Follow colour, Cool white

    private final Supplier<CompletableFuture<Integer>> getter;
    private final ExceptionalConsumer<Integer> setter;
    private final Consumer<HomekitCharacteristicChangeCallback> subscriber;
    private final Runnable unsubscriber;

    public ColourSync(
        Supplier<CompletableFuture<Integer>> getter,
        ExceptionalConsumer<Integer> setter,
        Consumer<HomekitCharacteristicChangeCallback> subscriber,
        Runnable unsubscriber
    ) {
        super("CA7897C4-4B46-448D-AF68-29C686B730F4", true, true, "Ambilight Sync", 19);

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
