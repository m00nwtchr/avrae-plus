package io.github.lmarianski.avraeplus;

import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;
import io.github.lmarianski.avraeplus.avrae.AvraeClient;
import io.github.lmarianski.avraeplus.avrae.homebrew.spells.Tome;
import org.apache.commons.text.WordUtils;
import org.bson.BsonDocument;
import org.bson.Document;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.util.event.ListenerManager;

import java.lang.reflect.Array;
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
    public static MongoDatabase serverTomeDB;
    public static Gson gson;

    public static final String LEFT_ARROW = "⬅️";
    public static final String RIGHT_ARROW = "➡️";

    public static final HashMap<Long, HashMap<String, ArrayList<Tome.Spell>>> SERVER_SPELL_MAP = new HashMap<>();

    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();

        gson = new GsonBuilder()
                .create();

        {
            bot = new DiscordApiBuilder().setToken(DISCORD_BOT_TOKEN).login().join();
            cmdHandler = new JavacordHandler(bot);

            cmdHandler.setDefaultPrefix(env.getOrDefault("PREFIX", "!!"));

            cmdHandler.registerCommand(new Main());
            cmdHandler.registerCommand(new HelpCommand(cmdHandler));

            bot.updateActivity(cmdHandler.getDefaultPrefix()+"help");
        }

        {
            ConnectionString mongoUri = env.containsKey("MONGODB_URI") ? new ConnectionString(env.get("MONGODB_URI")) : null;

            mongoClient = mongoUri != null ? MongoClients.create(mongoUri) : MongoClients.create();
            serverTomeDB = mongoClient.getDatabase(mongoUri != null && mongoUri.getDatabase() != null ? mongoUri.getDatabase() : "serverTomeDB");

            bot.getServers().forEach(Main::getServerTomes);
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

    public static HashMap<String, ArrayList<Tome.Spell>> buildSpellMap(Server server) {
        HashMap<String, ArrayList<Tome.Spell>> map = new HashMap<>();


        long time = System.currentTimeMillis();
        List<Tome> tomes = getServerTomes(server);

        tomes.add(AvraeClient.getSRD());

        ArrayList<Tome.Spell> allSpells = tomes.stream()
                .map(tome -> tome.spells)
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));

        allSpells.forEach(spell -> {
            for (String clazz : spell.classes.split(",")) {
                //if (clazz.endsWith(",")) clazz = clazz.substring(0, clazz.length() - 2);

                clazz = clazz.trim().toLowerCase();

                ArrayList<Tome.Spell> list = map.computeIfAbsent(clazz, k -> new ArrayList<>());
                list.add(spell);
            }
        });

        map.forEach((clazz, spells) -> {
            spells.sort(Comparator.comparing(a -> a.name));
        });

        //SERVER_SPELL_MAP.remove(server.getId());
        //SERVER_SPELL_MAP.put(server.getId(), map);

        return map;
    }

    private static final HashMap<Long, ArrayList<Tome>> SERVER_TOME_MAP = new HashMap<>();

    public static ArrayList<Tome> getServerTomes(final Server server) {
        return SERVER_TOME_MAP.computeIfAbsent(server.getId(), Main::_getServerTomes);
    }

    private static ArrayList<Tome> _getServerTomes(final long id) {
        MongoCollection<Document> serverCol = serverTomeDB.getCollection("_"+id);

        return Streams.stream(serverCol.find())
                .map(el -> AvraeClient.getTome((String) el.get("id")))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static boolean addTome(Server server, Tome tome) {
//        if (AvraeClient.getTome(tome) == null) {
//            throw new IllegalArgumentException("Invalid tome id");
//        }

        MongoCollection<Document> serverCol = serverTomeDB.getCollection("_"+server.getIdAsString());
        Document obj = new Document("id", tome);

        if (serverCol.countDocuments(obj) == 0) {
            serverCol.insertOne(obj);

            ArrayList<Tome> tomes = SERVER_TOME_MAP.computeIfAbsent(server.getId(), Main::_getServerTomes);
            tomes.add(tome);
            SERVER_TOME_MAP.put(server.getId(), tomes);
            return true;
        }
        return false;
    }

    public static boolean removeTome(Server server, Tome tome) {
//        if (AvraeClient.getTome(tome) == null) {
//            throw new IllegalArgumentException("Invalid tome id");
//        }

        MongoCollection<Document> serverCol = serverTomeDB.getCollection("_"+server.getIdAsString());
        Document obj = new Document("id", tome.id);

        if (serverCol.countDocuments(obj) == 0) {
            return false;
        } else {
            serverCol.deleteOne(obj);

            ArrayList<Tome> tomes = SERVER_TOME_MAP.computeIfAbsent(server.getId(), Main::_getServerTomes);
            tomes.add(tome);
            SERVER_TOME_MAP.put(server.getId(), tomes);
            return true;
        }
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException | NullPointerException e) {
            return false;
        }
        return true;
    }

    @Command(aliases = "invite", description = "Sends an invite for this bot.")
    public String onInviteCommand() {
        return bot.createBotInvite(new PermissionsBuilder().setAllowed(
                PermissionType.SEND_MESSAGES,
                PermissionType.ADD_REACTIONS,
                PermissionType.MANAGE_MESSAGES
        ).build());
    }

    @Command(aliases = "addtome", usage = "addtome <tomeid>", description = "Adds a tome to this bots database for this server")
    public void onAddTomeCommand(String name, String tomeid, Server server, TextChannel channel) {
        Tome tome = AvraeClient.getTome(tomeid);

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
                .setUrl("https://avrae.io/homebrew/spells/"+tome.id)
                .setImage(tome.image)
            );

            //SERVER_SPELL_MAP.remove(server.getId());
            SERVER_SPELL_MAP.put(server.getId(), buildSpellMap(server));
        } else {
            channel.sendMessage(new EmbedBuilder()
                .setDescription("Tome is already added!")
            );
        }
    }

    @Command(aliases = {"removetome", "rmtome"}, usage = "rmtome <tomeid>", description = "Removes a tome from this bots database for this server")
    public void onRemoveTome(String[] args, Server server, TextChannel channel) throws Exception {
        Tome tome = AvraeClient.getTome(args[0]);

        if (tome == null) {
            channel.sendMessage(new EmbedBuilder()
                    .setDescription("Invalid tome id!")
            );
            return;
        }

        if (removeTome(server, tome)) {
            channel.sendMessage(new EmbedBuilder()
                    .setTitle(tome.name)
                    .setDescription("Tome is not added!")
            );
        } else {
            channel.sendMessage(new EmbedBuilder()
                    .setTitle(tome.name)
                    .setDescription("Tome removed!")
            );

            //SERVER_SPELL_MAP.remove(server.getId());
            SERVER_SPELL_MAP.put(server.getId(), buildSpellMap(server));
        }
    }

    @Command(aliases = {"listtomes", "lstomes"}, description = "Adds a tome to this bots database for this server")
    public void onListTomes(Server server, TextChannel channel) {
        channel.sendMessage(new EmbedBuilder()
            .setDescription(
                    getServerTomes(server).stream()
                            .map(tome -> tome.name+" (https://avrae.io/homebrew/spells/"+tome.id+")")
                            .collect(Collectors.joining("\n"))
            ));
    }

    @Command(aliases = {"spelllist", "spells", "sl"}, usage = "sl <class> [level] [--ritual]", description = "Lists spells for that class and level")
    public void onSpellListCommand(String[] argz, Server server, TextChannel channel, User user) {
        ArrayList<String> args = new ArrayList<>(Arrays.asList(argz));

        String clazz = args.get(0).toLowerCase();

        int tmpLvl = -1;
        try {
            tmpLvl = Integer.parseInt(args.get(1));
        } catch (Exception ignored) {}
        int level = tmpLvl;

        boolean ritualOnly = args.contains("--ritual");

        HashMap<String, ArrayList<Tome.Spell>> serverMap = SERVER_SPELL_MAP.computeIfAbsent(server.getId(), id -> buildSpellMap(bot.getServerById(id).get()));

        List<Tome.Spell> spells = serverMap.get(clazz);

        if (spells != null) {
            Stream<Tome.Spell> spellStream = spells.stream();

            if (level != -1) {
                spellStream = spellStream.filter(s -> s.level == level);
            }

            if (ritualOnly) {
                spellStream = spellStream.filter(s -> s.ritual);
            }

            if (args.contains("--!wizard")) {
                spellStream = spellStream.filter(s -> !s.classes.toLowerCase().contains("wizard"));
            }

            ArrayList<String> pages;

            if (level == -1) {
                ArrayList<Map.Entry<Integer, List<Tome.Spell>>> l = new ArrayList<>(spellStream.collect(Collectors.groupingBy(spell -> spell.level, Collectors.toList())).entrySet());
                ArrayList<String> strings = new ArrayList<>();
                l.forEach((el) -> {
                    el.getValue().sort(Comparator.comparing(a -> a.name));
                    strings.add("**Level "+el.getKey()+" Spells**");
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

            AtomicInteger pageIndex = new AtomicInteger();

            String classCapit = WordUtils.capitalizeFully(clazz);
            String title = level == -1 ? "All"+(ritualOnly ? " ritual" : "")+" spells for class "+classCapit : "Level "+level+(ritualOnly ? " ritual" : "")+" spells for class "+classCapit;

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(pages.get(0))
                    .setFooter("Page 1 out of "+pages.size())
                    .setAuthor(user);

            Message msg = channel.sendMessage(embed).join();

            msg.addReactions(LEFT_ARROW, RIGHT_ARROW).join();

            ListenerManager<ReactionAddListener> listener = msg.addReactionAddListener(e -> {
               if (e.getUser() == user) {
                   e.removeReaction();

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
               } else if (e.getUser() != bot.getYourself()) {
                   e.removeReaction();
               }
            });

            listener.removeAfter(5, TimeUnit.MINUTES).addRemoveHandler(() -> {
                msg.edit(new EmbedBuilder()
                        .setTitle(title)
                        .setDescription("Expired!")
                        .setAuthor(user)
                );
                msg.removeAllReactions();
            });

        }
    }

}
