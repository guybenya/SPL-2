package memory;

public class main {
    public static void main(String[] args) {
        double[][] matrix = new double[3][4];
        double num = 1.0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                matrix[i][j] = num;
                num += 1.0;
            }
        }
        System.out.println(matrix.length);
    }
}
