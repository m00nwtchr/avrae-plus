package io.github.lmarianski.avraeplus;

import io.github.lmarianski.avraeplus.avrae.homebrew.spells.Tome;
import org.bson.*;
import org.bson.codecs.*;
import org.javacord.api.entity.server.Server;

import java.util.*;

public class ServerData {

    public transient Server server;

    public long serverId;

    public transient Map<String, List<Tome.Spell>> spellMap = new HashMap<>();
    public List<Tome> tomes = new ArrayList<>();

    public long forecastPeriod;
    public long forecastChannel;

    public ServerData(Server server) {
        this.server = server;
        this.serverId = server.getId();
    }

    public ServerData() {
    }

    public String toJSON() {
        return Main.gson.toJson(this);
    }

    public static ServerData fromJSON(String json) {
        return Main.gson.fromJson(json, ServerData.class);
    }

    public Document toMongo() {
        Document doc = new Document();

        doc.append("serverId", serverId);
        doc.append("spellMap", spellMap);
        doc.append("tomes", tomes);

        return doc;
    }

    public static ServerData fromMongo(Document doc, Server server) {
        ServerData data = new ServerData(server);

        data.spellMap = (Map<String, List<Tome.Spell>>) doc.get("spellMap");
        data.tomes    = (List<Tome>) doc.get("tomes");

        return data;
    }

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
            data.spellMap = (Map<String, List<Tome.Spell>>) doc.get("spellMap");
            data.tomes = doc.getList("tomes", Tome.class);

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
