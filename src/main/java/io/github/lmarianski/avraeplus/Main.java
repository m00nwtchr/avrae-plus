package io.github.lmarianski.avraeplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpell;
import io.github.lmarianski.avraeplus.data.interfaces.spells.ISpellCollection;
import io.github.lmarianski.avraeplus.data.sources.SourceManager;
import io.github.lmarianski.avraeplus.data.sources.avrae.AvraeClient;
import io.github.lmarianski.avraeplus.data.SpellSchool;
import io.github.lmarianski.avraeplus.data.sources.avrae.spells.Tome;
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
import org.javacord.api.listener.server.ServerJoinListener;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
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
    public static MongoCollection<ServerData> serversCollection;

    public static Timer timer = new Timer();

    public static final Type MAP_STRING_STRING_TYPE = new TypeToken<Map<String, String>>() {}.getType();

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
            ISpellCollection.Codec spellCollectionCodec = new ISpellCollection.Codec();

            CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    CodecRegistries.fromCodecs(tomeCodec, spellCollectionCodec),
                    CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            mongoClient = mongoUri != null ? MongoClients.create(mongoUri) : MongoClients.create();
            db = mongoClient.getDatabase(mongoUri != null && mongoUri.getDatabase() != null ? mongoUri.getDatabase() : "serverTomeDB");
            serversCollection = db.getCollection("servers", ServerData.class).withCodecRegistry(codecRegistry);

            bot.getServers().forEach(Main::getOrCreateData);
