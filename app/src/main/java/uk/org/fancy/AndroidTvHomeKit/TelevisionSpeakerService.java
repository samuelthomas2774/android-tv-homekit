package uk.org.fancy.AndroidTvHomeKit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import android.util.Log;

import uk.org.fancy.AndroidTvHomeKit.characteristics.Mute;
import uk.org.fancy.AndroidTvHomeKit.characteristics.VolumeControlType;
import uk.org.fancy.AndroidTvHomeKit.characteristics.Volume;
import uk.org.fancy.AndroidTvHomeKit.characteristics.VolumeSelector;
import io.github.hapjava.Service;
import io.github.hapjava.characteristics.Characteristic;
import io.github.hapjava.HomekitCharacteristicChangeCallback;
import io.github.hapjava.impl.characteristics.common.Name;
import io.github.hapjava.impl.characteristics.common.ActiveCharacteristic;

public class TelevisionSpeakerService implements Service {
    private final TelevisionAccessory accessory;
    private final TelevisionSpeakerInterface speaker;
    private HomekitCharacteristicChangeCallback muteCallback;
    private HomekitCharacteristicChangeCallback volumeCallback;

    public TelevisionSpeakerService(TelevisionAccessory _accessory) {
        accessory = _accessory;
        speaker = ((TelevisionInterface.TelevisionSpeakerInterface) accessory.service.implementation).getSpeaker();
    }

    public String getType() {
        return "00000113-0000-1000-8000-0026BB765291";
    }

    public List<Characteristic> getCharacteristics() {
        List<Characteristic> characteristics = new LinkedList<>();

        // Mute
        Characteristic mute = new Mute(
            () -> speaker.getMute(),
            v -> speaker.setMute(v),
            c -> {
                muteCallback = c;
                speaker.onMuteSubscribe(c);
            },
            () -> {
                muteCallback = null;
                speaker.onMuteUnsubscribe();
            }
        );
        characteristics.add(mute);

        // Volume Control Type
        int type;
        switch (speaker.getVolumeControlType()) {
            default:
            case NONE:
                type = VolumeControlType.NONE;
            case RELATIVE:
                type = VolumeControlType.RELATIVE;
            case RELATIVE_WITH_CURRENT:
                type = VolumeControlType.RELATIVE_WITH_CURRENT;
            case ABSOLUTE:
                type = VolumeControlType.ABSOLUTE;
        }
        Characteristic volumeControlType = new VolumeControlType(type);

        // Volume
        if (speaker instanceof TelevisionSpeakerInterface.AbsoluteVolumeInterface) {
            Characteristic volume = new Volume(
                () -> ((TelevisionSpeakerInterface.AbsoluteVolumeInterface) speaker).getVolume(),
                v -> ((TelevisionSpeakerInterface.AbsoluteVolumeInterface) speaker).setVolume(v),
                c -> {
                    volumeCallback = c;
                    ((TelevisionSpeakerInterface.AbsoluteVolumeInterface) speaker).onVolumeSubscribe(c);
                },
                () -> {
                    volumeCallback = null;
                    ((TelevisionSpeakerInterface.AbsoluteVolumeInterface) speaker).onVolumeUnsubscribe();
                }
            );
            characteristics.add(volume);
        }

        // Volume Selector
        if (speaker instanceof TelevisionSpeakerInterface.RelativeVolumeInterface) {
            Characteristic volumeSelector = new VolumeSelector(v -> {
                switch (v) {
                    default:
                    case VolumeSelector.INCREMENT:
                        ((TelevisionSpeakerInterface.RelativeVolumeInterface) speaker).incrementVolume();
                    case VolumeSelector.DECREMENT:
                        ((TelevisionSpeakerInterface.RelativeVolumeInterface) speaker).decrementVolume();
                }
            });
            characteristics.add(volumeSelector);
        }

        return Collections.unmodifiableList(characteristics);
    }
}
