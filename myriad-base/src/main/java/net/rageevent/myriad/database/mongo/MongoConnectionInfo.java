package net.rageevent.myriad.database.mongo;

public record MongoConnectionInfo(String url) {

    public static MongoConnectionInfo LOCALHOST = new MongoConnectionInfo("");
}
