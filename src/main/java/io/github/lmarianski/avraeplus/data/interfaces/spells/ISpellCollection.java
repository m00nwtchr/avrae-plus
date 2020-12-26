package io.github.lmarianski.avraeplus.data.interfaces.spells;

import java.io.IOException;
import java.lang.Class;

import io.github.lmarianski.avraeplus.data.sources.SourceManager;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.*;

import java.util.List;
import java.util.Map;

public interface ISpellCollection {

    String getId();
    String getName();
    String getImage();

    ISpell[] getSpells();
    Map<String, List<String>> getSpellLists();

//    Document toMongo();

    class Codec implements CollectibleCodec<ISpellCollection> {
        private final org.bson.codecs.Codec<String> stringCodec = new StringCodec();

        @Override
        public ISpellCollection generateIdIfAbsentFromDocument(ISpellCollection document) {
            return document;
        }

        @Override
        public boolean documentHasId(ISpellCollection document) {
            return false;
        }

        @Override
        public BsonValue getDocumentId(ISpellCollection document) {
            return null;
        }

        @Override
        public ISpellCollection decode(BsonReader reader, DecoderContext decoderContext) {
//            Document doc = documentCodec.decode(reader, decoderContext);

//            Class clazz = doc.get("documentType", Class.class);
//
//            clazz.getMethod("fromMongo")
//
            try {
                return SourceManager.getTome(stringCodec.decode(reader, decoderContext));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void encode(BsonWriter writer, ISpellCollection value, EncoderContext encoderContext) {
            stringCodec.encode(writer, value.getId(), encoderContext);
        }

        @Override
        public Class<ISpellCollection> getEncoderClass() {
            return ISpellCollection.class;
        }
    }
}
