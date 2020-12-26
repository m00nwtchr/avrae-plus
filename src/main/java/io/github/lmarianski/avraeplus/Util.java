package io.github.lmarianski.avraeplus;

import org.apache.commons.io.IOUtils;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.util.event.ListenerManager;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.lmarianski.avraeplus.Main.LEFT_ARROW;
import static io.github.lmarianski.avraeplus.Main.RIGHT_ARROW;

public class Util {

    public static String GET(URL url, String... opts) throws IOException {
        return GET(url.toString(), opts);
    }

    public static String GET(String url, String... opts) throws IOException {
        List<String> opt = Arrays.asList("curl", url);
        opt.addAll(Arrays.asList(opts));

        Process p = new ProcessBuilder(
                opt
        ).start();

        return IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);
    }

    public static Stream<String> paginate(Stream<String> strings, int no) {
        AtomicInteger counter = new AtomicInteger();

        return strings
                .collect(Collectors.groupingBy(ch -> Math.floor(counter.getAndIncrement() / (double) no), Collectors.joining("\n")))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue);
    }

    public static CompletableFuture<Message> sendPaginatedMessage(TextChannel channel, User user, EmbedBuilder embed, List<String> pages) {
        embed.setDescription(pages.get(0))
                .setFooter("Page 1 out of " + pages.size());

        return channel.sendMessage(embed).thenApply((msg) -> {
            msg.addReaction(LEFT_ARROW).join();
            msg.addReaction(RIGHT_ARROW).join();

            AtomicInteger pageIndex = new AtomicInteger();

            ListenerManager<ReactionAddListener> listener = msg.addReactionAddListener(e -> {

                User u = e.getUser().orElse(null);

                if (u == null || !u.isYourself()) e.removeReaction();
                if (u == null || u.equals(user)) {
                    int pageIndexx = pageIndex.get();

                    if (e.getEmoji().asUnicodeEmoji().isPresent()) {
                        switch (e.getEmoji().asUnicodeEmoji().get()) {
                            case (LEFT_ARROW):
                                pageIndexx--;
                                break;
                            case (RIGHT_ARROW):
                                pageIndexx++;
                                break;
                        }

                        pageIndexx = (pageIndexx < 0) ? 0 : ((pageIndexx >= pages.size()) ? (pages.size() - 1) : pageIndexx);

                        if (pageIndex.get() != pageIndexx) {
                            e.editMessage(embed
                                    .setDescription(pages.get(pageIndexx))
                                    .setFooter("Page " + (pageIndexx + 1) + " out of " + pages.size())
                            );
                        }

                        pageIndex.set(pageIndexx);
                    }
                }
            });

            listener.removeAfter(5, TimeUnit.MINUTES).addRemoveHandler(() -> {
                msg.edit(embed
                        .setDescription("Expired!")
                        .setFooter("")
                );
                msg.removeAllReactions();
            });

            return msg;
        });
    }

}
