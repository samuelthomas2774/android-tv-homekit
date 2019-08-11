package uk.org.fancy.AndroidTvHomeKit.characteristics;

import io.github.hapjava.characteristics.StaticStringCharacteristic;

public class TIFID extends StaticStringCharacteristic {
    public TIFID(String value) {
        super("CA7897C2-4B46-448D-AF68-29C686B730F4", "Android TV Input Framework ID", value);
    }
}
