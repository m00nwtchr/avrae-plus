package io.github.lmarianski.avraeplus.logistics.logs;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;

import javax.annotation.Nonnull;
import java.util.Optional;

public class Log {

    public Message msg;
    public User creator;

    public double gold;
    public boolean goldChanged = false;
    public double oldGold;

    public int downtime;
    public boolean downtimeChanged = false;
    public int oldDowntime;

    public int shards;
    public boolean shardsChanged = false;
    public int oldShards;

    public boolean invalid = false;

    public Log(Message msg) {
        this.msg = msg;
        msg.getUserAuthor().ifPresent(user -> {
            this.creator = user;
        });
    }

    @Override
    public String toString() {
        return "Log{creator=" + creator +
                ", msg="+msg.getContent() +
                ", oldGold=" + oldGold +
                ", gold=" + gold +
                ", oldDowntime=" + oldDowntime +
                ", downtime=" + downtime +
                ", oldShards=" + oldShards +
                ", shards=" + shards;
    }

}
