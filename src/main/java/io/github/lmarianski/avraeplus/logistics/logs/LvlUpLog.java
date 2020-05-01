package io.github.lmarianski.avraeplus.logistics.logs;

import org.javacord.api.entity.message.Message;
import org.mariuszgromada.math.mxparser.Expression;

public class LvlUpLog extends CharacterLog {

    public LvlUpLog(Message msg) {
        super(msg);
        String[] msgLines = msg.getContent().split("\n");

        for (String line : msgLines) {
            if ((line.contains("Remaining") || line.contains("Total")) && !line.contains("Level")) {
                // if (line.matches(".*[Aa].*[Ss].*")) {
                line = line.substring(line.indexOf(':') + 1).replaceAll("[Aa][Ss]", "").replaceAll("\\*", "").replaceAll("  * ", " ").trim();
                if (line.startsWith("-")) {
                    line = line.substring(1).trim();
                }

                Expression exp = new Expression(line.substring(0, line.lastIndexOf("=")).trim());

                shards = (int) exp.calculate();
                oldShards = (int) exp.getCopyOfInitialTokens().get(0).tokenValue;

                shardsChanged = shards != oldShards;
//                }
            }
        }
    }

    @Override
    public String toString() {
        return "LvlUpLog{oldShards=" + oldShards +
                ", shards=" + shards +
                '}';
    }
}
