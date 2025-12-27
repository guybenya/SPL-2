package scheduling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TiredExecutorTest {

    private TiredExecutor executor;
    private final int DEFAULT_POOL_SIZE = 4;

    @BeforeEach
    void setUp() {
        // Clear any interrupt status left over from previous failed tests to avoid false positives
        Thread.interrupted();

        // Initialize a new executor before each test to ensure a clean state
        executor = new TiredExecutor(DEFAULT_POOL_SIZE);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Ensure the executor is shut down after each test to free resources
        if (executor != null) {
            executor.shutdown();
        }
    }

    // Helper method to access the private 'workers' array using Reflection
    // This allows us to inspect the internal state of the threads for fatigue verification
    private TiredThread[] getWorkersReflectively(TiredExecutor executor) {
        try {
            Field field = TiredExecutor.class.getDeclaredField("workers");
            field.setAccessible(true);
            return (TiredThread[]) field.get(executor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access workers via reflection", e);
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testBasicExecution() throws InterruptedException {
        // Verify that a single task can be submitted and executed
        CountDownLatch latch = new CountDownLatch(1);
        executor.submit(latch::countDown);
        
        boolean completed = latch.await(1, TimeUnit.SECONDS);
        assertTrue(completed, "Single task should finish within timeout");
    }

    @Test
    void testFatigueFairnessDistribution() throws InterruptedException {
        // This test verifies that the workload is distributed among threads.
        // It pushes many small tasks and checks that no single thread did ALL the work.
        
        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                // Simulate tiny amount of work to increment internal timers
                Math.pow(10, 10); 
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All tasks should finish");

        // Use reflection to inspect the workers
        TiredThread[] workers = getWorkersReflectively(executor);
        
        long totalUsedTime = 0;
        int activeWorkers = 0;

        for (TiredThread worker : workers) {
            long used = worker.getTimeUsed();
            totalUsedTime += used;
            if (used > 0) {
                activeWorkers++;
            }
        }

        assertTrue(totalUsedTime > 0, "Total time used across all workers should be positive");
        assertTrue(activeWorkers > 1, "Workload should be distributed to more than one thread");
    }

    @Test
    void testExceptionIsolation() throws InterruptedException {
        // Verify that if a task throws an exception, the worker thread remains alive
        // and can process subsequent tasks.
        
        CountDownLatch latch = new CountDownLatch(1);

        // Submit a task that crashes
        executor.submit(() -> {
            throw new RuntimeException("Intentional Failure");
        });

        // Submit a healthy task immediately after
        executor.submit(latch::countDown);

        boolean success = latch.await(2, TimeUnit.SECONDS);
        assertTrue(success, "Executor should recover from task exception and run the next task");
    }

    @Test
    void testLargeWorkloadStress() throws InterruptedException {
        // Stress test with a large number of tasks to ensure no deadlocks or memory leaks
        int taskCount = 10_000;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "Executor stuck on large workload");
        assertEquals(taskCount, counter.get(), "All tasks must be executed exactly once");
    }

    @Test
    void testSubmitAllHelper() throws InterruptedException {
        // Test the submitAll utility method with a list of tasks
        int taskCount = 50;
        List<Runnable> tasks = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            tasks.add(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        executor.submitAll(tasks);
        
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(taskCount, counter.get(), "submitAll should execute all tasks in the collection");
    }

    @Test
    void testEmptyShutdown() {
        // Verify that shutting down an executor with no tasks doesn't throw exceptions
        assertDoesNotThrow(() -> executor.shutdown(), "Shutdown on idle executor should be safe");
    }

    @Test
    void testPoolSizeOneSerialization() throws InterruptedException {
        // Edge case: Pool size of 1 should execute tasks sequentially
        TiredExecutor singleExecutor = new TiredExecutor(1);
        int taskCount = 5;
        List<Integer> executionOrder = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            final int id = i;
            singleExecutor.submit(() -> {
                try {
                    Thread.sleep(10); // Ensure noticeable duration
                    executionOrder.add(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(2, TimeUnit.SECONDS);
        singleExecutor.shutdown();

        assertEquals(taskCount, executionOrder.size());
    }
    
    @Test
    void testFatigueValueCorrectness() throws InterruptedException {
        // Verify the math: fatigue = timeUsed * fatigueFactor
        CountDownLatch latch = new CountDownLatch(1);
        long sleepTime = 50;

        executor.submit(() -> {
            try {
                Thread.sleep(sleepTime); 
            } catch (InterruptedException e) {
                 Thread.currentThread().interrupt();
            }
            latch.countDown();
        });

        latch.await(2, TimeUnit.SECONDS);
        
        // Allow time for atomic updates
        Thread.sleep(100);

        TiredThread[] workers = getWorkersReflectively(executor);
        for (TiredThread t : workers) {
            long used = t.getTimeUsed();
            if (used > 0) {
                double actualFatigue = t.getFatigue();
                assertTrue(actualFatigue > 0, "Fatigue should be positive for working thread");
            }
        }
    }

/**
     * SPECIAL TEST: Prints a FULL fatigue report.
     * Includes Variance, Standard Deviation, and CV for easy comparison.
     */
    @Test
    void testFullFatigueReport() throws InterruptedException {
        int taskCount = 2000; 
        CountDownLatch latch = new CountDownLatch(taskCount);

        // 1. Run workload
        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                double val = 0;
                for (int j = 0; j < 500; j++) { val += Math.sin(j); }
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // 2. Retrieve data
        TiredThread[] workers = getWorkersReflectively(executor);
        
        double totalFatigue = 0;
        System.out.println("\n========================================");
        System.out.println("       FULL FATIGUE REPORT              ");
        System.out.println("========================================");
        
        for (TiredThread worker : workers) {
            double f = worker.getFatigue();
            totalFatigue += f;
            System.out.printf("Worker %d | Fatigue: %10.2f%n", worker.getWorkerId(), f);
        }

        double average = totalFatigue / workers.length;
        
        // 3. חישוב כל המדדים הסטטיסטיים
        double sumSquaredDiffs = 0;
        for (TiredThread worker : workers) {
            sumSquaredDiffs += Math.pow(worker.getFatigue() - average, 2);
        }
        
        double variance = sumSquaredDiffs; // שונות כוללת (המדד מההוראות)
        double stdDev = Math.sqrt(sumSquaredDiffs / workers.length); // סטיית תקן
        double cv = (average > 0) ? (stdDev / average) : 0; // מקדם השתנות (באחוזים)

        System.out.println("----------------------------------------");
        System.out.printf("Average Fatigue:    %.2f%n", average);
        System.out.println("----------------------------------------");
        System.out.printf("1. Total Variance:  %.2f  (Original metric)%n", variance);
        System.out.printf("2. Std Deviation:   %.2f%n", stdDev);
        System.out.printf("3. Fairness (CV):   %.5f  (Best for comparison)%n", cv);
        System.out.println("========================================\n");
    }
}