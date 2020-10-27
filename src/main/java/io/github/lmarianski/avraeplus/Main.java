package io.github.lmarianski.avraeplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;
import io.github.lmarianski.avraeplus.avrae.AvraeClient;
import io.github.lmarianski.avraeplus.avrae.homebrew.spells.School;
import io.github.lmarianski.avraeplus.avrae.homebrew.spells.Tome;
import io.github.lmarianski.avraeplus.logistics.LogCommands;
import org.apache.commons.text.WordUtils;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.server.ServerJoinListener;
import org.javacord.api.util.event.ListenerManager;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main implements CommandExecutor {

    private static final String DISCORD_BOT_TOKEN = System.getenv("DISCORD_BOT_TOKEN");

    public static DiscordApi bot;
    public static CommandHandler cmdHandler;

    public static MongoClient mongoClient;
    public static MongoDatabase db;
    public static Gson gson;

    public static final String LEFT_ARROW = "⬅️";
    public static final String RIGHT_ARROW = "➡️";

    public static final Map<Long, ServerData> SERVER_DATA_MAP = new HashMap<>();

    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static MongoCollection<ServerData> serversCollection;

    public static Timer timer = new Timer();

    public static void updateActivity() {
        bot.updateActivity(bot.getServers().size() + " Servers | " + cmdHandler.getDefaultPrefix() + "help");
    }

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();

        gson = new GsonBuilder()
                .create();

        {
            bot = new DiscordApiBuilder().setToken(DISCORD_BOT_TOKEN).login().join();
            bot.addListener(ServerJoinListener.class, (e) -> updateActivity());

            cmdHandler = new JavacordHandler(bot);

            cmdHandler.setDefaultPrefix(env.getOrDefault("PREFIX", "!!"));

            cmdHandler.registerCommand(new Main());
            cmdHandler.registerCommand(new LogCommands());

            cmdHandler.registerCommand(new HelpCommand(cmdHandler));

            updateActivity();
        }

        {
            ConnectionString mongoUri = env.containsKey("MONGODB_URI") ? new ConnectionString(env.get("MONGODB_URI")) : null;

            Tome.TomeCodec tomeCodec = new Tome.TomeCodec();
            ServerData.ServerDataCodec serverDataCodec = new ServerData.ServerDataCodec();

            CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    CodecRegistries.fromCodecs(tomeCodec),
                    CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            mongoClient = mongoUri != null ? MongoClients.create(mongoUri) : MongoClients.create();
            db = mongoClient.getDatabase(mongoUri != null && mongoUri.getDatabase() != null ? mongoUri.getDatabase() : "serverTomeDB");
            serversCollection = db.getCollection("servers", ServerData.class).withCodecRegistry(codecRegistry);

            bot.getServers().forEach(Main::getOrCreateData);
        }

        {



        }

//        try {
//            System.out.println(GSheetsClient.getRange("1KnzmBGs2CgUoApHe-okcCHDmJDWkxjSKRYM3nQQAzX0","VersionNum").getValues().get(0).get(0));
//
//            final ResourceConfig resourceConfig = new ResourceConfig(GSheetResource.class);
//            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(env.getOrDefault("HOST", "http://localhost")+":"+env.getOrDefault("PORT", "8080")), resourceConfig, false);
//            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    server.shutdownNow();
//                }
//            }));
//            server.start();
//
//            Thread.currentThread().join();
//        } catch (IOException | InterruptedException ex) {
//            LOGGER.log(Level.SEVERE, null, ex);
//        }
    }

    public static boolean hasRole(String string, User user, Server server) {
        List<Role> r = server.getRolesByNameIgnoreCase(string);

        return r.size() > 0 && user.getRoles(server).contains(r.get(0));
    }

    public static boolean isManager(User user, Server server) {
        return bot.getOwnerId() == user.getId() || hasRole("Server Brewer", user, server) || server.getPermissions(user).getState(PermissionType.MANAGE_SERVER) == PermissionState.ALLOWED;
    }

    public static ServerData getOrCreateData(Server server) {
        synchronized (SERVER_DATA_MAP) {
            return SERVER_DATA_MAP.computeIfAbsent(server.getId(), id -> {

                Document doc = new Document("serverId", id);
                ServerData data;

                if (serversCollection.countDocuments(doc) == 0) {
                    data = new ServerData(server);

                    serversCollection.insertOne(data);
                } else {
                    data = Objects.requireNonNull(serversCollection.find(doc).limit(1).first());
                    data.server = server;
                    data.serverId = server.getId();
                }

                data.buildSpellMap();

                return data;
            });
        }
    }

    @Command(aliases = {"rebuild"}, description = "Rebuilds this server's spell database. (Use this to pull changes)")
    public void rebuildSpellMap(Server server, TextChannel channel, User user) {
        if (isManager(user, server)) {
            ServerData serverData = getOrCreateData(server);

            Message msg = channel.sendMessage("Rebuilding DB...").join();
            serverData.buildSpellMap();

            List<Tome> tomes = new ArrayList<>(serverData.tomes);
            tomes.add(AvraeClient.getSRD());
            long spellCount = tomes.stream()
                    .filter(tome -> tome.spells != null)
                    .map(tome -> tome.spells)
                    .flatMap(Arrays::stream)
                    .count();

            msg.edit("Done, " + serverData.spellMap.size() + " classes found, with a total of " + spellCount + " spells");
        } else {
            channel.sendMessage("You don't have permission to do that!");
        }
    }

    public static void onDataChange(Server server, ServerData data) {

        serversCollection.findOneAndReplace(new Document("serverId", server.getId()), data);

    }


    public static boolean addTome(Server server, Tome tome) {
        ServerData data = getOrCreateData(server);

        if (!data.tomes.contains(tome)) {
            data.tomes.add(tome);
            onDataChange(server, data);
            return true;
        }
        return false;
    }

    public static boolean removeTome(Server server, Tome tome) {
        ServerData data = getOrCreateData(server);

        if (!data.tomes.contains(tome)) {
            return false;
        } else {
            data.tomes.remove(tome);
            onDataChange(server, data);
            return true;
        }
    }


    // @Command(aliases = "stats", description = "Bot stats")
    // public void statCommand(TextChannel channel) {
        // channel.sendMessage("# of servers: "+bot.getServers().size(););
    // }

    @Command(aliases = "invite", description = "Sends an invite for this bot.")
    public String onInviteCommand() {
        return bot.createBotInvite(new PermissionsBuilder().setAllowed(
                PermissionType.SEND_MESSAGES,
                PermissionType.ADD_REACTIONS,
                PermissionType.MANAGE_MESSAGES
        ).build());
    }

    @Command(aliases = "addtome", usage = "addtome <tomeid>", description = "Adds a tome to this bots database for this server")
    public void onAddTomeCommand(String name, String tomeid, User user, Server server, TextChannel channel) {
        if (isManager(user, server)) {
            Tome tome = AvraeClient.getTome(tomeid);
            ServerData serverData = getOrCreateData(server);

            if (tome == null) {
                channel.sendMessage(new EmbedBuilder()
                        .setDescription("Invalid tome id!")
                );
                return;
            }

            if (addTome(server, tome)) {
                channel.sendMessage(new EmbedBuilder()
                        .setTitle(tome.name)
                        .setDescription("Tome added!")
                        .setUrl("https://avrae.io/homebrew/spells/" + tome.id)
                        .setImage(tome.image)
                );

                serverData.buildSpellMap();
            } else {
                channel.sendMessage(new EmbedBuilder()
                        .setDescription("Tome is already added!")
                );
            }
        } else {
            channel.sendMessage("You don't have permission to do that!");
        }
    }

    @Command(aliases = {"removetome", "rmtome"}, usage = "rmtome <tomeid>", description = "Removes a tome from this bots database for this server")
    public void onRemoveTome(String[] args, User user, Server server, TextChannel channel) throws Exception {
        if (isManager(user, server)) {
            Tome tome = AvraeClient.getTome(args[0]);
            ServerData serverData = getOrCreateData(server);

            if (tome == null) {
                channel.sendMessage(new EmbedBuilder()
                        .setDescription("Invalid tome id!")
                );
                return;
            }

            if (removeTome(server, tome)) {
                channel.sendMessage(new EmbedBuilder()
                        .setTitle(tome.name)
                        .setDescription("Tome removed!")
                );

                serverData.buildSpellMap();
            } else {
                channel.sendMessage(new EmbedBuilder()
                        .setTitle(tome.name)
                        .setDescription("Tome is not added!")
                );
            }
        } else {
            channel.sendMessage("You don't have permission to do that!");
        }
    }

    public static boolean isURL(String uri) {
        final URL url;
        try {
            url = new URL(uri);
        } catch (Exception e1) {
            return false;
        }
        return url.getProtocol().matches("http(s)");
    }

    @Command(aliases = {"listtomes", "lstomes"}, description = "Adds a tome to this bots database for this server")
    public void onListTomes(Server server, TextChannel channel) {
        ServerData data = getOrCreateData(server);
        channel.sendMessage(new EmbedBuilder()
                .setDescription(
                        data.tomes.stream()
                                .map(tome -> tome.name + " ("+ (isURL(tome.id) ? "" : "https://avrae.io/homebrew/spells/") + tome.id + ")")
                                .collect(Collectors.joining("\n"))
                ));
    }

