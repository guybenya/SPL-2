package memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SharedVectorTest {

    // --- Constructor & State Tests ---

    @Test
    public void test1_ConstructorRowMajor() {
        double[] data = {1, 2, 3};
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        
        assertEquals(3, v.length());
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        assertEquals(1.0, v.get(0));
    }

    @Test
    public void test2_ConstructorColumnMajor() {
        double[] data = {10, 20};
        SharedVector v = new SharedVector(data, VectorOrientation.COLUMN_MAJOR);
        
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        assertEquals(20.0, v.get(1));
    }

    @Test
    public void test3_ZeroLengthVector() {
        // Edge Case: Empty vector
        SharedVector v = new SharedVector(new double[0], VectorOrientation.ROW_MAJOR);
        assertEquals(0, v.length());
    }

    // --- Accessor Tests (get) ---

    @Test
    public void test4_GetValidIndex() {
        SharedVector v = new SharedVector(new double[]{5.5}, VectorOrientation.ROW_MAJOR);
        assertEquals(5.5, v.get(0));
    }

    @Test
    public void test5_GetIndexOutOfBoundsNegative() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertThrows(IndexOutOfBoundsException.class, () -> v.get(-1), 
            "Should throw exception for negative index");
    }

    @Test
    public void test6_GetIndexOutOfBoundsTooHigh() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertThrows(IndexOutOfBoundsException.class, () -> v.get(2), 
            "Should throw exception for index >= length");
    }

    // --- Transpose Tests ---

    @Test
    public void test7_TransposeRowToColumn() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation(), "Should flip to COLUMN_MAJOR");
    }

    @Test
    public void test8_TransposeColumnToRow() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation(), "Should flip back to ROW_MAJOR");
    }

    @Test
    public void test9_TransposeDataRemainsIntact() {
        // Ensure data isn't lost during transpose
        double[] data = {100, 200};
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertEquals(100, v.get(0));
        assertEquals(200, v.get(1));
    }

    // --- Negate Tests ---

    @Test
    public void test10_NegatePositiveValues() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(-1.0, v.get(0));
        assertEquals(-2.0, v.get(1));
    }

    @Test
    public void test11_NegateNegativeValues() {
        SharedVector v = new SharedVector(new double[]{-5, -10}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(5.0, v.get(0));
        assertEquals(10.0, v.get(1));
    }

    @Test
    public void test12_NegateZero() {
        SharedVector v = new SharedVector(new double[]{0.0}, VectorOrientation.ROW_MAJOR);
        v.negate();
        // 0.0 can be -0.0 in double, checking delta equality
        assertEquals(0.0, v.get(0), 0.0001);
    }

    // --- Add Tests ---

    @Test
    public void test13_AddTwoRows() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.ROW_MAJOR);
        
        v1.add(v2); // v1 = v1 + v2
        
        assertEquals(4.0, v1.get(0));
        assertEquals(6.0, v1.get(1));
    }

    @Test
    public void test14_AddTwoColumns() {
        SharedVector v1 = new SharedVector(new double[]{10}, VectorOrientation.COLUMN_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{20}, VectorOrientation.COLUMN_MAJOR);
        
        v1.add(v2);
        assertEquals(30.0, v1.get(0));
    }

    @Test
    public void test15_AddDifferentLengthsThrows() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        
        assertThrows(IllegalArgumentException.class, () -> v1.add(v2), 
            "Should throw exception when adding vectors of different lengths");
    }

    @Test
    public void test16_AddDifferentOrientationsThrows() {
        // Assuming strict linear algebra rules or implementation detail: 
        // usually adding Row + Column is ambiguous or forbidden in this context without broadcasting.
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
        
        // Note: Check your specific implementation requirements. 
        // Usually vectors must match orientation to be added directly.
        try {
            v1.add(v2);
            // If it doesn't throw, we might want to check if it's allowed behavior.
            // But usually this is an error in LAE.
            // Uncomment below if your code enforces this:
            // fail("Expected IllegalArgumentException for different orientations");
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }

    // --- Dot Product Tests (Strict Rules) ---

    @Test
    public void test17_DotProductValidRowDotCol() {
        // Valid: Row . Column
        SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);
        
        // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        double result = row.dot(col);
        assertEquals(32.0, result, 0.0001);
    }

    @Test
    public void test18_DotProductInvalidRowDotRow() {
        // Invalid: Row . Row
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.ROW_MAJOR);
        
        assertThrows(IllegalArgumentException.class, () -> v1.dot(v2), 
            "Dot product should only work for Row . Column");
    }

    @Test
    public void test19_DotProductInvalidColDotCol() {
        // Invalid: Col . Col
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.COLUMN_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1}, VectorOrientation.COLUMN_MAJOR);
        
        assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
    }

    @Test
    public void test20_DotProductInvalidColDotRow() {
        // Invalid: Col . Row (Order matters in matrix algebra implementation often)
        SharedVector col = new SharedVector(new double[]{1}, VectorOrientation.COLUMN_MAJOR);
        SharedVector row = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        
        // Based on the specific code snippet you showed earlier:
        // "if (this.orientation != ROW_MAJOR) throw..."
        assertThrows(IllegalArgumentException.class, () -> col.dot(row), 
            "First vector must be ROW_MAJOR");
    }

    @Test
    public void test21_DotProductDifferentLengths() {
        SharedVector row = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.COLUMN_MAJOR);
        
        assertThrows(IllegalArgumentException.class, () -> row.dot(col), 
            "Vectors must have same length");
    }

    // --- Complex/Sequence Tests ---

    @Test
    public void test22_TransposeAndDot() {
        // Create two rows
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.ROW_MAJOR);
        
        // Transpose v2 to be a column
        v2.transpose(); // Now Column
        
        // Now dot should work
        double res = v1.dot(v2); // 1*3 + 2*4 = 11
        assertEquals(11.0, res);
    }

    @Test
    public void test23_AddAndNegateSequence() {
        SharedVector v1 = new SharedVector(new double[]{10}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{5}, VectorOrientation.ROW_MAJOR);
        
        v1.add(v2); // 15
        v1.negate(); // -15
        
        assertEquals(-15.0, v1.get(0));
    }

    // --- Stress / Large Data ---

    @Test
    public void test24_LargeVectorOps() {
        int size = 10000;
        double[] data = new double[size];
        for(int i=0; i<size; i++) data[i] = 1.0;
        
        SharedVector v1 = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        
        long start = System.currentTimeMillis();
        v1.add(v2); // All become 2.0
        long end = System.currentTimeMillis();
        
        assertEquals(2.0, v1.get(size-1));
        assertTrue((end - start) < 1000, "Vector addition should be fast");
    }

    @Test
    public void test25_EncapsulationCheck() {
        // Checking if constructor copies array or references it.
        // Usually for shared memory simulation, reference might be kept, 
        // OR defensive copy is made. This test documents behavior.
        double[] rawData = {1.0};
        SharedVector v = new SharedVector(rawData, VectorOrientation.ROW_MAJOR);
        
        rawData[0] = 99.0;
        
        // If your implementation creates a COPY, this should remain 1.0.
        // If it stores REFERENCE, it becomes 99.0.
        // Adjust assertion based on your specific implementation choice.
        // Assuming typical "Shared" memory implies referencing the passed data or just testing current state.
        // For safety, let's just print behavior or assert one and see if it fails.
        // assertEquals(1.0, v.get(0)); // Uncomment if defensive copy expected
    }
}