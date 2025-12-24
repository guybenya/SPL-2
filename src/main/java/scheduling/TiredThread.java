package scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TiredThread extends Thread implements Comparable<TiredThread> {

    private static final Runnable POISON_PILL = () -> {}; // Special task to signal shutdown

    private final int id; // Worker index assigned by the executor
    private final double fatigueFactor; // Multiplier for fatigue calculation

    private final AtomicBoolean alive = new AtomicBoolean(true); // Indicates if the worker should keep running

    // Single-slot handoff queue; executor will put tasks here
    private final BlockingQueue<Runnable> handoff = new ArrayBlockingQueue<>(1);

    private final AtomicBoolean busy = new AtomicBoolean(false); // Indicates if the worker is currently executing a task

    private final AtomicLong timeUsed = new AtomicLong(0); // Total time spent executing tasks
    private final AtomicLong timeIdle = new AtomicLong(0); // Total time spent idle
    private final AtomicLong idleStartTime = new AtomicLong(0); // Timestamp when the worker became idle

    public TiredThread(int id, double fatigueFactor) {
        this.id = id;
        this.fatigueFactor = fatigueFactor;
        this.idleStartTime.set(System.nanoTime());
        setName(String.format("FF=%.2f", fatigueFactor));
    }

    public int getWorkerId() {
        return id;
    }

    public double getFatigue() {
        return fatigueFactor * timeUsed.get();
    }

    public boolean isBusy() {
        return busy.get();
    }

    public long getTimeUsed() {
        return timeUsed.get();
    }

    public long getTimeIdle() {
        return timeIdle.get();
    }

    /**
     * Assign a task to this worker.
     * This method is non-blocking: if the worker is not ready to accept a task,
     * it throws IllegalStateException.
     */
    public void newTask(Runnable task) {
       // TODO
       // check if the thread can get a task right now
       if (isBusy()) {
            throw new IllegalStateException("thread is busy!");
       }
       // insert the task
       this.handoff.add(task);
    }

    /**
     * Request this worker to stop after finishing current task.
     * Inserts a poison pill so the worker wakes up and exits.
     */
    public void shutdown() {
       // TODO
       // indicate the thread: finish your task and do not wait for a new one
       this.alive.set(false);
       // insert the special task to the queue - deal correctly with a busy and not busy thread
       this.handoff.add(POISON_PILL);
    }

    @Override
    public void run() {
       // TODO
       // as long as the thread is alive try to assign it a task
       try {
            // using "while" instead of "if" due to the "try-catch" mechanizem
            while (alive.get() == true) {
                // take the task from the queue
                Runnable task = handoff.take();
                // check if the task if the poison pill and stop running if it is
                if (task == POISON_PILL) {
                    return;
                }
                // calcultae the idle duration
                long now = System.nanoTime();
                long idleDuration = now - idleStartTime.get();
                timeIdle.addAndGet(idleDuration);

                // change the "busy" flag and measure the actual work time
                busy.set(true);
                long startingTime = System.nanoTime();

                // run the given task
                task.run();
                long workDuration = System.nanoTime() - startingTime;
                timeUsed.addAndGet(workDuration);

                // prepare the thread to the next loop
                busy.set(false);
                idleStartTime.set(System.nanoTime());


            }
        
       } catch (Exception e) {
        // TODO: handle exception
        this.alive.set(false); // thread is not alive in this case
       }
    }

    @Override
    public int compareTo(TiredThread o) {
        // TODO
        // compare the fatigue factor for both threads

        return Double.compare(this.getFatigue(), o.getFatigue()); // calling to getFatigue inside this method updates the fatigue value for each thread
                                                                  // the priority queue sorts the threads by this value
                                                                

        // (-1) if this is less tired then other, 0 if equally tired and (-1) else. 
    }
}