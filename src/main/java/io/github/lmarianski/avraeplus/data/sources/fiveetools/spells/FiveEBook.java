package io.github.lmarianski.avraeplus.data.sources.fiveetools.spells;

import io.github.lmarianski.avraeplus.data.SpellSchool;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpell;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpellCollection;
import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FiveEBook implements ISpellCollection {

    public String id;

    public FiveESpell[] spell;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public String getImage() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ISpellCollection && ((ISpellCollection) o).getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public FiveESpell[] getSpells() {
        return spell;
    }

    @Override
    public Map<String, List<String>> getSpellLists() {
        return null;
    }

    public static class FiveESpell implements ISpell {
        public String name;
        public int level;
        public SpellSchool school;
//        public XXX components;
//        public XXX duration;
//        public XXX[] entries;
        public Classes classes;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public SpellSchool getSchool() {
            return school;
        }

        @Override
        public String[] getClasses() {
            return classes != null && classes.fromClassList != null ? Arrays.stream(classes.fromClassList).map(el -> el.name).toArray(String[]::new) : new String[0];
        }

        @Override
        public String getClassString() {
            return String.join(", ", getClasses());
        }

        @Override
        public String getDescription() {
            return "null";
        }

        @Override
        public boolean isRitual() {
            return false;
        }

        public static class Classes {
            public FiveEClass[] fromClassList;
            public FiveEClass[] fromClassListVariant;
            public FiveESubClass[] fromSubclass;

            public static class FiveEClass {
                public String name;
                public String source;
            }
            public static class FiveESubClass {
                @BsonProperty("class")
                public FiveEClass clasz;
                public FiveEClass subclass;
            }
        }
    }

}
