package net.rageevent.myriad.api.player.store;

public interface SectionedDataContainer extends DataContainer {
    String section();

    @Override
    default String dotNotation() {
        if(section() == null || section().isBlank())
            return databaseName();

        return section() + "." + databaseName();
    }
}
