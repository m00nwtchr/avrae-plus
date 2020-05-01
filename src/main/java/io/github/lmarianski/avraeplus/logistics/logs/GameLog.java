package io.github.lmarianski.avraeplus.logistics.logs;

import org.javacord.api.entity.message.Message;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GameLog extends CharacterLog {

    public String gameName;
    public Type gameType;
    public ActionType actionType;

    public GameLog(Message msg) {
        super(msg);

        String[] msgLines = msg.getContent().split("\n");

        actionType = msgLines[1].matches(".*[Cc]ompleted.*") ? ActionType.COMPLETED : ActionType.RAN;
        gameType = msgLines[1].matches(".*[Aa]ssignment.*") ? Type.ASSIGNMENT : Type.GAME;

        int endI = msgLines[1].length()+1;

        if (msgLines[1].contains("-"))
            endI = msgLines[1].lastIndexOf('-');
        else if (msgLines[1].contains("*"))
            endI = msgLines[1].replaceAll("\\*\\*", "*").lastIndexOf("*");

        gameName = msgLines[1].substring(msgLines[1].indexOf(':')+1, endI).trim();

        gameName = gameName.replaceAll("\\*", "").replaceAll("_", "").trim();

        for (String line : msgLines) {
            if (line.matches(".*[Tt]otal .*") && !line.contains("healing")) {
                if (line.matches(".*[Gg].*[Pp].*")) {
                    line = line.substring(line.lastIndexOf('*')+1).replaceAll("[Gg][Pp]", "").replaceAll("\\*", "").replaceAll("  * ", " ").trim();
                    if (line.startsWith("-")) {
                        line = line.substring(1).trim();
                    }

                    Expression exp = new Expression(line.substring(0, line.lastIndexOf("=")).trim());

                    gold = exp.calculate();
                    oldGold = exp.getCopyOfInitialTokens().get(0).tokenValue;

                    goldChanged = gold != oldGold;
                }
                if (line.matches(".*[Aa].*[Ss].*")) {
                    line = line.substring(line.lastIndexOf('*')+1).replaceAll("[Aa][Ss]", "").replaceAll("\\*", "").replaceAll("  * ", " ").trim();
                    if (line.startsWith("-")) {
                        line = line.substring(1).trim();
                    }

                    Expression exp = new Expression(line.substring(0, line.lastIndexOf("=")).trim());

                    shards = (int) exp.calculate();
                    oldShards = (int) exp.getCopyOfInitialTokens().get(0).tokenValue;

                    shardsChanged = shards != oldShards;
                }
                if (line.matches(".*[Dd].*[Dd].*")) {
                    line = line.substring(line.lastIndexOf('*')+1).replaceAll("[Dd][Dd]", "").replaceAll("\\*", "").replaceAll("  * ", " ").trim();
                    if (line.startsWith("-")) {
                        line = line.substring(1).trim();
                    }

                    Expression exp = new Expression(line.substring(0, line.lastIndexOf("=")).trim());

                    downtime = (int) exp.calculate();
                    oldDowntime = (int) exp.getCopyOfInitialTokens().get(0).tokenValue;

                    downtimeChanged = downtime != oldDowntime;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "GameLog{" +
                "gameName='" + gameName + '\'' +
                ", oldGold=" + oldGold +
                ", gold=" + gold +
                ", oldDowntime=" + oldDowntime +
                ", downtime=" + downtime +
                ", oldShards=" + oldShards +
                ", shards=" + shards;
    }

    public enum Type {
        ASSIGNMENT, GAME;

        private static List<String> vals = Arrays.stream(values()).map(Enum::toString).collect(Collectors.toList());

        public static Optional<Type> value(String s) {
            return Optional.ofNullable(vals.contains(s) ? valueOf(s) : null);
        }
    }

    public enum ActionType {
        COMPLETED, RAN;

        private static List<String> vals = Arrays.stream(values()).map(Enum::toString).collect(Collectors.toList());

        public static Optional<ActionType> value(String s) {
            String sup = s.toUpperCase(Locale.ROOT);
            return Optional.ofNullable(vals.contains(sup) ? valueOf(sup) : null);
        }
    }
}
