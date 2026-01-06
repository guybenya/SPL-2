package memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

public class SharedVectorTest {

    private SharedVector rowVec;
    private SharedVector colVec;

    @BeforeEach
    public void setUp() {
        // Standard setup for basic tests
        rowVec = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        colVec = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);
    }

    @Test
    public void test01_ConstructorNullArray() {
        System.out.println("Test 01: Verify constructor throws exception on null array.");
        assertThrows(IllegalArgumentException.class, () -> new SharedVector(null, VectorOrientation.ROW_MAJOR));
    }

    @Test
    public void test02_ConstructorNullOrientation() {
        System.out.println("Test 02: Verify behavior with null orientation (assuming allowed or handled).");
        SharedVector v = new SharedVector(new double[]{1}, null);
        assertNull(v.getOrientation());
    }

    @Test
    public void test03_GetValidIndex() {
        System.out.println("Test 03: Verify get() returns correct value.");
        assertEquals(2.0, rowVec.get(1));
    }

    @Test
    public void test04_GetIndexOutOfBoundsNegative() {
        System.out.println("Test 04: Verify get() throws exception on negative index.");
        assertThrows(IndexOutOfBoundsException.class, () -> rowVec.get(-1));
    }

    @Test
    public void test05_GetIndexOutOfBoundsPositive() {
        System.out.println("Test 05: Verify get() throws exception on index >= length.");
        assertThrows(IndexOutOfBoundsException.class, () -> rowVec.get(3));
    }

    @Test
    public void test06_TransposeRowToCol() {
        System.out.println("Test 06: Verify transpose changes ROW to COLUMN.");
        rowVec.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, rowVec.getOrientation());
    }

    @Test
    public void test07_TransposeColToRow() {
        System.out.println("Test 07: Verify transpose changes COLUMN to ROW.");
        colVec.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, colVec.getOrientation());
    }

    @Test
    public void test08_NegateValues() {
        System.out.println("Test 08: Verify negate flips signs.");
        rowVec.negate();
        assertEquals(-1.0, rowVec.get(0));
        assertEquals(-2.0, rowVec.get(1));
        assertEquals(-3.0, rowVec.get(2));
    }

    @Test
    public void test09_NegateZero() {
        System.out.println("Test 09: Verify negate on zero works correctly.");
        SharedVector zero = new SharedVector(new double[]{0, 0}, VectorOrientation.ROW_MAJOR);
        zero.negate();
        assertEquals(0.0, zero.get(0), 0.0001);
    }

    @Test
    public void test10_AddVectorsSuccess() {
        System.out.println("Test 10: Verify addition of two row vectors.");
        SharedVector other = new SharedVector(new double[]{10, 20, 30}, VectorOrientation.ROW_MAJOR);
        rowVec.add(other);
        assertEquals(11.0, rowVec.get(0));
        assertEquals(22.0, rowVec.get(1));
        assertEquals(33.0, rowVec.get(2));
    }

    @Test
    public void test11_AddVectorsDimensionMismatch() {
        System.out.println("Test 11: Verify addition throws exception on length mismatch.");
        SharedVector other = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> rowVec.add(other));
    }

    @Test
    public void test12_AddVectorsOrientationMismatch() {
        System.out.println("Test 12: Verify addition throws exception on orientation mismatch.");
        assertThrows(IllegalArgumentException.class, () -> rowVec.add(colVec));
    }

    @Test
    public void test13_DotProductSuccess() {
        System.out.println("Test 13: Verify dot product (Row . Col).");
        // [1,2,3] . [4,5,6] = 4 + 10 + 18 = 32
        double result = rowVec.dot(colVec);
        assertEquals(32.0, result);
    }

    @Test
    public void test14_DotProductRowDotRowFail() {
        System.out.println("Test 14: Verify dot product fails for Row . Row.");
        SharedVector other = new SharedVector(new double[]{4,5,6}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> rowVec.dot(other));
    }

    @Test
    public void test15_DotProductColDotColFail() {
        System.out.println("Test 15: Verify dot product fails for Col . Col.");
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.COLUMN_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1}, VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
    }

    @Test
    public void test16_DotProductDimensionMismatch() {
        System.out.println("Test 16: Verify dot product fails on dimension mismatch.");
        SharedVector other = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> rowVec.dot(other));
    }

    @Test
    public void test17_LargeVectorAdd() {
        System.out.println("Test 17: Stress test - Adding large vectors.");
        int size = 10000;
        double[] data = new double[size];
        for(int i=0; i<size; i++) data[i] = 1;
        SharedVector v1 = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        v1.add(v2);
        assertEquals(2.0, v1.get(size-1));
    }

    @Test
    public void test18_VecMatMulRowMajorMatrix() {
        System.out.println("Test 18: Vector x Matrix (Row Major Matrix).");
        // Vector: [1, 2]
        // Matrix: [[1, 2], [3, 4]] -> Row Major
        // Result: [1*1 + 2*3, 1*2 + 2*4] = [7, 10]
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][]{{1, 2}, {3, 4}}); // Row major by default
        
        v.vecMatMul(m);
        assertEquals(7.0, v.get(0));
        assertEquals(10.0, v.get(1));
    }

