package memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SharedMatrixTest {

    private SharedMatrix matrix;

    @BeforeEach
    public void setUp() {
        matrix = new SharedMatrix();
    }

    @Test
    public void testConstructor_NullInput_ThrowsException() {
        // בודק שזריקת השגיאה תקינה
        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(null));
    }

    @Test
    public void testConstructor_DeepCopy() {
        double[][] data = {{1.0}};
        SharedMatrix m = new SharedMatrix(data);
        data[0][0] = 999.0;
        assertEquals(1.0, m.get(0).get(0), "Matrix should perform deep copy");
    }

    @Test
    public void testLoadRowMajor_StructuralChange() {
        matrix.loadRowMajor(new double[][]{{1, 2}}); // 1x2
        assertEquals(1, matrix.length());
        
        matrix.loadRowMajor(new double[][]{{1}, {2}, {3}}); // 3x1
        assertEquals(3, matrix.length());
    }

    @Test
    public void testLoadColumnMajor_Logic() {
        // קלט: מטריצה 2x2
        // 1 2
        // 3 4
        double[][] input = {{1, 2}, {3, 4}};
        matrix.loadColumnMajor(input);
        
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
        // מכיוון שאנו טוענים עמודות, הוקטורים הפנימיים צריכים לייצג את העמודות של המטריצה המקורית
        assertEquals(2, matrix.length()); // 2 עמודות
        
        // בדיקת עמודה 0: צריכה להיות [1, 3]
        assertEquals(1.0, matrix.get(0).get(0));
        assertEquals(3.0, matrix.get(0).get(1));
        
        // בדיקת עמודה 1: צריכה להיות [2, 4]
        assertEquals(2.0, matrix.get(1).get(0));
        assertEquals(4.0, matrix.get(1).get(1));
    }

        /**
     * Test loadColumnMajor with structural changes.
     */
    @Test
    @DisplayName("Logic: Column-Major Transposition (2x3 -> 3 Vectors)")
    void testTransposeLogic() {
        double[][] data = {
            {10, 20, 30},
            {40, 50, 60}
        };
        System.out.println("zibi");
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data);

        assertEquals(3, matrix.length(), "Storage should have 3 vectors (one per column).");

        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());

        SharedVector firstCol = matrix.get(0);
        assertEquals(10.0, firstCol.get(0));
        assertEquals(40.0, firstCol.get(1));
        
        SharedVector lastCol = matrix.get(2);
        assertEquals(30.0, lastCol.get(0));
        assertEquals(60.0, lastCol.get(1));
    }

    @Test
    public void testReadRowMajor_FromColumnMajor() {
        // קלט:
        // 1 2
        // 3 4
        double[][] input = {{1, 2}, {3, 4}};
        matrix.loadColumnMajor(input);
        
        // הפונקציה readRowMajor צריכה לדעת "להפוך חזרה" ולהחזיר את המטריצה המקורית (שורות)
        double[][] result = matrix.readRowMajor();
        
        // אנחנו מצפים לקבל חזרה בדיוק את מה שהכנסנו
        assertArrayEquals(input[0], result[0]); // [1, 2]
        assertArrayEquals(input[1], result[1]); // [3, 4]
    }

    @Test
    public void testReadRowMajor_Empty() {
        double[][] res = matrix.readRowMajor();
        assertNotNull(res);
        assertEquals(0, res.length);
    }
}