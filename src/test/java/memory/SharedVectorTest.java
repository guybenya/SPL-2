package memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SharedVectorTest {

    // --- 1. Constructors & Basics ---

    @Test
    public void testConstructor_NullInput_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SharedVector(null, VectorOrientation.ROW_MAJOR);
        }, "Constructor should throw exception when vector array is null");
    }

    @Test
    public void testConstructor_ValidVector() {
        double[] data = {1.0, 2.0, 3.0};
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);

        assertEquals(3, v.length(), "Should return correct length");
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation(), "Should return correct orientation");
    }

    @Test
    public void testConstructor_EmptyVector() {
        SharedVector v = new SharedVector(new double[0], VectorOrientation.ROW_MAJOR);
        assertEquals(0, v.length());
    }

    @Test
    public void testConstructor_ScalarVector() {
        SharedVector v = new SharedVector(new double[]{5.5}, VectorOrientation.COLUMN_MAJOR);
        assertEquals(1, v.length());
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        assertEquals(5.5, v.get(0));
    }

    // --- 2. Get Method ---

    @Test
    public void testGet_OutOfBounds_ThrowsException() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertThrows(IndexOutOfBoundsException.class, () -> v.get(-1), "Negative index should throw exception");
        assertThrows(IndexOutOfBoundsException.class, () -> v.get(2), "Index >= length should throw exception");
    }

    // --- 3. Mathematical Operations: Add ---

    @Test
    public void testAdd_ValidVectors() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.ROW_MAJOR);

        v1.add(v2);

        assertEquals(4.0, v1.get(0));
        assertEquals(6.0, v1.get(1));
    }

    @Test
    public void testAdd_DifferentSizes_ThrowsException() {
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> v1.add(v2), "Adding vectors of different sizes should fail");
    }

    @Test
    public void testAdd_DifferentOrientation_ThrowsException() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.COLUMN_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> v1.add(v2), "Adding vectors with different orientations should fail");
    }

    @Test
    public void testAdd_VectorsSizeOne() {
        SharedVector v1 = new SharedVector(new double[]{10}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{20}, VectorOrientation.ROW_MAJOR);

        v1.add(v2);
        assertEquals(30.0, v1.get(0));
    }

    @Test
    public void testAdd_BigVectors() {
        int size = 10000;
        double[] data = new double[size];
        for(int i=0; i<size; i++) data[i] = 1.0;

        SharedVector v1 = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(data.clone(), VectorOrientation.ROW_MAJOR);

        v1.add(v2);
        assertEquals(2.0, v1.get(size-1));
    }

    // --- 4. Mathematical Operations: Negate ---

    @Test
    public void testNegate_RegularAndNull() {
        SharedVector v = new SharedVector(new double[]{1, -1, 0}, VectorOrientation.ROW_MAJOR);
        v.negate();

        assertEquals(-1.0, v.get(0));
        assertEquals(1.0, v.get(1));
        assertEquals(0.0, v.get(2), 0.0001); 
    }

    // --- 5. Mathematical Operations: Dot Product ---

    @Test
    public void testDot_ValidRowDotCol() {
        SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);

        // 1*4 + 2*5 + 3*6 = 32
        double result = row.dot(col);
        assertEquals(32.0, result);
    }

    @Test
    public void testDot_DifferentLengths_ThrowsException() {
        SharedVector row = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.COLUMN_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> row.dot(col));
    }

    @Test
    public void testDot_SameOrientation_ThrowsException() {
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        
        assertThrows(IllegalArgumentException.class, () -> v1.dot(v2), "Dot product requires Row . Column");
    }

    @Test
    public void testDot_Aliasing() {
        SharedVector v = new SharedVector(new double[]{2}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> v.dot(v));
    }
    
    // --- 6. Vector-Matrix Multiplication (vecMatMul) ---
    
    @Test
    public void testVecMatMul_Valid() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        
        // Matrix 2x2:
        // 3 4
        // 5 6
        double[][] matData = {{3, 4}, {5, 6}};
        SharedMatrix m = new SharedMatrix(matData);
        
        // Result: [1*3 + 2*5, 1*4 + 2*6] = [13, 16]
        v.vecMatMul(m);
        
        assertEquals(13.0, v.get(0));
        assertEquals(16.0, v.get(1));
    }
    
    @Test
    public void testVecMatMul_DimensionMismatch() {
        SharedVector v = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR); 
        SharedMatrix m = new SharedMatrix(new double[][]{{1, 2}, {3, 4}}); 
        
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m), "Vector length must match Matrix height");
    }

    @Test
    public void testVecMatMul_ColumnMajorMatrix() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        
        // Matrix input (Rows):
        // 3 4
        // 5 6
        // loadColumnMajor internally transposes this to columns:
        // Col0: [3, 5]
        // Col1: [4, 6]
        
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(new double[][]{{3, 4}, {5, 6}}); 
        
        // Calculation:
        // v * Col0 = [1, 2] * [3, 5] = 1*3 + 2*5 = 13
        // v * Col1 = [1, 2] * [4, 6] = 1*4 + 2*6 = 16
        
        v.vecMatMul(m);
        
        assertEquals(13.0, v.get(0));
        assertEquals(16.0, v.get(1));
    }
}