@Test
    public void test19_VecMatMulColumnMajorMatrix() {
        System.out.println("Test 19: Vector x Matrix (Column Major Matrix) - Fixed Interpretation.");
        // Vector: [1, 2]
        // Matrix Input (Standard Rows): [[1, 3], [2, 4]]
        //
        // Logic Matrix:
        // | 1  3 |
        // | 2  4 |
        //
        // Loaded as Column Major means internally it stores:
        // Col 0: [1, 2]
        // Col 1: [3, 4]
        //
        // Calculation:
        // v * Col0 = [1, 2] . [1, 2] = 1*1 + 2*2 = 5
        // v * Col1 = [1, 2] . [3, 4] = 1*3 + 2*4 = 3 + 8 = 11
        
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(new double[][]{{1, 3}, {2, 4}}); 
        
        v.vecMatMul(m);
        
        assertEquals(5.0, v.get(0), 0.0001);
        assertEquals(11.0, v.get(1), 0.0001);
    }

    @Test
    public void test20_VecMatMulInvalidVectorOrientation() {
        System.out.println("Test 20: VecMatMul should fail if vector is Column Major.");
        SharedMatrix m = new SharedMatrix(new double[][]{{1}});
        assertThrows(IllegalArgumentException.class, () -> colVec.vecMatMul(m));
    }

    @Test
    public void test21_VecMatMulDimensionMismatch() {
        System.out.println("Test 21: VecMatMul dimension mismatch check.");
        SharedMatrix m = new SharedMatrix(new double[][]{{1, 2}, {3, 4}, {5, 6}}); // 3 rows
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR); // length 2
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    public void test22_CloningData() {
        System.out.println("Test 22: Verify constructor clones data (encapsulation).");
        double[] raw = {1.0};
        SharedVector v = new SharedVector(raw, VectorOrientation.ROW_MAJOR);
        raw[0] = 99.0;
        assertEquals(1.0, v.get(0), "Vector should store a copy of the array.");
    }
    
    @Test
    public void test23_LockingBasics() {
        System.out.println("Test 23: Call lock methods to ensure no exceptions.");
        assertDoesNotThrow(() -> rowVec.readLock());
        assertDoesNotThrow(() -> rowVec.readUnlock());
        assertDoesNotThrow(() -> rowVec.writeLock());
        assertDoesNotThrow(() -> rowVec.writeUnlock());
    }

    @Test
    public void test24_ZeroLengthVector() {
        System.out.println("Test 24: Zero length vector operations.");
        SharedVector v = new SharedVector(new double[]{}, VectorOrientation.ROW_MAJOR);
        assertEquals(0, v.length());
    }

    @Test
    public void test25_AddNegativeNumbers() {
        System.out.println("Test 25: Adding negative numbers.");
        SharedVector v1 = new SharedVector(new double[]{-1, -2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{-1, -1}, VectorOrientation.ROW_MAJOR);
        v1.add(v2);
        assertEquals(-2.0, v1.get(0));
    }
}