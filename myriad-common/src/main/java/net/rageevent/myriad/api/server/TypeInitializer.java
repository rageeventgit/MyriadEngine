package net.rageevent.myriad.api.server;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TypeInitializer {

    public static <T> List<T> initializeTypes(Class<T> type) {
        List<T> tempTypes = new ArrayList<>();
        try {
            // Reach into the jar and grab the index built by 'mvn install'
            InputStream input = ServerType.class.getResourceAsStream("/META-INF/jandex.idx");
            if (input != null) {
                Index index = new IndexReader(input).read();
                DotName targetInterface = DotName.createSimple(type.getName());

                // Scan for ALL enums implementing ServerType
                for (ClassInfo info : index.getAllKnownImplementors(targetInterface)) {
                    Class<?> clazz = Class.forName(info.name().toString());

                    if (clazz.isEnum()) {
                        T[] enumConstants = (T[]) clazz.getEnumConstants();
                        if (enumConstants != null) {
                            Collections.addAll(tempTypes, enumConstants);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Critical: Failed to auto-index ServerModeType enums!");
            e.printStackTrace();
        }
        return List.copyOf(tempTypes);
    }
}
