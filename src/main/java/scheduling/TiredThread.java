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
     * Uses a blocking put to ensure the task is accepted and the worker is not lost.
     */
    public void newTask(Runnable task) {
        try {
            // put() waits until there is space, ensuring the worker is never "lost"
            // due to a momentary race condition.
            this.handoff.put(task); 
        } catch (InterruptedException e) {
            // Restore the interrupt status
            Thread.currentThread().interrupt();
            // Wrap the checked exception in a RuntimeException so we don't break the signature
            throw new RuntimeException("Worker " + id + " was interrupted while accepting a task", e);
        }
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
       this.handoff.offer(POISON_PILL);
    }

@Override
    public void run() {
        try {
            // Loop as long as the thread is kept alive
            while (alive.get()) {
                // 1. Wait for a task (blocking operation)
                Runnable task = handoff.take();

                // 2. Check for Poison Pill (Shutdown signal)
                if (task == POISON_PILL) {
                    return;
                }

                // 3. Measure idle duration before starting work
                long now = System.nanoTime();
                long idleDuration = now - idleStartTime.get();
                timeIdle.addAndGet(idleDuration);

                // 4. Mark as busy and start measuring work time
                busy.set(true);
                long startingTime = System.nanoTime();

                // --- CRITICAL CHANGE START ---
                // We wrap the task execution in a try-catch block.
                // This ensures that if the task throws an exception, the worker thread
                // does NOT die and can continue to process the next tasks in the queue.
                try {
                    task.run();
                } catch (Throwable t) {
                    System.err.println("Worker " + id + " failed to execute task: " + t.getMessage());
                }
                // --- CRITICAL CHANGE END ---

                // 5. Update work metrics
                long workDuration = System.nanoTime() - startingTime;
                timeUsed.addAndGet(workDuration);

                // 6. Reset state for the next loop
                busy.set(false);
                idleStartTime.set(System.nanoTime());
            }

        } catch (InterruptedException e) {
            // Handle interruption (usually happens during shutdown if thread is waiting in take())
            this.alive.set(false);
            Thread.currentThread().interrupt(); // Restore interrupt status
        } catch (Exception e) {
            // Handle unexpected crashes that break the loop
            this.alive.set(false);
            System.err.println("Worker " + id + " encountered a fatal error and terminated: " + e.getMessage());
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