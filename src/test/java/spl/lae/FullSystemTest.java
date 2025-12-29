package spl.lae;

import org.junit.jupiter.api.*;
import parser.ComputationNode;
import parser.ComputationNodeType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit System Test for SPL Assignment 2.
 * * Features:
 * - Uses a static engine instance to accumulate "Fatigue" stats across all tests.
 * - 50 Randomized Correctness iterations.
 * - Input Edge Case validation (Exceptions).
 * - Concurrency & Deadlock Stress Test (Large Matrix).
 * - Prints Worker Fatigue Report after all tests complete.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullSystemTest {

    private static final int NUM_THREADS = 10;
    private static final int RANDOM_TEST_ITERATIONS = 50;
    private static LinearAlgebraEngine engine;
    private static final Random random = new Random(System.currentTimeMillis());

    @BeforeAll
    public static void setUp() {
        System.out.println("=== Initializing Linear Algebra Engine (10 Threads) ===");
        engine = new LinearAlgebraEngine(NUM_THREADS);
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        System.out.println("\n=== Tests Finished. Shutting down engine... ===");
        engine.shutdown();
        
        // Print the Fatigue Report at the very end
        printFatigueComparisonTable(engine.getWorkerReport());
    }

    /**
     * PART 1: Randomized Mathematical Correctness
     * Runs 50 iterations of mixed operations (Add, Mul, Transpose, Negate, Complex).
     */
    @Test
    @Order(1)
    @DisplayName("Run 50 Randomized Correctness Tests")
    public void testRandomizedCorrectness() {
        System.out.println("Running " + RANDOM_TEST_ITERATIONS + " randomized mixed operations...");
        
        for (int i = 1; i <= RANDOM_TEST_ITERATIONS; i++) {
            // 0=Add, 1=Mul, 2=Transpose, 3=Negate, 4=Complex((A+B)*C)
            int opType = random.nextInt(5);
            
            try {
                switch (opType) {
                    case 0: runRandomAddition(); break;
                    case 1: runRandomMultiplication(); break;
                    case 2: runRandomTranspose(); break;
                    case 3: runRandomNegate(); break;
                    case 4: runComplexIntegration(); break;
                }
            } catch (Exception e) {
                fail("Randomized test iteration #" + i + " failed with exception: " + e.getMessage());
            }
        }
        System.out.println(">> Part 1 (Randomized Correctness) PASSED.");
    }

    /**
     * PART 2: Edge Cases (Dimension Mismatches, Empty Matrices, etc.)
     */
    @Test
    @Order(2)
    @DisplayName("Edge Cases & Exception Handling")
    public void testEdgeCases() {
        System.out.println("Running Edge Case Tests...");

        // 1. Dimension Mismatch (Addition)
        double[][] m1 = generateMatrix(2, 2);
        double[][] m2 = generateMatrix(3, 3);
        ComputationNode rootAdd = new ComputationNode(ComputationNodeType.ADD, 
                Arrays.asList(new ComputationNode(m1), new ComputationNode(m2)));
        
        assertThrows(IllegalArgumentException.class, () -> engine.run(rootAdd), 
                "Should throw IllegalArgumentException for ADD dimension mismatch");

        // 2. Dimension Mismatch (Multiplication)
        double[][] m3 = generateMatrix(2, 5);
        double[][] m4 = generateMatrix(4, 2); // Cols(5) != Rows(4)
        ComputationNode rootMul = new ComputationNode(ComputationNodeType.MULTIPLY, 
                Arrays.asList(new ComputationNode(m3), new ComputationNode(m4)));
        
        assertThrows(IllegalArgumentException.class, () -> engine.run(rootMul), 
                "Should throw IllegalArgumentException for MUL dimension mismatch");

        // 3. 1x1 Matrix Operation (Valid)
        double[][] smallA = {{5.0}};
        double[][] smallB = {{3.0}};
        ComputationNode res = engine.run(new ComputationNode(ComputationNodeType.ADD, 
                Arrays.asList(new ComputationNode(smallA), new ComputationNode(smallB))));
        
        assertEquals(8.0, res.getMatrix()[0][0], 0.001, "1x1 Matrix addition incorrect");
        
        System.out.println(">> Part 2 (Edge Cases) PASSED.");
    }

    /**
     * PART 3: Stress & Deadlock Test
     * Multiplies two 500x500 matrices.
     */
    @Test
    @Order(3)
    @DisplayName("Stress Test & Deadlock Check (500x500)")
    public void testStressAndDeadlock() {
        int size = 500;
        System.out.println("Running Stress Test (500x500 multiplication)...");
        
        double[][] m1 = generateMatrix(size, size);
        double[][] m2 = generateIdentity(size); // M * I = M

        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, 
                Arrays.asList(new ComputationNode(m1), new ComputationNode(m2)));

        long start = System.currentTimeMillis();
        ComputationNode result = assertDoesNotThrow(() -> engine.run(root), "Execution threw exception during stress test");
        long end = System.currentTimeMillis();

        System.out.println("Stress test finished in " + (end - start) + "ms");

        // Verify result (M * I = M)
        assertMatrixEquals(m1, result.getMatrix());
        
        System.out.println(">> Part 3 (Stress Test) PASSED.");
    }


    // =========================================================================================
    //  PRIVATE TEST RUNNERS (Called by the loop)
    // =========================================================================================

    private void runRandomAddition() {
        int rows = random.nextInt(40) + 1;
        int cols = random.nextInt(40) + 1;
        double[][] m1 = generateMatrix(rows, cols);
        double[][] m2 = generateMatrix(rows, cols);

        ComputationNode result = engine.run(new ComputationNode(ComputationNodeType.ADD, 
                Arrays.asList(new ComputationNode(m1), new ComputationNode(m2))));
        
        assertMatrixEquals(simpleAdd(m1, m2), result.getMatrix());
    }

    private void runRandomMultiplication() {
        int rowsA = random.nextInt(30) + 1;
        int colsA = random.nextInt(30) + 1;
        int colsB = random.nextInt(30) + 1;
        double[][] m1 = generateMatrix(rowsA, colsA);
        double[][] m2 = generateMatrix(colsA, colsB);

        ComputationNode result = engine.run(new ComputationNode(ComputationNodeType.MULTIPLY, 
                Arrays.asList(new ComputationNode(m1), new ComputationNode(m2))));
        
        assertMatrixEquals(simpleMultiply(m1, m2), result.getMatrix());
    }

    private void runRandomTranspose() {
        int rows = random.nextInt(30) + 1;
        int cols = random.nextInt(30) + 1;
        double[][] m1 = generateMatrix(rows, cols);

        ComputationNode result = engine.run(new ComputationNode(ComputationNodeType.TRANSPOSE, 
                Arrays.asList(new ComputationNode(m1))));
        
        assertMatrixEquals(simpleTranspose(m1), result.getMatrix());
    }

    private void runRandomNegate() {
        int rows = random.nextInt(30) + 1;
        int cols = random.nextInt(30) + 1;
        double[][] m1 = generateMatrix(rows, cols);

        ComputationNode result = engine.run(new ComputationNode(ComputationNodeType.NEGATE, 
                Arrays.asList(new ComputationNode(m1))));
        
        assertMatrixEquals(simpleNegate(m1), result.getMatrix());
    }

    private void runComplexIntegration() {
        // (A + B) * T(C)
        int size = random.nextInt(20) + 2;
        double[][] A = generateMatrix(size, size);
        double[][] B = generateMatrix(size, size);
        double[][] C = generateMatrix(size, size);

        ComputationNode nodeA = new ComputationNode(A);
        ComputationNode nodeB = new ComputationNode(B);
        ComputationNode nodeC = new ComputationNode(C);

        ComputationNode addNode = new ComputationNode(ComputationNodeType.ADD, Arrays.asList(nodeA, nodeB));
        ComputationNode transNode = new ComputationNode(ComputationNodeType.TRANSPOSE, Arrays.asList(nodeC));
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, Arrays.asList(addNode, transNode));

        ComputationNode result = engine.run(root);
        
        double[][] expected = simpleMultiply(simpleAdd(A, B), simpleTranspose(C));
        assertMatrixEquals(expected, result.getMatrix());
    }

    // =========================================================================================
    //  SIMPLE ORACLE IMPLEMENTATIONS & HELPERS
    // =========================================================================================

    private double[][] simpleAdd(double[][] a, double[][] b) {
        double[][] res = new double[a.length][a[0].length];
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < a[0].length; j++)
                res[i][j] = a[i][j] + b[i][j];
        return res;
    }

    private double[][] simpleMultiply(double[][] a, double[][] b) {
        int rows = a.length;
        int cols = b[0].length;
        int common = a[0].length;
        double[][] res = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                for (int k = 0; k < common; k++) {
                    res[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return res;
    }

    private double[][] simpleTranspose(double[][] a) {
        double[][] res = new double[a[0].length][a.length];
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < a[0].length; j++)
                res[j][i] = a[i][j];
        return res;
    }

    private double[][] simpleNegate(double[][] a) {
        double[][] res = new double[a.length][a[0].length];
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < a[0].length; j++)
                res[i][j] = -a[i][j];
        return res;
    }

    private double[][] generateMatrix(int rows, int cols) {
        double[][] m = new double[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                m[i][j] = random.nextDouble() * 10;
        return m;
    }

    private double[][] generateIdentity(int size) {
        double[][] m = new double[size][size];
        for (int i = 0; i < size; i++) m[i][i] = 1.0;
        return m;
    }

    private void assertMatrixEquals(double[][] expected, double[][] actual) {
        assertEquals(expected.length, actual.length, "Row count mismatch");
        assertEquals(expected[0].length, actual[0].length, "Col count mismatch");

        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected[0].length; j++) {
                assertEquals(expected[i][j], actual[i][j], 0.001, 
                        "Mismatch at [" + i + "][" + j + "]");
            }
        }
    }

    // =========================================================================================
    //  FATIGUE REPORT PRINTER
    // =========================================================================================

    private static void printFatigueComparisonTable(String rawReport) {
        System.out.println("\n[PART 4] Fatigue Factor Comparison Table");
        System.out.println("========================================");
        System.out.println(String.format("| %-10s | %-15s |", "Worker ID", "Fatigue Factor"));
        System.out.println("|------------|-----------------|");

        if (rawReport != null && !rawReport.isEmpty()) {
            String[] lines = rawReport.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                try {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String idPart = parts[0].split(":")[1].trim();
                        String fatiguePart = parts[1].split(":")[1].trim();
                        System.out.println(String.format("| %-10s | %-15s |", idPart, fatiguePart));
                    }
                } catch (Exception e) { 
                    // Ignore parsing errors for non-matching lines
                }
            }
        } else {
            System.out.println("| No data available              |");
        }
        System.out.println("========================================");
    }
}