package io.github.lmarianski.avraeplus.data.sources;

import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpell;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpellCollection;
import io.github.lmarianski.avraeplus.data.sources.avrae.AvraeClient;
import io.github.lmarianski.avraeplus.data.sources.fiveetools.FiveEToolsClient;
import io.github.lmarianski.avraeplus.data.sources.json.JsonClient;

import java.io.IOException;
import java.net.URL;

public class SourceManager {

    public static boolean isURL(String uri) {
        final URL url;
        try {
            url = new URL(uri);
        } catch (Exception e1) {
            return false;
        }
        return url.getProtocol().matches("http(s)");
    }


    public static ISpellCollection getTome(String source) throws IOException {
        if (isURL(source)) {
            return JsonClient.getTome(source);
        }

        if (source.matches(".*\\d.*") || source.equals("srd")) {
            return source.equals("srd") ? AvraeClient.getSRD() : AvraeClient.getTome(source);
        } else {
            return FiveEToolsClient.getSource(source);
        }
    }

}
