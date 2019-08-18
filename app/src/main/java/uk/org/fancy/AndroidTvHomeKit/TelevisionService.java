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
    private final TelevisionAccessory accessory;
    private final List<InputSourceService> inputSourceServices;
    private HomekitCharacteristicChangeCallback activeCallback;
    private HomekitCharacteristicChangeCallback activeIdentifierCallback;
    private HomekitCharacteristicChangeCallback configuredNameCallback;
    private HomekitCharacteristicChangeCallback sleepDiscoveryModeCallback;

    public TelevisionService(TelevisionAccessory _accessory, List<InputSourceService> _inputSourceServices) {
        accessory = _accessory;
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
                accessory.service.implementation.getPowerStateManager().onSubscribe(c);
            },
            () -> {
                activeCallback = null;
                accessory.service.implementation.getPowerStateManager().onUnsubscribe();
            }
        );
        characteristics.add(active);

        // Active Identifier
        Characteristic activeIdentifier = new ActiveIdentifier(
            () -> getActiveIdentifier(),
            v -> setActiveIdentifier(v),
            c -> {
                activeIdentifierCallback = c;
                accessory.service.implementation.getInputSourceManager().onSubscribe(c);
            },
            () -> {
                activeIdentifierCallback = null;
                accessory.service.implementation.getInputSourceManager().onUnsubscribe();
            }
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
        boolean on = accessory.service.implementation.getPowerStateManager().getPowerState();

        Log.i("HomeKit:TelevisionService", "getPowerState: " + (on ? "on" : "off"));
		return CompletableFuture.completedFuture(on ? ActiveCharacteristic.ACTIVE : ActiveCharacteristic.INACTIVE);
    }

    public void setPowerState(int active) {
        Log.i("HomeKit:TelevisionService", "Turn TV " + (active == ActiveCharacteristic.ACTIVE ? "on" : "off"));
        accessory.service.implementation.getPowerStateManager().setPowerState(active == ActiveCharacteristic.ACTIVE);
    }

    public CompletableFuture<Integer> getActiveIdentifier() {
        return accessory.service.implementation.getInputSourceManager().getActiveInput().thenApply(activeInputSource -> {
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
            Log.i("HomeKit:TelevisionService", "Cannot set ActiveIdentifier to " + Integer.toString(activeIdentifier) + ": unknown InputSource service");
            return;
        }

        accessory.service.implementation.getInputSourceManager().setActiveInput(activeInputSourceService.inputSource);
    }

    public CompletableFuture<String> getConfiguredName() {
        return CompletableFuture.completedFuture(accessory.getLabel());
    }

    public void setConfiguredName(String configuredName) {
        Log.i("HomeKit:TelevisionService", "Set configured name " + configuredName);
    }
}
