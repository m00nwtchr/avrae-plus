package io.github.lmarianski.avraeplus.logistics;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import io.github.lmarianski.avraeplus.logistics.logs.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogCommands implements CommandExecutor {

    public static final Comparator<Message> TIMESTAMP_COMPARATOR = Comparator.comparingLong(a -> a.getCreationTimestamp().getEpochSecond());

    public LogCommands() {
    }

    //grabLogs solarblaster#1222 Raldoran
    //grabLogs Aelwolf#0050 Snirfell
    @Command(aliases = {"grabLogs"}, description = "Shows this page")
    public void grabLogs(String cmd, String username, String character, Server server, Message message, DiscordApi api) {
        message.delete().join();

        new Thread(() -> {
            server.getMemberByDiscriminatedName(username)
                    .ifPresent(user ->
                            api.getChannelCategoryById(508537365791506432L)
                                    .ifPresent(channelCategory -> api
                                            .getChannelById(508537338410827788L)
                                            .flatMap(Channel::asServerTextChannel)
                                            .flatMap(channel -> channel
                                                    .getMessagesAsStream()
                                                    .parallel()
                                                    .filter(msg -> msg.getMentionedUsers().contains(user) && msg.getContent().contains(character))
                                                    .min(Comparator.naturalOrder()))
                                            .ifPresent(creationMsg -> {
                                                NewCharacterLog newCharacterLog = new NewCharacterLog(creationMsg);

                                                Pattern p = Pattern.compile(".*[sS]pend.*[Ll]evel.*([0-9]*).*");
                                                List<Log> logList = channelCategory.getChannels().stream()
                                                        .filter(serverChannel -> serverChannel.asServerTextChannel().isPresent())
                                                        .flatMap(serverChannel -> serverChannel.asServerTextChannel().get().getMessagesAfterAsStream(creationMsg))
                                                        //.parallel()
                                                        .filter(msg -> msg.getMentionedUsers().contains(user) && msg.getContent().contains(character))
                                                        .filter(msg -> {
                                                            long id = msg.getChannel().getId();
                                                            return id != 509373380156194826L && id != 509373346341847050L && id != 549114112588644383L;
                                                        })
                                                        .sorted(TIMESTAMP_COMPARATOR)
                                                        .map((msg) -> {
                                                            Log log = new Log(msg);
                                                            String name = msg.getChannel().asServerChannel().get().getName();

                                                            if ("character-logs".equals(name)) {
                                                                if (p.matcher(msg.getContent()).find()) {
                                                                    log = new LvlUpLog(msg);
                                                                } else {
                                                                    log = new GameLog(msg);
                                                                }
                                                            } else if ("session-logs".equals(name)) {
                                                                log = new SessionLog(msg);
                                                            } else if ("market-logs".equals(name) && msg.getUserAuthor().isPresent() && msg.getUserAuthor().get() == user) {
                                                                log = new MarketLog(msg);
                                                            }

                                                            return log;
                                                        })
                                                        .filter(l -> l.msg.getUserAuthor().get() == user)
                                                        .collect(Collectors.toList());

                                                logList.forEach((log) -> {
                                                    int i = logList.indexOf(log);

                                                    Log prevLog = i == 0 ? newCharacterLog : logList.get(i - 1);

                                                    if (!log.goldChanged) {
                                                        log.gold = prevLog.gold;
                                                    }

                                                    if (!log.shardsChanged) {
                                                        log.shards = prevLog.shards;
                                                    }

                                                    if (!log.downtimeChanged) {
                                                        log.downtime = prevLog.downtime;
                                                    }

                                                    if ((log.shardsChanged && log.oldShards != prevLog.shards) ||
                                                            (log.goldChanged && log.oldGold != prevLog.gold) ||
                                                            (log.downtimeChanged && log.oldDowntime != prevLog.downtime)) {
                                                        log.invalid = true;
                                                        System.out.print(prevLog);
                                                        System.out.print(", ");
                                                        System.out.println(log);
                                                    }

                                                });

                                            })));
        }, "Log Processing Thread").start();
    }

}