//            Main.SERVER_DATA_MAP.values().forEach(ServerData::getInvite);
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

    private static ServerData _fetch(long id, Server server) {
            Document doc = new Document("serverId", id);
            ServerData data;

            if (serversCollection.countDocuments(doc) == 0) {
                data = new ServerData(server);

                serversCollection.insertOne(data);
            } else {
                data = Objects.requireNonNull(serversCollection.find(doc).limit(1).first());
                data.server = server;
                data.serverId = id;
            }

            data.buildSpellMap();

            return data;
    }

    public static ServerData getOrCreateData(Server server) {
        return getOrCreateData(server, false);
    }

    public static synchronized ServerData getOrCreateData(Server server, boolean fetch) {
        synchronized (SERVER_DATA_MAP) {
            return fetch ? SERVER_DATA_MAP.compute(server.getId(), (id,a) -> _fetch(id, server)) : SERVER_DATA_MAP.computeIfAbsent(server.getId(), id -> _fetch(id, server));
        }
    }

    @Command(aliases = {"rebuild"}, description = "Rebuilds this server's spell database. (Use this to pull changes)")
    public void rebuildSpellMap(Server server, TextChannel channel, User user) {
        if (isManager(user, server)) {
            ServerData serverData = getOrCreateData(server, true);

            Message msg = channel.sendMessage("Rebuilding DB...").join();
            serverData.buildSpellMap();

            long spellCount = serverData.spellMap.get("all").size();

            msg.edit("Done, " + serverData.spellMap.size() + " classes found, with a total of " + spellCount + " spells");
        } else {
            channel.sendMessage("You don't have permission to do that!");
        }
    }

    public static void onDataChange(Server server, ServerData data) {

        serversCollection.findOneAndReplace(new Document("serverId", server.getId()), data);

    }


    public static boolean addTome(Server server, ISpellCollection tome) {
        ServerData data = getOrCreateData(server);

        if (!data.tomes.contains(tome)) {
            data.tomes.add(tome);
            onDataChange(server, data);
            return true;
        }
        return false;
    }

    public static boolean removeTome(Server server, ISpellCollection tome) {
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
    public void onAddTomeCommand(String name, String tomeid, User user, Server server, TextChannel channel) throws IOException {
        if (isManager(user, server)) {
            ISpellCollection tome = SourceManager.getTome(tomeid);


//            ServerData serverData = getOrCreateData(server);

            if (tome == null) {
                channel.sendMessage(new EmbedBuilder()
                        .setDescription("Invalid tome id!")
                );
                return;
            }

            if (addTome(server, tome)) {
                channel.sendMessage(new EmbedBuilder()
                        .setTitle(tome.getName())
                        .setDescription("Tome added!")
                        .setUrl("https://avrae.io/homebrew/spells/" + tome.getId())
                        .setImage(tome.getImage())
                );

                getOrCreateData(server, true);
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
            ISpellCollection tome = SourceManager.getTome(args[0]);
            ServerData serverData = getOrCreateData(server);

            if (tome == null) {
                channel.sendMessage(new EmbedBuilder()
                        .setDescription("Invalid tome id!")
                );
                return;
            }

            if (removeTome(server, tome)) {
                channel.sendMessage(new EmbedBuilder()
                        .setTitle(tome.getName())
                        .setDescription("Tome removed!")
                );

                getOrCreateData(server, true);
            } else {
                channel.sendMessage(new EmbedBuilder()
                        .setTitle(tome.getName())
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
                                .map(tome -> tome.getName() + " ("+ (isURL(tome.getId()) ? "" : "https://avrae.io/homebrew/spells/") + tome.getId() + ")")
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


    @Command(aliases = {"spelllist", "spells", "sl"}, usage = "sl <class> [level] [--ritual] [--!<classname>] [--<schoolname>] [-S<searchTerm>]", description = "Lists spells for that class and level")
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

        List<ISpell> spells = serverData.spellMap.get(clazz);

        if (spells != null) {
            Stream<ISpell> spellStream = spells.stream().sorted(Comparator.comparing(ISpell::getLevel).thenComparing(ISpell::getName));//.filter(spellFilter(level != -1, ritualOnly, );

            if (level != -1) {
                spellStream = spellStream.filter(s -> s.getLevel() == level);
            }

            if (ritualOnly) {
                spellStream = spellStream.filter(ISpell::isRitual);
            }

            List<SpellSchool> schools = args.stream()
                    .filter(s -> s.startsWith("--"))
                    .map(s -> s.substring(2).toLowerCase(Locale.ROOT))
                    .map(SpellSchool::get)
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

            List<String> searchTerms = args.stream()
                    .filter(s -> s.startsWith("-S"))
                    .map(s -> s.substring(2).toLowerCase(Locale.ROOT))
                    .collect(Collectors.toList());

            if (notClasses.size() != 0) {
                spellStream = spellStream
                        .filter(s -> notClasses.stream().noneMatch(st -> s.getClassString().toLowerCase(Locale.ROOT).contains(st)));
            }

            if (yesClasses.size() != 0) {
                spellStream = spellStream
                        .filter(s -> yesClasses.stream().allMatch(st -> s.getClassString().toLowerCase(Locale.ROOT).contains(st)));
            }

            if (schools.size() != 0) {
                spellStream = spellStream
                        .filter(s -> schools.contains(s.getSchool()));
            }

            if (searchTerms.size() != 0) {
                spellStream = spellStream.filter(s->Arrays.stream(s.getDescription().split(" ")).anyMatch(searchTerms::contains));
            }

            List<String> pages;

            if (level == -1) {
                ArrayList<Map.Entry<Integer, List<ISpell>>> l = new ArrayList<>(spellStream.collect(Collectors.groupingBy(ISpell::getLevel, Collectors.toList())).entrySet());
                ArrayList<String> strings = new ArrayList<>();
                l.forEach((el) -> {
                    el.getValue().sort(Comparator.comparing(ISpell::getName));
                    strings.add("**Level " + el.getKey() + " Spells**");
                    strings.addAll(el.getValue().stream().map(ISpell::getName).collect(Collectors.toList()));
                });

                pages = Util.paginate(strings.stream(), 20).collect(Collectors.toList());
            } else {
                pages = Util.paginate(spellStream.map(ISpell::getName), 20).collect(Collectors.toList());
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

            if (searchTerms.size() > 0) {
                titleBuilder.append(", searching for \"").append(String.join("\" OR \"", searchTerms)).append("\"");
            }

            String title = titleBuilder.toString();


            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
//                    .setDescription(pages.get(0))
//                    .setFooter("Page 1 out of " + pages.size())
                    .setAuthor(user);

            Message msg = Util.sendPaginatedMessage(channel, user, embed, pages).join();
//
//            Message msg = channel.sendMessage(embed).join();
//            msg.addReactions(LEFT_ARROW, RIGHT_ARROW).join();
//
//            AtomicInteger pageIndex = new AtomicInteger();
//
//            ListenerManager<ReactionAddListener> listener = msg.addReactionAddListener(e -> e.getUser().ifPresent((u) -> {
//                if (!u.isYourself()) e.removeReaction();
//                if (u.equals(user)) {
//                    int pageIndexx = pageIndex.get();
//
//                    if (e.getEmoji().asUnicodeEmoji().isPresent()) {
//                        switch (e.getEmoji().asUnicodeEmoji().get()) {
//                            case (LEFT_ARROW):
//                                pageIndexx--;
//                                break;
//                            case (RIGHT_ARROW):
//                                pageIndexx++;
//                                break;
//                        }
//
//                        pageIndexx = (pageIndexx < 0) ? 0 : ((pageIndexx >= pages.size()) ? (pages.size() - 1) : pageIndexx);
//
//                        if (pageIndex.get() != pageIndexx) {
//                            e.editMessage(new EmbedBuilder()
//                                    .setTitle(title)
//                                    .setDescription(pages.get(pageIndexx))
//                                    .setFooter("Page " + (pageIndexx + 1) + " out of " + pages.size())
//                                    .setAuthor(user)
//                            );
//                        }
//
//                        pageIndex.set(pageIndexx);
//                    }
//                }
//            }));
//
//            listener.removeAfter(5, TimeUnit.MINUTES).addRemoveHandler(() -> {
//                msg.edit(new EmbedBuilder()
//                        .setTitle(title)
//                        .setDescription("Expired!")
//                        .setAuthor(user)
//                );
//                msg.removeAllReactions();
//            });

        } else {
            channel.sendMessage("Error: Unknown class");
        }
    }

    @Command(aliases = {"support"}, usage = "support", description = "For when the bot is going crazy")
    public void onSupport(String[] cmd, User user, TextChannel channel) {
        User owner = bot.getOwner().join();
//        System.out.println(String.join(" ", cmd));
        channel.sendMessage("Hi! "+owner.getMentionTag() + " made this bot, if he's here he'll get to you ASAP, otherwise you can shoot him a DM! (GMT+1 Timezone)").join();
    }

//
//    @Command(aliases = {"forecast", "fc"}, usage = "forecast", description = "Lists spells for that class and level")
//    public void onForecast(String cmd, Server server, User user, TextChannel channel, Message message) {
//
//    }

//   ive 
}
