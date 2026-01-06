package memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class SharedMatrixTest {

    private SharedMatrix matrix;

    @BeforeEach
    public void setUp() {
        matrix = new SharedMatrix();
    }

    @Test
    public void test01_NewMatrixIsEmpty() {
        System.out.println("Test 01: Verify new matrix is empty.");
        assertTrue(matrix.isEmpty());
        assertEquals(0, matrix.length());
    }

    @Test
    public void test02_ConstructorWithData() {
        System.out.println("Test 02: Verify constructor with data.");
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix(data);
        assertFalse(m.isEmpty());
        assertEquals(2, m.length());
    }

    @Test
    public void test03_LoadRowMajorNull() {
        System.out.println("Test 03: Load Row Major Null throws exception.");
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(null));
    }

    @Test
    public void test04_LoadRowMajorEmpty() {
        System.out.println("Test 04: Load Row Major Empty throws exception.");
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(new double[][]{}));
    }

    @Test
    public void test05_LoadRowMajorValid() {
        System.out.println("Test 05: Load Row Major Valid Data.");
        double[][] data = {{1, 2}, {3, 4}};
        matrix.loadRowMajor(data);
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
        assertEquals(1.0, matrix.get(0).get(0));
    }

    @Test
    public void test06_LoadColumnMajorValid() {
        System.out.println("Test 06: Load Column Major Valid Data.");
        // Input is a standard matrix:
        // 1 2
        // 3 4
        // Loaded as columns, it becomes: Col1=[1,3], Col2=[2,4]
        matrix.loadColumnMajor(new double[][]{{1, 2}, {3, 4}});
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
        assertEquals(2, matrix.length()); // 2 columns
    }

    @Test
    public void test07_ReadRowMajorFromRowMajor() {
        System.out.println("Test 07: Read RowMajor from RowMajor source.");
        double[][] data = {{1, 2, 3}, {4, 5, 6}};
        matrix.loadRowMajor(data);
        double[][] res = matrix.readRowMajor();
        assertArrayEquals(data[0], res[0]);
        assertArrayEquals(data[1], res[1]);
    }

    @Test
    public void test08_ReadRowMajorFromColumnMajor() {
        System.out.println("Test 08: Read RowMajor from ColumnMajor source (Data Consistency).");
        // Input Standard Matrix:
        // 1 2
        // 3 4
        double[][] input = {{1, 2}, {3, 4}};
        
        // This splits it into columns internally, but logically it is still the same matrix
        matrix.loadColumnMajor(input);
        
        // Reading it back should reconstruct the rows correctly
        double[][] res = matrix.readRowMajor();
        
        assertEquals(1.0, res[0][0]);
        assertEquals(2.0, res[0][1]); // Was failing here because I expected transpose
        assertEquals(3.0, res[1][0]);
        assertEquals(4.0, res[1][1]);
    }

    @Test
    public void test09_GetVectorOutOfBounds() {
        System.out.println("Test 09: Get vector out of bounds.");
        matrix.loadRowMajor(new double[][]{{1}});
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(5));
    }

    @Test
    public void test10_GetVectorNegativeIndex() {
        System.out.println("Test 10: Get vector negative index.");
        matrix.loadRowMajor(new double[][]{{1}});
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(-1));
    }

    @Test
    public void test11_ConstructorThrowsOnNull() {
        System.out.println("Test 11: Constructor throws on null input.");
        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(null));
    }

    @Test
    public void test12_LoadEmptyRows() {
        System.out.println("Test 12: Load {{}} should throw.");
        double[][] data = new double[2][0];
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(data));
    }

    @Test
    public void test13_SingleElementMatrix() {
        System.out.println("Test 13: 1x1 Matrix.");
        double[][] data = {{42}};
        matrix.loadRowMajor(data);
        assertEquals(42.0, matrix.get(0).get(0));
    }

    @Test
    public void test14_LargeMatrixLoad() {
        System.out.println("Test 14: Load Large Matrix check.");
        int size = 100;
        double[][] data = new double[size][size];
        assertDoesNotThrow(() -> matrix.loadRowMajor(data));
    }

    @Test
    public void test15_ReloadingData() {
        System.out.println("Test 15: Reloading overwrites previous data.");
        matrix.loadRowMajor(new double[][]{{1}});
        matrix.loadRowMajor(new double[][]{{2}, {3}});
        assertEquals(2, matrix.length());
        assertEquals(2.0, matrix.get(0).get(0));
    }

    @Test
    public void test16_ReadFromEmptyReturnsEmpty() {
        System.out.println("Test 16: Reading empty matrix returns 0x0 array.");
        double[][] res = matrix.readRowMajor();
        assertEquals(0, res.length);
    }

    @Test
    public void test17_OrientationEmptyMatrix() {
        System.out.println("Test 17: Orientation of empty matrix is null.");
        assertNull(matrix.getOrientation());
    }

    @Test
    public void test18_GetReferenceCheck() {
        System.out.println("Test 18: Verify get() returns the actual SharedVector.");
        matrix.loadRowMajor(new double[][]{{1}});
        SharedVector v = matrix.get(0);
        assertNotNull(v);
        assertEquals(1.0, v.get(0));
    }

    @Test
    public void test19_LoadColumnMajorEmptyInput() {
        System.out.println("Test 19: Load Column Major Empty.");
        assertThrows(IllegalArgumentException.class, () -> matrix.loadColumnMajor(new double[][]{}));
    }

    @Test
    public void test20_RectangularMatrixRowMajor() {
        System.out.println("Test 20: 2x3 Matrix.");
        double[][] data = {{1, 2, 3}, {4, 5, 6}};
        matrix.loadRowMajor(data);
        assertEquals(2, matrix.length());
        assertEquals(3, matrix.get(0).length());
    }

    @Test
    public void test21_RectangularMatrixColumnMajor() {
        System.out.println("Test 21: 3x2 Matrix loaded as Column Major.");
        // Input:
        // 1 4
        // 2 5
        // 3 6
        // This is 3 rows, 2 columns.
        double[][] data = {{1, 4}, {2, 5}, {3, 6}};
        matrix.loadColumnMajor(data);
        
        // Should produce 2 vectors (columns)
        assertEquals(2, matrix.length()); 
        
        // Each vector should have length 3 (rows)
        assertEquals(3, matrix.get(0).length()); 
    }
}