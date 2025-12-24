package memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

public class SharedMatrixTest {

    private SharedMatrix matrix;

    @BeforeEach
    public void setUp() {
        matrix = new SharedMatrix();
    }

    // --- Initialization & Basic State Tests ---

    @Test
    public void test1_NewMatrixIsEmpty() {
        // Verify a new instance is empty and has 0 length
        assertEquals(0, matrix.length(), "New matrix should have length 0");
        assertNull(matrix.getOrientation(), "New matrix should have null orientation");
    }

    @Test
    public void test2_ConstructorWithValidData() {
        // Test the constructor that accepts double[][]
        double[][] data = {{1.0, 2.0}, {3.0, 4.0}};
        SharedMatrix m = new SharedMatrix(data);
        assertEquals(2, m.length(), "Constructor should initialize correct length");
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation(), "Default constructor should set ROW_MAJOR");
    }

    // --- Load Row Major Tests ---

    @Test
    public void test3_LoadRowMajorValid() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}};
        matrix.loadRowMajor(data);
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
        assertEquals(2, matrix.length());
    }

    @Test
    public void test4_LoadRowMajorNullInput() {
        // Expect Exception when loading null
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(null), 
            "Loading null should throw IllegalArgumentException");
    }

    @Test
    public void test5_LoadRowMajorEmptyArray() {
        // Expect Exception when loading empty array {}
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(new double[0][0]), 
            "Loading empty array should throw exception");
    }

    @Test
    public void test6_LoadRowMajorEmptySubArray() {
        // Expect Exception when loading {{}} (rows exist but are empty)
        double[][] data = new double[2][0];
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(data), 
            "Loading array with empty rows should throw exception");
    }

    // --- Load Column Major Tests ---

    @Test
    public void test7_LoadColumnMajorValid() {
        // Loading data represented as columns
        double[][] cols = {{1, 2}, {3, 4}, {5, 6}};
        matrix.loadColumnMajor(cols);
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
        assertEquals(3, matrix.length(), "Length should be number of columns");
    }

    @Test
    public void test8_LoadColumnMajorNullInput() {
        assertThrows(IllegalArgumentException.class, () -> matrix.loadColumnMajor(null), 
            "Loading null should throw exception");
    }

    @Test
    public void test9_LoadColumnMajorEmpty() {
        assertThrows(IllegalArgumentException.class, () -> matrix.loadColumnMajor(new double[0][0]), 
            "Loading empty array should throw exception");
    }

    // --- Read Logic (The Core Logic) ---

    @Test
    public void test10_ReadRowMajorFromRowMajorStorage() {
        // Simple pass-through check
        double[][] data = {{10, 20}, {30, 40}};
        matrix.loadRowMajor(data);
        double[][] result = matrix.readRowMajor();
        assertArrayEquals(data, result, "Should return identical data");
    }

    @Test
    public void test11_ReadRowMajorFromColumnMajorStorage() {
        // Complex check: Logic must transpose columns back to rows
        // Input (Columns):
        // Col0: [1, 2]
        // Col1: [3, 4]
        // Logical Matrix (Rows):
        // [1, 3]
        // [2, 4]
        double[][] cols = {{1, 2}, {3, 4}};
        double[][] expectedRows = {{1, 3}, {2, 4}};
        
        matrix.loadColumnMajor(cols);
        double[][] result = matrix.readRowMajor();
        
        assertEquals(2, result.length); // 2 rows
        assertEquals(2, result[0].length); // 2 cols
        assertArrayEquals(expectedRows[0], result[0]);
        assertArrayEquals(expectedRows[1], result[1]);
    }

    @Test
    public void test12_ReadFromEmptyMatrix() {
        // Reading an uninitialized matrix should return a 0x0 array (not null)
        double[][] result = matrix.readRowMajor();
        assertNotNull(result, "Result should not be null");
        assertEquals(0, result.length, "Result should be empty");
    }

    // --- Accessor & Exception Tests ---

    @Test
    public void test13_GetVectorValidIndex() {
        double[][] data = {{1.1}, {2.2}};
        matrix.loadRowMajor(data);
        assertNotNull(matrix.get(0));
        assertEquals(1.1, matrix.get(0).get(0));
    }

    @Test
    public void test14_GetVectorIndexOutOfBoundsHigh() {
        double[][] data = {{1.1}};
        matrix.loadRowMajor(data);
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(5), 
            "Should throw IndexOutOfBounds for high index");
    }

    @Test
    public void test15_GetVectorIndexOutOfBoundsNegative() {
        double[][] data = {{1.1}};
        matrix.loadRowMajor(data);
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(-1), 
            "Should throw IndexOutOfBounds for negative index");
    }

    @Test
    public void test16_GetOrientationEmpty() {
        // Special case: orientation of empty matrix
        assertNull(matrix.getOrientation(), "Empty matrix should have null orientation");
    }

    // --- Dimensionality Edge Cases ---

    @Test
    public void test17_SingleElementMatrix() {
        // 1x1 Matrix
        double[][] data = {{42.0}};
        matrix.loadRowMajor(data);
        assertEquals(1, matrix.length());
        assertEquals(42.0, matrix.readRowMajor()[0][0]);
    }

    @Test
    public void test18_SingleRowMatrix() {
        // 1x5 Matrix
        double[][] data = {{1, 2, 3, 4, 5}};
        matrix.loadRowMajor(data);
        assertEquals(1, matrix.length());
        assertEquals(5, matrix.get(0).length());
    }

    @Test
    public void test19_SingleColumnMatrix() {
        // 5x1 Matrix (loaded as Row Major)
        double[][] data = {{1}, {2}, {3}, {4}, {5}};
        matrix.loadRowMajor(data);
        assertEquals(5, matrix.length());
        assertEquals(1, matrix.get(0).length());
    }

    @Test
    public void test20_SingleColumnLoadedAsColumnMajor() {
        // 5x1 Matrix (loaded as Column Major -> 1 column vector)
        double[][] cols = {{1, 2, 3, 4, 5}};
        matrix.loadColumnMajor(cols);
        assertEquals(1, matrix.length(), "Should have 1 column vector");
        assertEquals(5, matrix.get(0).length(), "Column vector should have length 5");
    }

    // --- State Management ---

    @Test
    public void test21_ReloadingMatrixUpdatesData() {
        // Verify that calling load replaces old data completely
        double[][] data1 = {{1, 1}};
        double[][] data2 = {{2, 2}, {3, 3}};
        
        matrix.loadRowMajor(data1);
        assertEquals(1, matrix.length());
        
        matrix.loadRowMajor(data2);
        assertEquals(2, matrix.length());
        assertEquals(2.0, matrix.get(0).get(0));
    }

    @Test
    public void test22_SwitchingOrientations() {
        // Load Row Major, then switch to Column Major
        double[][] rows = {{1, 2}};
        matrix.loadRowMajor(rows);
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
        
        double[][] cols = {{1}, {2}};
        matrix.loadColumnMajor(cols);
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }

    // --- Stress / Large Data Tests ---

    @Test
    public void test23_LargeMatrixLoadAndRead() {
        // 1000x1000 Matrix
        int size = 1000;
        double[][] largeData = new double[size][size];
        // Fill with some data
        for(int i=0; i<size; i++) largeData[i][i] = 1.0; // Identity-like
        
        long start = System.currentTimeMillis();
        matrix.loadRowMajor(largeData);
        long loadTime = System.currentTimeMillis() - start;
        
        assertEquals(size, matrix.length());
        
        start = System.currentTimeMillis();
        double[][] result = matrix.readRowMajor();
        long readTime = System.currentTimeMillis() - start;
        
        assertArrayEquals(largeData[500], result[500], "Data integrity check for large matrix");
        
        // Ensure it doesn't take 'forever' (Basic sanity check)
        assertTrue(loadTime < 2000, "Loading 1000x1000 shouldn't take > 2 seconds");
        assertTrue(readTime < 2000, "Reading 1000x1000 shouldn't take > 2 seconds");
    }

    @Test
    public void test24_LargeMatrixTransposeRead() {
        // Load large data as columns and read as rows (stress test the transpose logic)
        int rows = 500;
        int cols = 500;
        double[][] colData = new double[cols][rows]; // 500 cols, each length 500
        
        matrix.loadColumnMajor(colData);
        double[][] result = matrix.readRowMajor();
        
        assertEquals(rows, result.length);
        assertEquals(cols, result[0].length);
    }

    // --- Deep Copy / Reference Behavior ---
    
    @Test
    public void test25_DataEncapsulationCheck() {
        // Check if modifying the source array affects the SharedMatrix
        // Note: Based on implementation, SharedMatrix typically wraps the arrays. 
        // This test documents the behavior (Reference vs Copy).
        
        double[][] data = {{10.0}};
        matrix.loadRowMajor(data);
        
        // Modify source
        data[0][0] = 99.0;
        
        // If implementation copies, this should remain 10.0. 
        // If it stores reference, it becomes 99.0.
        // YOUR IMPLEMENTATION stores reference (new SharedVector(matrix[i]...)).
        // So we assert that it DOES change (or simply assert the current behavior).
        
        assertEquals(99.0, matrix.get(0).get(0), 
            "Current implementation stores references to rows, so external changes might affect matrix");
    }
}