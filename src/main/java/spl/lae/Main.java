package spl.lae;
import java.io.IOException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
      // TODO: main 
      double[][] matrix = {
            {1.0, 2.0, 3.0}, // matrix[0] -> השורה הראשונה
            {4.0, 5.0, 6.0}, // matrix[1] -> השורה השנייה
            {7.0, 8.0, 9.0}  // matrix[2] -> השורה השלישית
        };

      System.out.println(java.util.Arrays.toString(matrix[0]));
      
    }
}