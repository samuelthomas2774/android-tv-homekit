package uk.org.fancy.AndroidTvHomeKit;

public interface TelevisionInterface {
    public String getName();
    public String getManufacturer();
    public String getModel();
    public String getSerialNumber();
    public String getFirmwareRevision();

    public PowerStateInterface getPowerStateManager();
    public InputSourceManagerInterface getInputSourceManager();
}
