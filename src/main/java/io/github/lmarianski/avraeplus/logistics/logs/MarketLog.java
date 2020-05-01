package io.github.lmarianski.avraeplus.logistics.logs;

import org.javacord.api.entity.message.Message;
import org.mariuszgromada.math.mxparser.Expression;

public class MarketLog extends CharacterLog {

    public Type type;

    public MarketLog(Message msg) {
        super(msg);

        String[] msgLines = msg.getContent().split("\n");

        if (msg.getContent().contains("Acceptance*")) {
            type = Type.ACCEPTANCE;
        } else if (msg.getContent().contains("Request")) {
            type = Type.TRADE;
        } else if (msg.getContent().contains("Purchase")) {
            type = Type.PURCHASE;
        }

        for (String line : msgLines) {
            if (line.matches(".*([Tt]otal|[Rr]emaining).*")) {
                if (line.matches(".*[Gg].*[Pp].*")) {

                    if (line.contains(":")) {
                        line = line.substring(line.indexOf(':') + 1);
                    } else {
                        line = line.substring(line.lastIndexOf('*') + 1);
                    }

                    line = line.replaceAll("[Gg][Pp]", "").replaceAll("\\*", "").replaceAll("  * ", " ").trim();
                    if (line.startsWith("-")) {
                        line = line.substring(1).trim();
                    }

                    Expression exp = new Expression(line.substring(0, line.lastIndexOf("=")).trim());

                    gold = exp.calculate();
                    oldGold = exp.getCopyOfInitialTokens().get(0).tokenValue;

                    goldChanged = gold != oldGold;
                }
                if (line.matches(".*[Aa].*[Ss].*")) {
                    line = line.substring(line.indexOf(':') + 1).replaceAll("[Aa][Ss]", "").replaceAll("\\*", "").replaceAll("  * ", " ").trim();
                    if (line.startsWith("-")) {
                        line = line.substring(1).trim();
                    }

                    Expression exp = new Expression(line.substring(0, line.lastIndexOf("=")).trim());

                    shards = (int) exp.calculate();
                    oldShards = (int) exp.getCopyOfInitialTokens().get(0).tokenValue;

                    shardsChanged = shards != oldShards;
                }
                if (line.matches(".*[Dd].*[Dd].*")) {
                    line = line.substring(line.indexOf(':') + 1).replaceAll("[Dd][Dd]", "").replaceAll("\\*", "").replaceAll("  * ", " ").trim();
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
        return "MarketLog{" +
                "type=" + type +
                ", oldGold=" + oldGold +
                ", gold=" + gold +
                ", oldDowntime=" + oldDowntime +
                ", downtime=" + downtime +
                ", oldShards=" + oldShards +
                ", shards=" + shards;
    }

    public enum Type {
        TRADE, ACCEPTANCE, PURCHASE
    }

}
