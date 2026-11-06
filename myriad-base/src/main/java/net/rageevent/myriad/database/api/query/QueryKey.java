package net.rageevent.myriad.database.api.query;

import org.jspecify.annotations.NonNull;

public record QueryKey(String key, String value) {

    public static QueryKey of(String combined) {
        int idx = combined.indexOf(':');
        if (idx < 0) {
            return new QueryKey(combined, null);
        }
        String k = combined.substring(0, idx);
        String v = combined.substring(idx + 1);
        return new QueryKey(k, v.isEmpty() ? null : v);
    }

    public @NonNull String namespaced() {
        return key + ":" + value;
    }

    public static QueryKey singlet(String key) {
        return new QueryKey(key, null);
    }
}
