package io.github.lmarianski.avraeplus;

import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpell;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpellCollection;
import io.github.lmarianski.avraeplus.data.sources.avrae.AvraeClient;
import io.github.lmarianski.avraeplus.data.sources.fiveetools.FiveEToolsClient;
import org.bson.*;
import org.bson.codecs.*;
import org.javacord.api.entity.server.Server;

import java.util.*;
import java.util.stream.Collectors;

public class ServerData {

    public transient Server server;

    public long serverId;

    public transient Map<String, List<ISpell>> spellMap = new HashMap<>();
    public HashSet<ISpellCollection> tomes = new HashSet<>();

    public long forecastPeriod;
    public long forecastChannel;

    public ServerData(Server server) {
        this.server = server;
        this.serverId = server.getId();
    }

    public ServerData() {
    }

//    @BsonIgnore
//    public Optional<? extends Invite> getInvite() {
//        if (server.hasPermission(server.getApi().getYourself(), PermissionType.MANAGE_SERVER)) {
//            return server.getInvites().thenApply(coll -> coll.stream().filter(i -> !i.isRevoked() && !i.isTemporary() && i.getMaxUses() == 0 && i.getMaxAgeInSeconds() == 0).findFirst()).join();
//        } else if (server.hasPermission(server.getApi().getYourself(), PermissionType.CREATE_INSTANT_INVITE)) {
//            return Optional.of(server.getChannels().get(0).createInviteBuilder().setMaxUses(1).setMaxAgeInSeconds(172800).setAuditLogReason("Invite for bot support").create().join());
//        }
//        return Optional.empty();
//    }

    public Map<String, List<ISpell>> buildSpellMap() {
        List<ISpellCollection> tomes = new ArrayList<>(this.tomes);
//        tomes.add(AvraeClient.getSRD());
        tomes.addAll(FiveEToolsClient.getIndex()
                .keySet()
                .stream()
                    .filter(el -> !el.startsWith("UA"))
                    .map(FiveEToolsClient::getSource)
                    .collect(Collectors.toList())
        );

        Map<String, List<String>> addSpellLists = tomes.stream()
                .filter(Objects::nonNull)
                .filter(tome -> tome.getSpellLists() != null)
                .flatMap(tome -> tome.getSpellLists().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, List<ISpell>> map = new HashMap<>();

        tomes.stream()
                .filter(tome -> tome.getSpells() != null)
                .map(ISpellCollection::getSpells)
                .flatMap(Arrays::stream)
                .forEach(spell -> {
                    List<String> classes = new ArrayList<>(Arrays.asList(spell.getClasses()));

                    if (addSpellLists.size() > 0) {
                        addSpellLists.forEach((key, value) -> {
                            if (value.contains(spell.getName())) {
                                classes.add(key);
                            }
                        });
                    }

                    classes.add("all");

                    for (String clazz : classes) {
                        clazz = clazz.trim().toLowerCase(Locale.ROOT);

                        if (clazz.contains(" ")) {
                            String[] c = clazz.split(" ");

                            clazz = c[0];
//
//                            List<Tome.Spell> list = map.computeIfAbsent(c[1], k -> new ArrayList<>());
//                            list.add(spell);
                        }

                        List<ISpell> list = map.computeIfAbsent(clazz, k -> new ArrayList<>());
                        list.add(spell);
                    }
                });

        this.spellMap = map;
        return spellMap;
    }

    public String toJSON() {
        return Main.gson.toJson(this);
    }

    public static ServerData fromJSON(String json) {
        return Main.gson.fromJson(json, ServerData.class);
    }

//    public Document toMongo() {
//        Document doc = new Document();
//
//        doc.append("serverId", serverId);
////        doc.append("spellMap", spellMap);
//        doc.append("tomes", tomes.stream().map(ISpellCollection::toMongo).toArray(Document[]::));
//
//        return doc;
//    }
//
//    public static ServerData fromMongo(Document doc, Server server) {
//        ServerData data = new ServerData(server);
//
////        data.spellMap = (Map<String, List<ISpell>>) doc.get("spellMap");
////        data.tomes    = (List<ISpellCollection<? extends ISpell>>) doc.get("tomes");
//
//        return data;
//    }

    public static class ServerDataCodec implements CollectibleCodec<ServerData> {
        private final Codec<Document> documentCodec = new DocumentCodec();

        @Override
        public ServerData generateIdIfAbsentFromDocument(ServerData document) {
            return document;
        }

        @Override
        public boolean documentHasId(ServerData document) {
            return true;
        }

        @Override
        public BsonValue getDocumentId(ServerData document) {
            return new BsonInt64(document.serverId);
        }

        @Override
        public ServerData decode(BsonReader reader, DecoderContext decoderContext) {
            Document doc = documentCodec.decode(reader, decoderContext);
            ServerData data = new ServerData();

            data.serverId = doc.getLong("serverId");
//            data.spellMap = (Map<String, List<ISpell>>) doc.get("spellMap");
            data.tomes = new HashSet<>(doc.getList("tomes", ISpellCollection.class));

            return data;
        }

        @Override
        public void encode(BsonWriter writer, ServerData value, EncoderContext encoderContext) {
            Document doc = new Document();

            doc.append("serverId", value.serverId);
            doc.append("spellMap", value.spellMap);
            doc.append("tomes", value.tomes);

            documentCodec.encode(writer, doc, encoderContext);
        }

        @Override
        public Class<ServerData> getEncoderClass() {
            return ServerData.class;
        }
    }
}
