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
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.util.event.ListenerManager;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

            cmdHandler.setDefaultPrefix("//");

            cmdHandler.registerCommand(new Main());
            cmdHandler.registerCommand(new HelpCommand(cmdHandler));

            bot.updateActivity("//help");
        }

        {
            ConnectionString mongoUri = env.containsKey("MONGODB_URI") ? new ConnectionString(env.get("MONGODB_URI")) : null;

            mongoClient = mongoUri != null ? MongoClients.create(mongoUri) : MongoClients.create();
            serverTomeDB = mongoClient.getDatabase(mongoUri != null && mongoUri.getDatabase() != null ? mongoUri.getDatabase() : "serverTomeDB");
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

        ArrayList<Tome.Spell> allSpells = new ArrayList<>();

        getServerTomes(server).forEach(tome -> allSpells.addAll(Arrays.asList(tome.spells)));
        allSpells.addAll(Arrays.asList(AvraeClient.getSRD()));

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

        SERVER_SPELL_MAP.remove(server.getId());
        SERVER_SPELL_MAP.put(server.getId(), map);

        return map;
    }

    public static List<Tome> getServerTomes(final Server server) {
        MongoCollection<Document> serverCol = serverTomeDB.getCollection("_"+server.getIdAsString());

        List<Tome> list = Streams.stream(serverCol.find()).map(el -> AvraeClient.getTome((String) el.get("id"))).collect(Collectors.toList());

        return list;
    }

    @Command(aliases = "invite", description = "Sends an invite for this bot.")
    public String onInviteCommand() {
        return bot.createBotInvite(new PermissionsBuilder().setAllowed(
                PermissionType.SEND_MESSAGES,
                PermissionType.ADD_REACTIONS,
                PermissionType.MANAGE_MESSAGES
        ).build());
    }


    @Command(aliases = "addtome", description = "Adds a tome to this bots database for this server")
    public void onAddTomeCommand(String[] args, Server server, TextChannel channel) throws Exception {
        MongoCollection<Document> serverCol = serverTomeDB.getCollection("_"+server.getIdAsString());

        if (AvraeClient.getTome(args[0]) == null) {
            channel.sendMessage(new EmbedBuilder()
                    .setDescription("Invalid tome id!")
            );
            return;
        }

        Document obj = new Document("id", args[0]);

        if (serverCol.countDocuments(obj) == 0) {
            Tome tome = AvraeClient.getTome(args[0]);

            serverCol.insertOne(obj);
            channel.sendMessage(new EmbedBuilder()
                .setTitle(tome.name)
                .setDescription("Tome added!")
                .setUrl("https://avrae.io/homebrew/spells/"+tome.id)
                .setImage(tome.image)
            );

            SERVER_SPELL_MAP.remove(server.getId());
            SERVER_SPELL_MAP.put(server.getId(), buildSpellMap(server));
        } else {
            channel.sendMessage(new EmbedBuilder()
                .setDescription("Tome is already added!")
            );
        }
    }

    @Command(aliases = {"removetome", "rmtome"}, description = "Removes a tome from this bots database for this server")
    public void onRemoveTome(String[] args, Server server, TextChannel channel) throws Exception {
        MongoCollection<Document> serverCol = serverTomeDB.getCollection("_"+server.getIdAsString());

        Document obj = new Document("id", args[0]);

        Tome tome = AvraeClient.getTome(args[0]);

        if (serverCol.countDocuments(new BsonDocument()) == 0) {
            channel.sendMessage(new EmbedBuilder()
                    .setTitle(tome.name)
                    .setDescription("Tome is not added!")
            );
        } else {
            serverCol.deleteOne(obj);
            channel.sendMessage(new EmbedBuilder()
                    .setTitle(tome.name)
                    .setDescription("Tome removed!")
            );

            SERVER_SPELL_MAP.remove(server.getId());
            SERVER_SPELL_MAP.put(server.getId(), buildSpellMap(server));
        }
    }

    @Command(aliases = "listtomes", description = "Adds a tome to this bots database for this server")
    public void onListTomes(String[] args, Server server, TextChannel channel) throws Exception {
        channel.sendMessage(new EmbedBuilder()
            .setDescription(getServerTomes(server).stream().map(tome -> tome.name+" (https://avrae.io/homebrew/spells/"+tome.id+")").collect(Collectors.joining("\n")))
        );
    }

    @Command(aliases = {"spelllist", "spells", "sl"}, description = "Lists spells for that class and level")
    public void onSpellListCommand(String[] args, Server server, TextChannel channel) throws Exception {
        String clazz = args[0];
        int level = Integer.parseInt(args.length == 2 ? args[1] : "-1");

        HashMap<String, ArrayList<Tome.Spell>> serverMap = SERVER_SPELL_MAP.get(server.getId());

        if (serverMap == null)
            serverMap = buildSpellMap(server);
            SERVER_SPELL_MAP.put(server.getId(), serverMap);

        List<Tome.Spell> spells = serverMap.get(clazz.toLowerCase());

        if (level != -1) {
            spells = spells.stream().filter(s -> s.level == level).collect(Collectors.toList());
        }

        if (spells != null) {
            String desc = spells.stream().map(spell -> spell.name).collect(Collectors.joining("\n"));



            AtomicInteger counter = new AtomicInteger();

            ArrayList<String> pages = new ArrayList<>(Arrays.stream(desc.split("\n"))
                    /*.mapToObj(i -> String.valueOf((char)i))*/
                    .collect(Collectors.groupingBy(ch -> Math.floor(counter.getAndIncrement() / 20D), Collectors.joining("\n")))
                    .values()
            );
            AtomicInteger pageIndex = new AtomicInteger();

            Message msg = channel.sendMessage(new EmbedBuilder()
                .setTitle("Level "+level+" spells for class "+clazz)
                .setDescription(pages.get(0))
                .setFooter("Page 1 out of "+pages.size())
            ).join();

            msg.addReactions(LEFT_ARROW, RIGHT_ARROW).join();

            ListenerManager<ReactionAddListener> listener = msg.addReactionAddListener(e -> {
               if (e.getUser() != bot.getYourself()) {
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

                       e.editMessage(new EmbedBuilder()
                               .setTitle("Level " + level + " spells for class " + clazz)
                               .setDescription(pages.get(pageIndexx))
                               .setFooter("Page " + (pageIndexx + 1) + " out of " + pages.size())
                       );

                       pageIndex.set(pageIndexx);
                   }
               }
            });

            listener.removeAfter(5, TimeUnit.MINUTES).addRemoveHandler(() -> {
                msg.edit(new EmbedBuilder()
                        .setTitle("Level "+level+" spells for class "+clazz)
                        .setDescription("Expired!")
                );
                msg.removeAllReactions();
            });

        }
    }

}
