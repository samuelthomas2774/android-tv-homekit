package uk.org.fancy.AndroidTvHomeKit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import android.util.Log;

import uk.org.fancy.AndroidTvHomeKit.characteristics.ActiveIdentifier;
import uk.org.fancy.AndroidTvHomeKit.characteristics.ConfiguredName;
import uk.org.fancy.AndroidTvHomeKit.characteristics.SleepDiscoveryMode;
import io.github.hapjava.Service;
import io.github.hapjava.characteristics.Characteristic;
import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.impl.characteristics.common.Name;
import io.github.hapjava.impl.characteristics.common.ActiveCharacteristic;

public class TelevisionService implements Service {
    private static final String TAG = "HomeKit:TelevisionService";
    private final TelevisionAccessory accessory;
    private final PowerStateInterface powerState;
    private final InputSourceManagerInterface inputSourceManager;
    private final List<InputSourceService> inputSourceServices;
    private HomekitCharacteristicChangeCallback activeCallback;
    private HomekitCharacteristicChangeCallback activeIdentifierCallback;
    private HomekitCharacteristicChangeCallback configuredNameCallback;
    private HomekitCharacteristicChangeCallback sleepDiscoveryModeCallback;

    public TelevisionService(TelevisionAccessory _accessory, List<InputSourceService> _inputSourceServices) {
        accessory = _accessory;
        powerState = accessory.service.implementation.getPowerStateManager();
        inputSourceManager = accessory.service.implementation.getInputSourceManager();
        inputSourceServices = _inputSourceServices;
    }

    public String getType() {
        return "000000D8-0000-1000-8000-0026BB765291";
    }

    public List<Characteristic> getCharacteristics() {
        List<Characteristic> characteristics = new LinkedList<>();

        // Name
        Characteristic name = new Name(accessory.getLabel());
        characteristics.add(name);

        // Active
        Characteristic active = new ActiveCharacteristic(
            () -> getPowerState(),
            v -> setPowerState(v),
            c -> {
                activeCallback = c;
                powerState.onSubscribe(c);
            },
            () -> {
                activeCallback = null;
                powerState.onUnsubscribe();
            }
        );
        characteristics.add(active);

        // Active Identifier
        Characteristic activeIdentifier = new ActiveIdentifier(
            () -> getActiveIdentifier(),
            v -> setActiveIdentifier(v),
            c -> {
                activeIdentifierCallback = c;
                inputSourceManager.onSubscribe(c);
            },
            () -> {
                activeIdentifierCallback = null;
                inputSourceManager.onUnsubscribe();
            },
            inputSourceServices.size() - 1
        );
        characteristics.add(activeIdentifier);

        // Configured Name
        Characteristic configuredName = new ConfiguredName(
            () -> getConfiguredName(),
            v -> setConfiguredName(v),
            c -> configuredNameCallback = c,
            () -> configuredNameCallback = null
        );
        characteristics.add(configuredName);

        // Sleep Discovery Mode
        Characteristic sleepDiscoveryMode = new SleepDiscoveryMode(
            () -> CompletableFuture.completedFuture(SleepDiscoveryMode.ALWAYS_DISCOVERABLE),
            c -> sleepDiscoveryModeCallback = c,
            () -> sleepDiscoveryModeCallback = null
        );
        characteristics.add(sleepDiscoveryMode);

        return Collections.unmodifiableList(characteristics);
    }

    public List<Service> getLinkedServices() {
		List<Service> linkedServices = new LinkedList<Service>();

		for (InputSourceService inputSourceService: inputSourceServices) {
			linkedServices.add(inputSourceService);
		}

		return Collections.unmodifiableList(linkedServices);
    }

    public CompletableFuture<Integer> getPowerState() {
        boolean on = powerState.getPowerState();

        Log.i(TAG, "getPowerState: " + (on ? "on" : "off"));
		return CompletableFuture.completedFuture(on ? ActiveCharacteristic.ACTIVE : ActiveCharacteristic.INACTIVE);
    }

    public void setPowerState(int active) {
        Log.i(TAG, "Turn TV " + (active == ActiveCharacteristic.ACTIVE ? "on" : "off"));
        powerState.setPowerState(active == ActiveCharacteristic.ACTIVE);
    }

    public CompletableFuture<Integer> getActiveIdentifier() {
        return inputSourceManager.getActiveInput().thenApply(activeInputSource -> {
            InputSourceService activeInputSourceService = null;

            for (InputSourceService inputSourceService: inputSourceServices) {
                if (inputSourceService.inputSource == activeInputSource) activeInputSourceService = inputSourceService;
            }

            if (activeInputSourceService == null) {
                return null;
            }

            return activeInputSourceService.getIdentifier();
        });
    }

    public void setActiveIdentifier(int activeIdentifier) {
        InputSourceService activeInputSourceService = null;

        for (InputSourceService inputSourceService: inputSourceServices) {
            if (inputSourceService.getIdentifier() == activeIdentifier) activeInputSourceService = inputSourceService;
        }

        if (activeInputSourceService == null) {
            Log.i(TAG, "Cannot set ActiveIdentifier to " + Integer.toString(activeIdentifier) + ": unknown InputSource service");
            return;
        }

        inputSourceManager.setActiveInput(activeInputSourceService.inputSource);
    }

    public CompletableFuture<String> getConfiguredName() {
        return CompletableFuture.completedFuture(accessory.getLabel());
    }

    public void setConfiguredName(String configuredName) {
        Log.i(TAG, "Set configured name " + configuredName);
    }
}
