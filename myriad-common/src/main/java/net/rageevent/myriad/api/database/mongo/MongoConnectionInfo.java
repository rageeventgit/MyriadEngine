package net.rageevent.myriad.api.database.mongo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record MongoConnectionInfo(String url, String username, String something, String password) {

    public static MongoConnectionInfo LOCALHOST = new MongoConnectionInfo("", "", "", "");

    public String toMongoUrl() {
        String encodedUser = URLEncoder.encode(username(), StandardCharsets.UTF_8);
        String encodedPass = URLEncoder.encode(password(), StandardCharsets.UTF_8);

        // 2. Separate protocol if it is already included in url()
        String protocol = "mongodb://";
        String host = url();
        if (host.startsWith("mongodb+srv://")) {
            protocol = "mongodb+srv://";
            host = host.substring("mongodb+srv://".length());
        } else if (host.startsWith("mongodb://")) {
            host = host.substring("mongodb://".length());
        }

        // 3. Remove trailing slash from host if present
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        // 4. Assemble the final connection string (assuming 'something' is the database name)
        String database = (something() != null && !something().isBlank())
                ? "/" + something()
                : "";

        return protocol + encodedUser + ":" + encodedPass + "@" + host + database;
    }
}
