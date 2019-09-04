package uk.org.fancy.AndroidTvHomeKit;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import android.util.Log;
import io.github.hapjava.HomekitAccessory;
import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.Service;
import io.github.hapjava.impl.accessories.Bridge;

public class TelevisionAccessory implements HomekitAccessory, Bridge {
	private static final String TAG = "HomeKit:TelevisionAccessory";
	public final HomeKitService service;
	private final String serialNumber;
	private final String model;
	private final String manufacturer;
	private final String firmwareRevision;
	private final Collection<InputSourceInterface> inputSources;
	private HomekitCharacteristicChangeCallback subscribeCallback = null;

	public TelevisionAccessory(HomeKitService _service) {
		service = _service;

		serialNumber = service.implementation.getSerialNumber();
		model = service.implementation.getModel();
		manufacturer = service.implementation.getManufacturer();

		String _firmwareRevision = service.implementation.getFirmwareRevision();
		if (_firmwareRevision != null && _firmwareRevision.length() > 0) _firmwareRevision = _firmwareRevision + "; ";
		_firmwareRevision = _firmwareRevision + "HomeKit " + HomeKitService.HOMEKIT_VERSION;

		firmwareRevision = _firmwareRevision;

		inputSources = service.implementation.getInputSourceManager().getInputSources();
	}

	public int getId() {
		return 1;
	}

	public String getLabel() {
		return "Android TV";
	}

	public void identify() {
		Log.i(TAG, "Identify called");
	}

	public String getSerialNumber() {
		if (serialNumber != null) return serialNumber;

		return "Unknown";
	}

	public String getModel() {
		if (model != null) return model;

		return "Unknown";
	}

	public String getManufacturer() {
		if (manufacturer != null) return manufacturer;

		return "Unknown";
	}

	public String getFirmwareRevision() {
		return firmwareRevision;
	}

    public Collection<Service> getServices() {
		Collection<Service> services = new LinkedList<Service>();
		List<InputSourceService> inputSourceServices = new LinkedList<InputSourceService>();

		int inputSourceIdentifier = 0;

		for (InputSourceInterface inputSource: inputSources) {
			InputSourceService inputSourceService = new InputSourceService(this, inputSource, inputSourceIdentifier++);
			inputSourceServices.add(inputSourceService);
		}

		// Television
		TelevisionService television = new TelevisionService(this, inputSourceServices);
		services.add(television);

		// TelevisionSpeaker
		if (service.implementation instanceof TelevisionInterface.TelevisionSpeakerInterface) {
			TelevisionSpeakerService speaker = new TelevisionSpeakerService(this);
			services.add(speaker);
		}

		for (InputSourceService inputSourceService: inputSourceServices) {
			services.add(inputSourceService);
		}

		return Collections.unmodifiableCollection(services);
    }
}
