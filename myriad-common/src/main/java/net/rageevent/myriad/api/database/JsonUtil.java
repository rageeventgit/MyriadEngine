package net.rageevent.myriad.api.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtil {

    public final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

}
