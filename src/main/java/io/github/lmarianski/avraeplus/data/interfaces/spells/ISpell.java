package io.github.lmarianski.avraeplus.data.interfaces.spells;

import io.github.lmarianski.avraeplus.data.SpellSchool;

public interface ISpell {

    String getName();
    int getLevel();
    SpellSchool getSchool();
    String[] getClasses();
    String getClassString();

    String getDescription();

    boolean isRitual();
}
