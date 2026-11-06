package net.rageevent.myriad.database.api.query;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueryLane {

    private final ExecutorService[] lanes;

    /**
     * A custom thread handler for {@link SequencedQuery} and such.
     * <p>
     * Allows for a locking a query key. See {@link net.rageevent.work.sql.protocol.bid.PostBidQuery}.
     * Queries are typically executed as what's first and allow multiple to write at once,
     * this however changes the execution to allow one query to finish before the next is processed.
     *
     * @param laneCount The amount of threads that are allowed to be used.
     * @param name Name that is going to be used for threads.
     */
    public QueryLane(int laneCount, String name){
        if(laneCount <= 0) throw new IllegalArgumentException("Lane count cannot be 0 or less.");
        this.lanes = new ExecutorService[laneCount];

        for(int i = 0; i < laneCount; i++){
            int idx = i;
            lanes[i] = Executors.newSingleThreadExecutor(r -> {
                Thread t  = new Thread(r, name + "-" + idx);
                t.setDaemon(true);
                return t;
            });
        }
    }
    public Executor executor(UUID key){
        int index = (key.hashCode() & 0x7fffffff) % lanes.length; //Converts the unique key into an index;
        return lanes[index];
    }
    public void shutdown(){
        for (ExecutorService lane : lanes) {
            lane.shutdown();
        }
    }
}
