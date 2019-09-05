package uk.org.fancy.AndroidTvHomeKit;

import android.util.Log;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

public class PollThread extends Thread {
    private static final String TAG = "HomeKit:PollThread";
    private boolean run = false;
    private final List<PollInterface> pollList = new LinkedList<PollInterface>();

    public PollThread() {
        super();
        setDaemon(true);
    }

    public void run() {
        Log.i(TAG, "Starting poll thread");

        run = true;

        while (run) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted", e);
            }
            if (!run) break;

            for (PollInterface poll: pollList) {
                try {
                    poll.poll();
                } catch (Throwable e) {
                    Log.e(TAG, "Error polling " + poll.toString(), e);
                }
            }
        }

        Log.i(TAG, "Poll thread completed");
    }

    public synchronized void setShouldStop() {
        Log.i(TAG, "Waiting for poll thread to complete");
        run = false;
    }

    public interface PollInterface {
        public void poll() throws Exception;
    }

    public synchronized void add(PollInterface poll) {
        pollList.add(poll);
    }

    public synchronized void remove(PollInterface poll) {
        pollList.remove(poll);
    }
}
