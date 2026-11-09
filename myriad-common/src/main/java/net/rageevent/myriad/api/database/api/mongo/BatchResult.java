package net.rageevent.myriad.api.database.api.mongo;

public record BatchResult(int items, int itemsChanged) {

    public boolean failed(){
        return itemsChanged != items; //If this somehow goes OVER the items amount, I've severely fucked up.
    }

}
