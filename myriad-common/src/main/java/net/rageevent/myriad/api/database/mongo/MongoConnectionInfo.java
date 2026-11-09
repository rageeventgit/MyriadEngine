package net.rageevent.myriad.api.database.mongo;

public record MongoConnectionInfo(String url, String username, String something, String password) {

    public static MongoConnectionInfo LOCALHOST = new MongoConnectionInfo("", "", "", "");
}
