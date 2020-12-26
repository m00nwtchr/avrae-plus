package io.github.lmarianski.avraeplus;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

public class GlobalData {

    private MongoCollection<Document> globalData;

    private Document statsFilter = new Document("type", "stats");
    private Document statsDoc;

    public static long DAY_MS = 86400000;

    private Calendar calendar = new Calendar.Builder().build();

    public GlobalData() {
        globalData = Main.db.getCollection("globalData");

        statsDoc = findOrCreate(statsFilter);

        if (!statsDoc.containsKey("hitsThisMonth")) {
            statsDoc.append("hitsThisMonth", 0);
        }
        Main.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
                    int hits = statsDoc.getInteger("hitsThisMonth");
                    statsDoc.append("hitsLastMonth", hits);
                    statsDoc.append("hitsThisMonth", 0);
                    update();
                }
            }
        }, 0, DAY_MS);
    }

    public Document findOrCreate(Document doc) {
        if (globalData.countDocuments(doc) == 0) {
            globalData.insertOne(doc);
            return doc;
        } else {
            return globalData.find(doc).first();
        }
    }

    public synchronized void update() {
//        globalData.insertMany(Arrays.asList(
//                statsDoc
//        ));
        globalData.findOneAndReplace(statsFilter, statsDoc);
    }

    public synchronized void incrementHits() {
        int hits = statsDoc.getInteger("hitsThisMonth");
        statsDoc.append("hitsThisMonth", hits+1);
        update();
    }

}
