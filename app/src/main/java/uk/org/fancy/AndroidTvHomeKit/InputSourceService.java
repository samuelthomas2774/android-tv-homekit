package uk.org.fancy.AndroidTvHomeKit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import android.util.Log;

import uk.org.fancy.AndroidTvHomeKit.characteristics.Identifier;
import uk.org.fancy.AndroidTvHomeKit.characteristics.ConfiguredName;
import uk.org.fancy.AndroidTvHomeKit.characteristics.InputSourceType;
import uk.org.fancy.AndroidTvHomeKit.characteristics.IsConfigured;
import uk.org.fancy.AndroidTvHomeKit.characteristics.CurrentVisibilityState;
import uk.org.fancy.AndroidTvHomeKit.characteristics.TIFID;
import io.github.hapjava.Service;
import io.github.hapjava.characteristics.Characteristic;
import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.impl.characteristics.common.Name;

public class InputSourceService implements Service {
    public final TelevisionAccessory accessory;
    public final InputSourceInterface inputSource;
    private final int identifier;
    private TelevisionService televisionService;
    private HomekitCharacteristicChangeCallback configuredNameCallback;
    private HomekitCharacteristicChangeCallback isConfiguredCallback;

    public InputSourceService(TelevisionAccessory _accessory, InputSourceInterface _inputSource, int _identifier) {
        accessory = _accessory;
        inputSource = _inputSource;
        identifier = _identifier;
    }

    public void setTelevisionService(TelevisionService television) {
        televisionService = television;
    }

    public String getType() {
        return "000000D9-0000-1000-8000-0026BB765291";
    }

    public List<Characteristic> getCharacteristics() {
        List<Characteristic> characteristics = new LinkedList<>();

        if (inputSource instanceof TIFInputSourceInterface) {
            Characteristic tifid = new TIFID(((TIFInputSourceInterface) inputSource).getId());
            characteristics.add(tifid);
        }

        // Name
        Characteristic name = new Name(inputSource.getName());
        characteristics.add(name);

        // Identifier
        Characteristic identifier = new Identifier(this.identifier);
        characteristics.add(identifier);

        // Configured Name
        Characteristic configuredName = new ConfiguredName(
            () -> getConfiguredName(),
            v -> setConfiguredName(v),
            c -> configuredNameCallback = c,
            () -> configuredNameCallback = null
        );
        characteristics.add(configuredName);

        // Input Source Type
        Characteristic inputSourceType = new InputSourceType(getInputSourceType());
        characteristics.add(inputSourceType);

        // Is Configured
        Characteristic isConfigured = new IsConfigured(
            () -> CompletableFuture.completedFuture(IsConfigured.CONFIGURED),
            v -> {
                Log.i("HomeKit:InputSourceService", "Set IsConfigured characteristic to " + v);
            },
            c -> isConfiguredCallback = c,
            () -> isConfiguredCallback = null
        );
        characteristics.add(isConfigured);

        // Current Visibility State
        Characteristic currentVisibilityState = new CurrentVisibilityState(CurrentVisibilityState.SHOWN);
        characteristics.add(currentVisibilityState);

        return Collections.unmodifiableList(characteristics);
    }

    public List<Service> getLinkedServices() {
        List<Service> linkedServices = new LinkedList<>();

        linkedServices.add(televisionService);

        return Collections.unmodifiableList(linkedServices);
    }

    public int getIdentifier() {
        return identifier;
    }

    public CompletableFuture<String> getConfiguredName() {
        String name = inputSource.getConfiguredName();

        Log.i("HomeKit:InputSourceService", "getConfiguredName: " + name);
		return CompletableFuture.completedFuture(name == null ? "" : name);
    }

    public void setConfiguredName(String name) {
        Log.i("HomeKit:InputSourceService", "setConfiguredName: " + name);
        inputSource.setConfiguredName(name);
    }

    public int getInputSourceType() {
        switch (inputSource.getInputSourceType()) {
            case OTHER:
            default:
                return InputSourceType.OTHER;
            case HOME_SCREEN:
                return InputSourceType.HOME_SCREEN;
            case TUNER:
                return InputSourceType.TUNER;
            case HDMI:
                return InputSourceType.HDMI;
            case COMPOSITE_VIDEO:
                return InputSourceType.COMPOSITE_VIDEO;
            case S_VIDEO:
                return InputSourceType.S_VIDEO;
            case COMPONENT_VIDEO:
                return InputSourceType.COMPONENT_VIDEO;
            case DVI:
                return InputSourceType.DVI;
            case AIRPLAY:
                return InputSourceType.AIRPLAY;
            case USB:
                return InputSourceType.USB;
            case APPLICATION:
                return InputSourceType.APPLICATION;
        }
    }
}
