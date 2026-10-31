package net.rageevent.myriad.interals;

import lombok.experimental.UtilityClass;

import java.lang.management.ManagementFactory;
import java.util.List;

@UtilityClass
public class FlagParser {

    public String getFlag(String flagName) {
        List<String> jvmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

        for (String jvmArgument : jvmArguments) {
            if (jvmArgument.startsWith("-D"  + flagName)) {
                String[] parts = jvmArgument.split("=", 2);

                if (parts.length > 1)
                    return parts[1];
            }
        }
        return null;
    }

}
