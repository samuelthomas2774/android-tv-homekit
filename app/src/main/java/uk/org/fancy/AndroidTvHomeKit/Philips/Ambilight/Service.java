package uk.org.fancy.AndroidTvHomeKit.Philips.Ambilight;

import uk.org.fancy.AndroidTvHomeKit.Philips.Television;
import uk.org.fancy.AndroidTvHomeKit.Philips.xtv.XTvAmbilightHttp;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import android.util.Log;
import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.Characteristic;
import io.github.hapjava.impl.characteristics.common.Name;
import io.github.hapjava.impl.characteristics.common.PowerStateCharacteristic;
import io.github.hapjava.impl.characteristics.lightbulb.BrightnessCharacteristic;
import io.github.hapjava.impl.characteristics.lightbulb.HueCharacteristic;
import io.github.hapjava.impl.characteristics.lightbulb.SaturationCharacteristic;

public class Service implements io.github.hapjava.Service {
    private static final String TAG = "HomeKit:Ambilight.Service";
    private final Television television;
    private final XTvAmbilightHttp xtvambilight;
    private boolean lastTvPowerState = false;
    private boolean hasEnabledAmbilightAfterTvOff = false;
    private XTvAmbilightHttp.Configuration lastEnabledConfiguration =
        new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.Style.FOLLOW_VIDEO);
    private CompletableFuture<XTvAmbilightHttp.Configuration> lastConfigurationFuture = null;
    private long lastConfigurationTimestamp = 0;
    private XTvAmbilightHttp.Colour lastColour = new XTvAmbilightHttp.Colour(0, 0, 0);
    private XTvAmbilightHttp.Colour lastColourDelta = new XTvAmbilightHttp.Colour(0, 0, 0);
    private int lastColourSpeed = 0;
    private int lastColourSyncState = ColourSync.OFF;
    private HomekitCharacteristicChangeCallback powerStateCallback;
    private HomekitCharacteristicChangeCallback colourSyncCallback;
    private HomekitCharacteristicChangeCallback brightnessCallback;
    private HomekitCharacteristicChangeCallback hueCallback;
    private HomekitCharacteristicChangeCallback saturationCallback;
    private HomekitCharacteristicChangeCallback variationCallback;
    private HomekitCharacteristicChangeCallback variationSpeedCallback;

    public Service(Television television) {
        this.television = television;
        xtvambilight = television.xtvhttp.ambilight;
    }
  
    public String getType() {
        return "00000043-0000-1000-8000-0026BB765291"; // Lightbulb
    }

    public List<Characteristic> getCharacteristics() {
        List<Characteristic> characteristics = new LinkedList<>();

        // Name
        Characteristic name = new Name("Ambilight");
        characteristics.add(name);

        // Power
        Characteristic power = new PowerStateCharacteristic(
            () -> getPowerState(), v -> setPowerState(v),
            c -> {powerStateCallback = c; onSubscribe();},
            () -> {powerStateCallback = null; onUnsubscribe();}
        );
        characteristics.add(power);

        // Colour Sync
        Characteristic colourSync = new ColourSync(
            () -> getColourSync(), v -> setColourSync(v),
            c -> {colourSyncCallback = c; onSubscribe();},
            () -> {colourSyncCallback = null; onUnsubscribe();}
        );
        characteristics.add(colourSync);

        // Brightness
        Characteristic brightness = new BrightnessCharacteristic(
            () -> getBrightness(), v -> setBrightness(v),
            c -> {brightnessCallback = c; onSubscribe();},
            () -> {brightnessCallback = null; onUnsubscribe();}
        );
        characteristics.add(brightness);

        // Hue
        Characteristic hue = new HueCharacteristic(
            () -> getHue(), v -> setHue(v),
            c -> {hueCallback = c; onSubscribe();},
            () -> {hueCallback = null; onUnsubscribe();}
        );
        characteristics.add(hue);

        // Saturation
        Characteristic saturation = new SaturationCharacteristic(
            () -> getSaturation(), v -> setSaturation(v),
            c -> {saturationCallback = c; onSubscribe();},
            () -> {saturationCallback = null; onUnsubscribe();}
        );
        characteristics.add(saturation);

        // Colour Variation
        Characteristic variation = new Variation(
            () -> getVariation(), v -> setVariation(v),
            c -> {variationCallback = c; onSubscribe();},
            () -> {variationCallback = null; onUnsubscribe();}
        );
        characteristics.add(variation);

        // Colour Variation Speed
        Characteristic variationSpeed = new VariationSpeed(
            () -> getVariationSpeed(), v -> setVariationSpeed(v),
            c -> {variationSpeedCallback = c; onSubscribe();},
            () -> {variationSpeedCallback = null; onUnsubscribe();}
        );
        characteristics.add(variationSpeed);

        return Collections.unmodifiableList(characteristics);
    }

    public List<io.github.hapjava.Service> getLinkedServices() {
        return Collections.emptyList();
    }

    private boolean getTvPowerState() {
        return television.getPowerStateManager().getPowerState();
    }

    private CompletableFuture<XTvAmbilightHttp.Configuration> getConfiguration() {
        long now = System.currentTimeMillis();
        if (now <= (lastConfigurationTimestamp + 2000) && lastConfigurationFuture != null) {
            return lastConfigurationFuture;
        }

        lastConfigurationTimestamp = now;
        return lastConfigurationFuture = xtvambilight.getConfiguration().thenApply(configuration -> {
            if (configuration.style != XTvAmbilightHttp.Style.OFF) {
                lastEnabledConfiguration = configuration;
            }

            if (configuration.style == XTvAmbilightHttp.Style.FOLLOW_COLOUR && configuration.isExpert) {
                lastColour = configuration.manualColour;
                lastColourDelta = configuration.manualColourDelta;
                // lastColourSpeed = configuration.speed;
            }

            return configuration;
        });
    }

    private CompletableFuture<Object> setConfiguration(XTvAmbilightHttp.Configuration configuration) {
        if (!getTvPowerState() && configuration.style != XTvAmbilightHttp.Style.OFF) {
            hasEnabledAmbilightAfterTvOff = true;
        } else {
            hasEnabledAmbilightAfterTvOff = false;
        }

        if (configuration.style != XTvAmbilightHttp.Style.OFF) {
            lastEnabledConfiguration = configuration;
        }

        if (configuration.style == XTvAmbilightHttp.Style.FOLLOW_COLOUR && configuration.isExpert) {
            lastColour = configuration.manualColour;
            lastColourDelta = configuration.manualColourDelta;
            // lastColourSpeed = configuration.speed;
        }

        return xtvambilight.setConfiguration(configuration);
    }
    private CompletableFuture<Object> setConfiguration(
        XTvAmbilightHttp.Colour colour, XTvAmbilightHttp.Colour delta, int speed
    ) {
        return setConfiguration(new XTvAmbilightHttp.Configuration(colour, delta, speed));
    }

    public CompletableFuture<Boolean> getPowerState() {
        return getConfiguration().thenApply(configuration -> {
            if (getTvPowerState()) {
                return configuration.style != XTvAmbilightHttp.Style.OFF;
            } else {
                return hasEnabledAmbilightAfterTvOff = true;
            }
        });
    }

    public void setPowerState(boolean on) {
        if (on) {
            // Turn Ambilight on
            setConfiguration(lastEnabledConfiguration);
        } else {
            if (getTvPowerState()) {
                // Turn Ambilight off, TV is on
                xtvambilight.setConfiguration(XTvAmbilightHttp.Style.OFF);
            } else {
                // Turn Ambilight off, TV is off
                // Reset to internal mode, which will turn Ambilight off but won't disable it
                xtvambilight.setMode(XTvAmbilightHttp.Mode.INTERNAL);
            }

            hasEnabledAmbilightAfterTvOff = false;
        }
    }

    public CompletableFuture<Integer> getColourSync() {
        return getConfiguration().thenApply(configuration -> {
            if (configuration.style == XTvAmbilightHttp.Style.FOLLOW_VIDEO) {
                switch (configuration.videoAlgorithm) {
                    case STANDARD: return ColourSync.VIDEO_STANDARD;
                    case NATURAL: return ColourSync.VIDEO_NATURAL;
                    case IMMERSIVE: return ColourSync.VIDEO_IMMERSIVE;
                    case VIVID: return ColourSync.VIDEO_VIVID;
                    case GAME: return ColourSync.VIDEO_GAME;
                    case COMFORT: return ColourSync.VIDEO_COMFORT;
                    case RELAX: return ColourSync.VIDEO_RELAX;
                }
            }

            if (configuration.style == XTvAmbilightHttp.Style.FOLLOW_AUDIO) {
                switch (configuration.audioAlgorithm) {
                    case ENERGY_ADAPTIVE_BRIGHTNESS: return ColourSync.AUDIO_ENERGY_ADAPTIVE_BRIGHTNESS;
                    case ENERGY_ADAPTIVE_COLORS: return ColourSync.AUDIO_ENERGY_ADAPTIVE_COLORS;
                    case VU_METER: return ColourSync.AUDIO_VU_METER;
                    case SPECTRUM_ANALYSER: return ColourSync.AUDIO_SPECTRUM_ANALYSER;
                    case KNIGHT_RIDER_ALTERNATING: return ColourSync.AUDIO_KNIGHT_RIDER_ALTERNATING;
                    case RANDOM_PIXEL_FLASH: return ColourSync.AUDIO_RANDOM_PIXEL_FLASH;
                    case MODE_RANDOM: return ColourSync.AUDIO_RANDOM;
                }
            }

            if (configuration.style == XTvAmbilightHttp.Style.FOLLOW_COLOUR && !configuration.isExpert) {
                switch (configuration.colour) {
                    case HOT_LAVA: return ColourSync.COLOUR_HOT_LAVA;
                    case DEEP_WATER: return ColourSync.COLOUR_DEEP_WATER;
                    case FRESH_NATURE: return ColourSync.COLOUR_FRESH_NATURE;
                    case ISF: return ColourSync.COLOUR_ISF;
                    case PTA_LOUNGE: return ColourSync.COLOUR_PTA_LOUNGE;
                }
            }

            if (configuration.style == XTvAmbilightHttp.Style.FOLLOW_COLOUR && configuration.isExpert) {
                return ColourSync.OFF;
            }
            
            return lastColourSyncState;
        }).thenApply(syncstate -> {
            return lastColourSyncState = syncstate;
        });
    }

    public void setColourSync(int syncstate) {
        XTvAmbilightHttp.Configuration configuration;

        switch (syncstate) {
            default:
                // Invalid value
                return;
            case ColourSync.OFF:
                Log.i(TAG, "Disabling colour sync");
                configuration = new XTvAmbilightHttp.Configuration(lastColour, lastColourDelta, lastColourSpeed); break;

            case ColourSync.VIDEO_STANDARD:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.VideoAlgorithm.STANDARD); break;
            case ColourSync.VIDEO_NATURAL:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.VideoAlgorithm.NATURAL); break;
            case ColourSync.VIDEO_IMMERSIVE:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.VideoAlgorithm.IMMERSIVE); break;
            case ColourSync.VIDEO_VIVID:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.VideoAlgorithm.VIVID); break;
            case ColourSync.VIDEO_GAME:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.VideoAlgorithm.GAME); break;
            case ColourSync.VIDEO_COMFORT:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.VideoAlgorithm.COMFORT); break;
            case ColourSync.VIDEO_RELAX:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.VideoAlgorithm.RELAX); break;

            case ColourSync.AUDIO_ENERGY_ADAPTIVE_BRIGHTNESS: configuration =
                new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.AudioAlgorithm.ENERGY_ADAPTIVE_BRIGHTNESS); break;
            case ColourSync.AUDIO_ENERGY_ADAPTIVE_COLORS: configuration =
                new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.AudioAlgorithm.ENERGY_ADAPTIVE_COLORS); break;
            case ColourSync.AUDIO_VU_METER:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.AudioAlgorithm.VU_METER); break;
            case ColourSync.AUDIO_SPECTRUM_ANALYSER: configuration =
                new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.AudioAlgorithm.SPECTRUM_ANALYSER); break;
            case ColourSync.AUDIO_KNIGHT_RIDER_ALTERNATING: configuration =
                new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.AudioAlgorithm.KNIGHT_RIDER_ALTERNATING); break;
            case ColourSync.AUDIO_RANDOM_PIXEL_FLASH: configuration =
                new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.AudioAlgorithm.RANDOM_PIXEL_FLASH); break;
            case ColourSync.AUDIO_RANDOM:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.AudioAlgorithm.MODE_RANDOM); break;

            case ColourSync.COLOUR_HOT_LAVA:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.ColourPreset.HOT_LAVA); break;
            case ColourSync.COLOUR_DEEP_WATER:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.ColourPreset.DEEP_WATER); break;
            case ColourSync.COLOUR_FRESH_NATURE:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.ColourPreset.FRESH_NATURE); break;
            case ColourSync.COLOUR_ISF:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.ColourPreset.ISF); break;
            case ColourSync.COLOUR_PTA_LOUNGE:
                configuration = new XTvAmbilightHttp.Configuration(XTvAmbilightHttp.ColourPreset.PTA_LOUNGE); break;
        }

        setConfiguration(configuration);
    }

    public int range(int value, int frommin, int frommax, int tomin, int tomax) {
        return range(value - frommin, frommax, tomax) + tomin;
    }
    public int range(int value, int frommax, int tomax) {
        // 50, 100, 1000 -> 500
        // 50 * (1000 / 100), 50 * 10
        // return value * (tomax / frommax);
        // (50 * 1000) / 100, 50000 / 100
        return (value * tomax) / frommax;
    }

    public CompletableFuture<Integer> getBrightness() {
        return getConfiguration().thenApply(configuration -> {
            // Max is still 255, but 120 seems to be the maximum brightness
            int brightness = range(lastColour.brightness, 120, 100);
            return brightness > 100 ? 100 : brightness;
        });
    }

    public void setBrightness(int brightness) {
        Log.i(TAG, "Setting Ambilight brightness " + Integer.toString(brightness));
        lastColour = new XTvAmbilightHttp.Colour(lastColour.hue, lastColour.saturation, range(brightness, 100, 120));
        setConfiguration(lastColour, lastColourDelta, lastColourSpeed);

        if (brightnessCallback != null) brightnessCallback.changed();
    }

    public CompletableFuture<Double> getHue() {
        return getConfiguration().thenApply(configuration -> {
            return (double) range(lastColour.hue, 255, 360);
        });
    }

    public void setHue(double hue) {
        Log.i(TAG, "Setting Ambilight hue " + Double.toString(hue));
        lastColour = new XTvAmbilightHttp.Colour(range((int) hue, 360, 255), lastColour.saturation, lastColour.brightness);
        setConfiguration(lastColour, lastColourDelta, lastColourSpeed);

        if (hueCallback != null) hueCallback.changed();
    }

    public CompletableFuture<Double> getSaturation() {
        return getConfiguration().thenApply(configuration -> {
            return (double) range(lastColour.saturation, 255, 100);
        });
    }

    public void setSaturation(double saturation) {
        Log.i(TAG, "Setting Ambilight saturation " + Double.toString(saturation));
        lastColour = new XTvAmbilightHttp.Colour(lastColour.hue, range((int) saturation, 100, 255), lastColour.brightness);
        setConfiguration(lastColour, lastColourDelta, lastColourSpeed);

        if (saturationCallback != null) saturationCallback.changed();
    }

    public CompletableFuture<Integer> getVariation() {
        return getConfiguration().thenApply(configuration -> {
            return (lastColourDelta.hue + lastColourDelta.saturation + lastColourDelta.brightness) / 3;
        });
    }

    public void setVariation(int variation) {
        Log.i(TAG, "Setting Ambilight colour variation " + Integer.toString(variation));
        lastColourDelta = new XTvAmbilightHttp.Colour(variation, variation, variation);
        setConfiguration(lastColour, lastColourDelta, lastColourSpeed);

        if (variationCallback != null) variationCallback.changed();
    }

    public CompletableFuture<Integer> getVariationSpeed() {
        return getConfiguration().thenApply(configuration -> {
            return lastColourSpeed;
        });
    }

    public void setVariationSpeed(int variation) {
        Log.i(TAG, "Setting Ambilight colour variation speed " + Integer.toString(variation));
        lastColourSpeed = variation;
        setConfiguration(lastColour, lastColourDelta, lastColourSpeed);

        if (variationSpeedCallback != null) variationSpeedCallback.changed();
    }

    private void onSubscribe() {
        //
    }

    private void onUnsubscribe() {
        //
    }
}
