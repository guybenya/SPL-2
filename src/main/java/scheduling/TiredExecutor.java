package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        // TODO
        this.workers = new TiredThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            // create a new thread with a unique id number and a random fatigue factor
            TiredThread toInsert = new TiredThread(i, 0.5 + Math.random());
            // insert it to workers array
            workers[i] = toInsert;
            // insert it to the minHeap - the thread is idle, we just created it
            idleMinHeap.add(toInsert);
            // ensure the thread is in stand by mode - make the system allocate CPU resources for it
            toInsert.start();
        }
    }

    public void submit(Runnable task) {
        // Increment the count of currently executing tasks
        this.inFlight.incrementAndGet();

        try {
            // Retrieve the least fatigued idle thread (blocks if none are available)
            TiredThread w1 = this.idleMinHeap.take();

            // Create a wrapper to ensure the worker is returned to the heap after execution
            Runnable wrapper = () -> {
                try {
                    task.run(); // Execute the actual task
                }
                finally {
                    this.idleMinHeap.add(w1); // Return the worker to the idle pool

                    // Decrement task count and notify if all tasks are complete
                    if (this.inFlight.decrementAndGet() == 0) {
                        synchronized (this) {
                            this.notifyAll();
                        }
                    }
                }
            };

            w1.newTask(wrapper); // Assign the wrapped task to the worker

        } 
        catch (Exception e) {
            // --- Added logging to reveal the actual error causing test failures ---
            System.err.println("Failed to submit task: " + e.getMessage());
            e.printStackTrace();
            // --------------------------------------------------------------------

            // Restore state on failure (decrement count since task wasn't submitted)
            this.inFlight.decrementAndGet(); 
            Thread.currentThread().interrupt(); 
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        // TODO: submit tasks one by one and wait until all finish
        for(Runnable task : tasks){ // for each loop that iterates throught the runnable tasks.
            this.submit(task); 
        }

        synchronized(this) {
            while(inFlight.get() > 0){
                try{
                    this.wait(); // release lock and sleep until notifyAll() called
                }
                catch(InterruptedException e){ 
                    Thread.currentThread().interrupt(); // handles the exception - send back to caller
                    break; // exit the waiting 
                }
            }    
        }
    
    }
    public void shutdown() throws InterruptedException {
        // TODO
        // use the shut down function in TiredThread class for each worker
        // ---maybe add a if for null case
        for(TiredThread t : workers){
            t.shutdown();
        }

        for(TiredThread t : workers){ // wait for all threads to be ready before executing the shutdown
            t.join();
        }
    }            
    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker.
        // using a java build in string accumulator efficient function
        StringBuilder output = new StringBuilder();
        
        // iterate the workers array
        for (TiredThread t : workers) {
            // string builder format
            String curr = String.format("Worker Id: %d, Fatigue: %.2f, Is busy: %b, Time used: %d, Time idle: %d\n",

            // calling all the getters in TiredThread class
            t.getWorkerId(), 
            t.getFatigue(), 
            t.isBusy(), 
            t.getTimeUsed(), 
            t.getTimeIdle());

            // concat the strings
            output.append(curr);            
        }

        return output.toString();
    }
}
