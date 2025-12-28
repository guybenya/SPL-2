package spl.lae;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.ComputationNode;
import parser.ComputationNodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LinearAlgebraEngineTest {

    private LinearAlgebraEngine lae;
    private final int NUM_THREADS = 4;
    private final double DELTA = 0.0001; // Tolerance for double comparison

    @BeforeEach
    void setUp() {
        lae = new LinearAlgebraEngine(NUM_THREADS);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (lae != null) {
            lae.shutdown();
        }
    }

    // --- Helper Methods ---

    private ComputationNode createMatrixNode(double[][] data) {
        return new ComputationNode(data);
    }

    private ComputationNode createOpNode(ComputationNodeType type, ComputationNode... children) {
        return new ComputationNode(type, new ArrayList<>(Arrays.asList(children)));
    }

    private void assertMatrixEquals(double[][] expected, double[][] actual) {
        assertNotNull(actual, "Result matrix should not be null");
        assertEquals(expected.length, actual.length, "Row count mismatch");
        assertEquals(expected[0].length, actual[0].length, "Column count mismatch");

        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i], DELTA, "Mismatch at row " + i);
        }
    }

    // --- Basic Operation Tests ---

    @Test
    void testSimpleAddition() {
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{5, 6}, {7, 8}};
        double[][] expected = {{6, 8}, {10, 12}};

        ComputationNode root = createOpNode(ComputationNodeType.ADD, createMatrixNode(m1), createMatrixNode(m2));
        ComputationNode resultNode = lae.run(root);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    @Test
    void testSimpleMultiplication() {
        // 2x3 * 3x2 = 2x2
        double[][] m1 = {
                {1, 2, 3},
                {4, 5, 6}
        };
        double[][] m2 = {
                {7, 8},
                {9, 1},
                {2, 3}
        };
        // Expected:
        // [1*7+2*9+3*2, 1*8+2*1+3*3] = [7+18+6, 8+2+9] = [31, 19]
        // [4*7+5*9+6*2, 4*8+5*1+6*3] = [28+45+12, 32+5+18] = [85, 55]
        double[][] expected = {
                {31, 19},
                {85, 55}
        };

        ComputationNode root = createOpNode(ComputationNodeType.MULTIPLY, createMatrixNode(m1), createMatrixNode(m2));
        ComputationNode resultNode = lae.run(root);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    @Test
    void testSimpleTranspose() {
        double[][] m = {{1, 2, 3}, {4, 5, 6}};
        double[][] expected = {{1, 4}, {2, 5}, {3, 6}};

        ComputationNode root = createOpNode(ComputationNodeType.TRANSPOSE, createMatrixNode(m));
        ComputationNode resultNode = lae.run(root);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    @Test
    void testSimpleNegate() {
        double[][] m = {{1, -2}, {0, 5}};
        double[][] expected = {{-1, 2}, {0, -5}};

        ComputationNode root = createOpNode(ComputationNodeType.NEGATE, createMatrixNode(m));
        ComputationNode resultNode = lae.run(root);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    // --- Nesting and Associativity Tests ---

    @Test
    void testAssociativeAddition() {
        // (A + B) + C handled as list [A, B, C] initially
        double[][] m1 = {{1}};
        double[][] m2 = {{2}};
        double[][] m3 = {{3}};
        double[][] expected = {{6}};

        // Simulate parser output: ADD with 3 children
        ComputationNode root = createOpNode(ComputationNodeType.ADD, 
                createMatrixNode(m1), createMatrixNode(m2), createMatrixNode(m3));
        
        // This implicitly tests 'associativeNesting'
        ComputationNode resultNode = lae.run(root);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    @Test
    void testComplexExpression() {
        // Expression: (A + B)^T * C
        // A, B: 2x2, C: 2x2
        double[][] A = {{1, 2}, {3, 4}};
        double[][] B = {{0, 1}, {1, 0}}; // A+B = {{1,3}, {4,4}}
        // (A+B)^T = {{1,4}, {3,4}}
        double[][] C = {{2, 0}, {0, 2}}; // Identity*2

        // Expected: {{1,4}, {3,4}} * {{2,0}, {0,2}} = {{2,8}, {6,8}}
        double[][] expected = {{2, 8}, {6, 8}};

        ComputationNode addNode = createOpNode(ComputationNodeType.ADD, createMatrixNode(A), createMatrixNode(B));
        ComputationNode transposeNode = createOpNode(ComputationNodeType.TRANSPOSE, addNode);
        ComputationNode root = createOpNode(ComputationNodeType.MULTIPLY, transposeNode, createMatrixNode(C));

        ComputationNode resultNode = lae.run(root);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    // --- Edge Cases & Error Handling ---

    @Test
    void testAddDimensionMismatch() {
        double[][] m1 = {{1, 2}}; // 1x2
        double[][] m2 = {{1}, {2}}; // 2x1

        ComputationNode root = createOpNode(ComputationNodeType.ADD, createMatrixNode(m1), createMatrixNode(m2));

        assertThrows(IllegalArgumentException.class, () -> lae.run(root), 
            "Should throw exception for adding matrices with different dimensions");
    }

    @Test
    void testMultiplyDimensionMismatch() {
        // 2x3 * 2x3 -> Invalid (cols of left != rows of right)
        double[][] m1 = {{1, 2, 3}, {4, 5, 6}};
        double[][] m2 = {{1, 2, 3}, {4, 5, 6}};

        ComputationNode root = createOpNode(ComputationNodeType.MULTIPLY, createMatrixNode(m1), createMatrixNode(m2));

        assertThrows(IllegalArgumentException.class, () -> lae.run(root),
            "Should throw exception for invalid matrix multiplication dimensions");
    }

    @Test
    void testEmptyMatrixInput() {
        // Depending on your InputParser/Implementation, empty matrices might be {{}} or array of length 0.
        // Based on your code "isEmpty" checks vectors length.
        
        // Case 1: 0x0 matrix
        double[][] empty = new double[0][0];
        ComputationNode root = createOpNode(ComputationNodeType.NEGATE, createMatrixNode(empty));

        assertThrows(IllegalArgumentException.class, () -> lae.run(root),
            "Should throw exception when operating on empty matrix");
    }
    
    @Test
    void testSingleValueMatrix() {
        double[][] m1 = {{5}};
        double[][] m2 = {{3}};
        double[][] expected = {{15}};
        
        ComputationNode root = createOpNode(ComputationNodeType.MULTIPLY, createMatrixNode(m1), createMatrixNode(m2));
        ComputationNode result = lae.run(root);
        
        assertMatrixEquals(expected, result.getMatrix());
    }

    // --- Stress / Large Input Test ---

    @Test
    void testLargeMatrixMultiplication() {
        int size = 100; // 100x100 matrix
        double[][] m1 = generateRandomMatrix(size, size);
        double[][] m2 = generateRandomMatrix(size, size);
        
        // Calculate expected result serially
        double[][] expected = serialMultiply(m1, m2);

        ComputationNode root = createOpNode(ComputationNodeType.MULTIPLY, createMatrixNode(m1), createMatrixNode(m2));
        
        long start = System.currentTimeMillis();
        ComputationNode resultNode = lae.run(root);
        long end = System.currentTimeMillis();

        System.out.println("Time for " + size + "x" + size + " multiplication: " + (end - start) + "ms");
        
        assertMatrixEquals(expected, resultNode.getMatrix());
    }
    @Test
    void testForDeadlockWithTimeout() {
        // יצירת מטריצות גדולות מספיק כדי להעסיק את כל ה-Threads
        int size = 100;
        double[][] m1 = generateRandomMatrix(size, size);
        double[][] m2 = generateRandomMatrix(size, size);
        
        ComputationNode root = createOpNode(ComputationNodeType.MULTIPLY, 
                                            createMatrixNode(m1), 
                                            createMatrixNode(m2));

        // הפעלה עם מגבלת זמן של 5 שניות.
        // אם הקוד נכנס ל-Deadlock, הוא לא יסיים בזמן והטסט ייכשל עם הודעה מתאימה.
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            lae.run(root);
        }, "Execution timed out! This likely indicates a DEADLOCK or starvation issue.");
    }

    // --- Auxiliary for Tests ---

    private double[][] generateRandomMatrix(int rows, int cols) {
        Random rand = new Random(42); // Seed for reproducibility
        double[][] m = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m[i][j] = rand.nextDouble() * 10;
            }
        }
        return m;
    }

    private double[][] serialMultiply(double[][] A, double[][] B) {
        int rowsA = A.length;
        int colsA = A[0].length;
        int colsB = B[0].length;
        double[][] C = new double[rowsA][colsB];

        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                for (int k = 0; k < colsA; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }
}