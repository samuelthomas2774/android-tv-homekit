package uk.org.fancy.AndroidTvHomeKit.characteristics;

import io.github.hapjava.characteristics.StaticStringCharacteristic;

public class ApplicationID extends StaticStringCharacteristic {
    public ApplicationID(String value) {
        super("CA7897C3-4B46-448D-AF68-29C686B730F4", "Application ID", value);
    }
}