//    public static Predicate<Tome.Spell> spellFilter(boolean level, boolean ritualOnly, List<String> notClasses) {
//        if (ritualOnly) {
//            return spell -> {
//                return
//            }
//        } else {
//            return spell -> {
//
//            }
//        }
//    }


    @Command(aliases = {"spelllist", "spells", "sl"}, usage = "sl <class> [level] [--ritual] [--!classname] [--schoolname]", description = "Lists spells for that class and level")
    public void onSpellListCommand(String[] argz, Server server, TextChannel channel, User user) {
        ArrayList<String> args = new ArrayList<>(Arrays.asList(argz));

        String clazz = args.get(0).toLowerCase(Locale.ROOT);

        int tmpLvl = -1;
        try {
            tmpLvl = Integer.parseInt(args.get(1));
        } catch (Exception ignored) {
        }
        int level = tmpLvl;

        boolean ritualOnly = args.contains("--ritual");

        ServerData serverData = getOrCreateData(server);

        List<Tome.Spell> spells = serverData.spellMap.get(clazz);

        if (spells != null) {
            Stream<Tome.Spell> spellStream = spells.stream().sorted(Comparator.<Tome.Spell, Integer>comparing(a -> a.level).thenComparing(a -> a.name));//.filter(spellFilter(level != -1, ritualOnly, );

            if (level != -1) {
                spellStream = spellStream.filter(s -> s.level == level);
            }

            if (ritualOnly) {
                spellStream = spellStream.filter(s -> s.ritual);
            }

            List<School> schools = args.stream()
                    .filter(s -> s.startsWith("--"))
                    .map(s -> s.substring(2).toLowerCase(Locale.ROOT))
                    .map(School::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<String> notClasses = args.stream()
                    .filter(s -> s.startsWith("--!"))
                    .map(s -> s.substring(3).toLowerCase(Locale.ROOT))
                    .filter(s -> serverData.spellMap.containsKey(s))
                    .collect(Collectors.toList());

            List<String> yesClasses = args.stream()
                    .filter(s -> s.startsWith("--"))
                    .map(s -> s.substring(2).toLowerCase(Locale.ROOT))
                    .filter(s -> serverData.spellMap.containsKey(s))
                    .collect(Collectors.toList());

            if (notClasses.size() != 0) {
                spellStream = spellStream
                        .filter(s -> notClasses.stream().noneMatch(st -> s.classes.toLowerCase(Locale.ROOT).contains(st)));
            }

            if (yesClasses.size() != 0) {
                spellStream = spellStream
                        .filter(s -> yesClasses.stream().allMatch(st -> s.classes.toLowerCase(Locale.ROOT).contains(st)));
            }

            if (schools.size() != 0) {
                spellStream = spellStream
                        .filter(s -> schools.contains(s.school));
            }

            List<String> pages;

            if (level == -1) {
                ArrayList<Map.Entry<Integer, List<Tome.Spell>>> l = new ArrayList<>(spellStream.collect(Collectors.groupingBy(spell -> spell.level, Collectors.toList())).entrySet());
                ArrayList<String> strings = new ArrayList<>();
                l.forEach((el) -> {
                    el.getValue().sort(Comparator.comparing(a -> a.name));
                    strings.add("**Level " + el.getKey() + " Spells**");
                    strings.addAll(el.getValue().stream().map(a -> a.name).collect(Collectors.toList()));
                });

                AtomicInteger counter = new AtomicInteger();

                pages = new ArrayList<>();

                strings.stream()
                        .collect(
                                Collectors.groupingBy(ch -> Math.floor(counter.getAndIncrement() / 20D), Collectors.joining("\n"))
                        ).entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEachOrdered(x -> pages.add(x.getValue()));
            } else {
                AtomicInteger counter = new AtomicInteger();

                pages = new ArrayList<>(spellStream.map(spell -> spell.name)
                        .collect(Collectors.groupingBy(ch -> Math.floor(counter.getAndIncrement() / 20D), Collectors.joining("\n")))
                        .values()
                );
            }

            if (pages.size() == 0) {
                pages.add("No spells found!");
            }

            StringBuilder titleBuilder = new StringBuilder("All");
            if (level != -1) {
                titleBuilder.append(" level ").append(level);
            }

            if (schools.size() != 0) {
                titleBuilder.append(" ");
                schools.forEach(el -> {
                    titleBuilder
                            .append(el.name().toLowerCase(Locale.ROOT));
                });
            }

            if (ritualOnly) {
                titleBuilder.append(" ritual");
            }

            titleBuilder
                    .append(" spells ");

            if (!clazz.equalsIgnoreCase("all")) {
                titleBuilder.append("spells for class");

                if (!yesClasses.isEmpty()) {
                    titleBuilder.append("es");
                }

                titleBuilder
                        .append(" ")
                        .append(WordUtils.capitalizeFully(clazz));

                if (!yesClasses.isEmpty()) {
                    if (yesClasses.size() == 1) {
                        titleBuilder.append(" and ").append(WordUtils.capitalizeFully(yesClasses.get(0)));
                    } else {
                        titleBuilder
                                .append(", ")
                                .append(
                                        WordUtils.capitalizeFully(String.join(", ", yesClasses.subList(0, yesClasses.size() - 1)))
                                ).append(" and ")
                                .append(WordUtils.capitalizeFully(yesClasses.get(yesClasses.size() - 1)));
                    }
                }
            }

            if (notClasses.size() > 0) {
                titleBuilder.append(", excluding ")
                            .append(
                                    WordUtils.capitalizeFully(String.join(", ", notClasses.size() == 1 ? notClasses : notClasses.subList(0, notClasses.size()-1)))
                            );
                if (notClasses.size() > 1)
                    titleBuilder.append(", and ")
                                .append(WordUtils.capitalizeFully(notClasses.get(notClasses.size()-1)));

                titleBuilder.append(" spells");
            }

            String title = titleBuilder.toString();


            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(pages.get(0))
                    .setFooter("Page 1 out of " + pages.size())
                    .setAuthor(user);

            Message msg = channel.sendMessage(embed).join();
            msg.addReactions(LEFT_ARROW, RIGHT_ARROW).join();

            AtomicInteger pageIndex = new AtomicInteger();

            ListenerManager<ReactionAddListener> listener = msg.addReactionAddListener(e -> {
                e.removeReaction();
                e.getUser().ifPresent((u) -> {
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
                            e.editMessage(new EmbedBuilder()
                                    .setTitle(title)
                                    .setDescription(pages.get(pageIndexx))
                                    .setFooter("Page " + (pageIndexx + 1) + " out of " + pages.size())
                                    .setAuthor(user)
                            );
                        }

                        pageIndex.set(pageIndexx);
                    }
                });
            });

            listener.removeAfter(5, TimeUnit.MINUTES).addRemoveHandler(() -> {
                msg.edit(new EmbedBuilder()
                        .setTitle(title)
                        .setDescription("Expired!")
                        .setAuthor(user)
                );
                msg.removeAllReactions();
            });

        } else {
            channel.sendMessage("Error: Unknown class");
        }
    }

    @Command(aliases = {"forecast", "fc"}, usage = "forecast", description = "Lists spells for that class and level")
    public void onForecast(String cmd, Server server, User user, TextChannel channel, Message message) {
        
    }

    @Command(aliases = {"autoforecast", "af"}, usage = "autoforecast <period in hours>", description = "Lists spells for that class and level")
    public void onBindAutoForecast(String cmd, String period, Server server, User user, TextChannel channel, Message message) {
        if (isManager(user, server)) {
            TextChannel ch = message.getMentionedChannels().size() == 0 ? channel : message.getMentionedChannels().get(0);

            ServerData data = getOrCreateData(server);

            data.forecastChannel = channel.getId();
            data.forecastPeriod = Integer.parseInt(period)*60*60*1000/4;

            Calendar time = Calendar.getInstance();
            time.set(Calendar.HOUR_OF_DAY, 8);
            time.set(Calendar.MINUTE, 0);
            time.set(Calendar.SECOND, 0);

            System.out.println(time.getTime());

            timer.schedule(
                    new ForecastTask(data),
                    time.getTime(),
                    data.forecastPeriod
            );

            onDataChange(server, data);

        } else {
            channel.sendMessage("Error: You don't have permission to do that");
        }
    }

}
