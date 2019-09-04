package uk.org.fancy.AndroidTvHomeKit;

public interface TelevisionInterface {
    public String getName();
    public String getManufacturer();
    public String getModel();
    public String getSerialNumber();
    public String getFirmwareRevision();

    public PowerStateInterface getPowerStateManager();
    public InputSourceManagerInterface getInputSourceManager();

    public interface TelevisionSpeakerInterface extends TelevisionInterface {
        public uk.org.fancy.AndroidTvHomeKit.TelevisionSpeakerInterface getSpeaker();
    }

    public interface RemoteInterface extends TelevisionInterface {
        public enum RemoteKey {
            REWIND,
            FAST_FORWARD,
            NEXT_TRACK,
            PREVIOUS_TRACK,
            ARROW_UP,
            ARROW_DOWN,
            ARROW_LEFT,
            ARROW_RIGHT,
            SELECT,
            BACK,
            EXIT,
            PLAY_PAUSE,
            INFORMATION,
        }

        public void sendRemoteKey(RemoteKey key);
    }
}
