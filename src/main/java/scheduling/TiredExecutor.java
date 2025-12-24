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
            toInsert.run();
        }
    }

    public void submit(Runnable task) {
        // TODO
        // increment the amount of working threads
        this.inFlight.incrementAndGet();

        try {
            TiredThread w1 = this.idleMinHeap.take(); // choose the least fatigue thread from the heap - build in blocking function - boolean codition is not needed,

            // create a wrapper for the task - in order to maintain the fields correctly
            Runnable wrapper = () -> {
                try {
                    task.run(); // run the given task
                }
                finally {
                    this.idleMinHeap.add(w1); // return the worker back to the heap after finishing the task

                    // check if all threads finished their tasks and notify if they are
                    if (this.inFlight.decrementAndGet() == 0) {
                        synchronized (this) {
                            this.notifyAll();
                        }
                    }
                }
            };

            w1.newTask(wrapper); // assign the task to thread - wrapper make sure the fields are updating

        } 
        catch (Exception e) {
            // handle exception
            this.inFlight.decrementAndGet(); // decrease back the number of working threads
            Thread.currentThread().interrupt(); // notify the calling thread that an interrupt occurred
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
