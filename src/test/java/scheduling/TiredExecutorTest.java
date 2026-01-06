package scheduling;

import org.junit.jupiter.api.Test;
import memory.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class TiredExecutorTest {

    @Test
    public void testHighLoadConcurrencyAndFatigueReport() throws InterruptedException {
        System.out.println("=================================================");
        System.out.println("TEST: High Load Concurrency with 20 Threads");
        System.out.println("=================================================");

        int numThreads = 20;
        int numTasks = 1000;
        TiredExecutor executor = new TiredExecutor(numThreads);

        // Prepare heavy tasks: Vector x Matrix multiplication
        List<Runnable> tasks = new ArrayList<>();
        SharedVector v = new SharedVector(new double[1000], VectorOrientation.ROW_MAJOR);
        // Fill vector
        for(int i=0; i<1000; i++) { 
            // Access via reflection or rebuild vector logic if set/add not avail. 
            // Using logic: create temp vector and add.
            // Simplified: Just use large add ops for load.
        }
        
        // Creating tasks that burn CPU time to generate fatigue
        for (int i = 0; i < numTasks; i++) {
            tasks.add(() -> {
                // Heavy numeric operation simulation
                double sum = 0;
                for (int j = 0; j < 10000; j++) {
                    sum += Math.sin(j) * Math.cos(j);
                }
                // Small sleep to force context switching and idle time calculations
                try { Thread.sleep(1); } catch (InterruptedException e) {}
            });
        }

        long start = System.currentTimeMillis();
        
        // Submit all tasks
        System.out.println("Submitting " + numTasks + " tasks...");
        executor.submitAll(tasks);
        
        long end = System.currentTimeMillis();
        System.out.println("All tasks completed in " + (end - start) + "ms");

        // Generate Report
        String report = executor.getWorkerReport();
        System.out.println("\n--- FINAL WORKER FATIGUE REPORT ---");
        System.out.println(report);
        System.out.println("-----------------------------------");

        // Assertions
        assertNotNull(report);
        assertTrue(report.contains("Worker Id:"));
        assertTrue(report.contains("Fatigue:"));

        // Shutdown
        executor.shutdown();
    }

    @Test
    public void testSmallExecutor() throws InterruptedException {
        System.out.println("Test: Small executor lifecycle.");
        TiredExecutor exec = new TiredExecutor(2);
        exec.submit(() -> System.out.println("Task 1"));
        exec.submit(() -> System.out.println("Task 2"));
        // submitAll is blocking, submit is not. To test submitAll waiting:
        List<Runnable> tasks = new ArrayList<>();
        tasks.add(() -> { try{ Thread.sleep(100); } catch(Exception e){} });
        exec.submitAll(tasks);
        
        exec.shutdown();
    }
}