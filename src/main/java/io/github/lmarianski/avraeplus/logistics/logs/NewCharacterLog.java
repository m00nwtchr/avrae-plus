package io.github.lmarianski.avraeplus.logistics.logs;

import org.javacord.api.entity.message.Message;
import org.mariuszgromada.math.mxparser.Expression;

public class NewCharacterLog extends CharacterLog {
    public NewCharacterLog(Message msg) {
        super(msg);

        String[] msgLines = msg.getContent().split("\n");

        for (String line : msgLines) {
            if (line.contains("Gold Pieces") || line.contains("Coin")) {
                // if (line.matches(".*[Aa].*[Ss].*")) {
                line = line.substring(line.indexOf(':') + 1).replaceAll("[Gg][Pp]", "").replaceAll("\\*", "").replaceAll("  * ", " ").trim();
                if (line.startsWith("-")) {
                    line = line.substring(1).trim();
                }

                Expression exp = new Expression(line);

                gold = (int) exp.calculate();

                goldChanged = true;
//                }
            }
        }
    }
}
