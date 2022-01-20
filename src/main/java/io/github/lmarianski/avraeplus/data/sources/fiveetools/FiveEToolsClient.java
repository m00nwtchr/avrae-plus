package io.github.lmarianski.avraeplus.data.sources.fiveetools;

import io.github.lmarianski.avraeplus.Main;
import io.github.lmarianski.avraeplus.Util;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpellCollection;
import io.github.lmarianski.avraeplus.data.sources.fiveetools.spells.FiveEBook;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.github.lmarianski.avraeplus.Main.MAP_STRING_STRING_TYPE;

public class FiveEToolsClient {

    // public static final String API_ENDPOINT = "https://5e.tools/data"; // Protected by Cloudflare, ugh
    public static final String API_ENDPOINT = "https://5etools-mirror-1.github.io/data";

    // {
    //     "AI": "spells-ai.json",
    //     "AitFR-AVT": "spells-aitfr-avt.json",
    //     "EGW": "spells-egw.json",
    //     "FTD": "spells-ftd.json",
    //     "GGR": "spells-ggr.json",
    //     "IDRotF": "spells-idrotf.json",
    //     "LLK": "spells-llk.json",
    //     "PHB": "spells-phb.json",
    //     "SCC": "spells-scc.json",
    //     "TCE": "spells-tce.json",
    //     "UA2020PsionicOptionsRevisited": "spells-ua-2020por.json",
    //     "UA2020SpellsAndMagicTattoos": "spells-ua-2020smt.json",
    //     "UA2021DraconicOptions": "spells-ua-2021do.json",
    //     "UAArtificerRevisited": "spells-ua-ar.json",
    //     "UAFighterRogueWizard": "spells-ua-frw.json",
    //     "UAModernMagic": "spells-ua-mm.json",
    //     "UASorcererAndWarlock": "spells-ua-saw.json",
    //     "UAStarterSpells": "spells-ua-ss.json",
    //     "UAThatOldBlackMagic": "spells-ua-tobm.json",
    //     "XGE": "spells-xge.json"
    // }
    

    private static final Map<String, String> SOURCES_FALLBACK = new HashMap<String, String>();
    static {
        SOURCES_FALLBACK.put("AI", "spells-ai.json");
        SOURCES_FALLBACK.put("AitFR-AVT", "spells-aitfr-avt.json");
        SOURCES_FALLBACK.put("EGW", "spells-egw.json");
        SOURCES_FALLBACK.put("FTD", "spells-ftd.json");
        SOURCES_FALLBACK.put("GGR", "spells-ggr.json");
        SOURCES_FALLBACK.put("IDRotF", "spells-idrotf.json");
        SOURCES_FALLBACK.put("LLK", "spells-llk.json");
        SOURCES_FALLBACK.put("PHB", "spells-phb.json");
        SOURCES_FALLBACK.put("SCC", "spells-scc.json");
        SOURCES_FALLBACK.put("TCE", "spells-tce.json");
        SOURCES_FALLBACK.put("XGE", "spells-xge.json");
    }

    private static Map<String, String> SOURCES;

    public static Map<String, String> getIndex() {
        if (SOURCES != null) {
            return SOURCES;
        }

        String response = null;
        Map<String, String> map = SOURCES_FALLBACK;
        try {
            response = Util.GET(API_ENDPOINT + "/spells/index.json");

            map = Main.gson.fromJson(response, MAP_STRING_STRING_TYPE);
        } catch (Exception e) {
            // e.printStackTrace();
        }

        SOURCES = map;
        return map;
    }

    public static FiveEBook[] getSources(String... ids) {
        return Arrays.stream(ids).map(FiveEToolsClient::getSource).toArray(FiveEBook[]::new);
    }

    public static Optional<ISpellCollection> getSource(String id) {
        String file = getIndex().get(id);

        String response = null;
        try {
            response = Util.GET(API_ENDPOINT + "/spells/" + file);

            FiveEBook t = Main.gson.fromJson(response, FiveEBook.class);
            t.id = id;
            // t.name = id;
            return Optional.of(t);
        } catch (Exception e) {
            // e.printStackTrace();
            Main.LOGGER.warning("Failed to get spell collection: "+id);
        }

        return Optional.empty();
    }

}
