package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        this.vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            this.vectors[i] = new SharedVector(matrix[i],VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
        // input validity check
        if (matrix == null || matrix.length == 0 || matrix[0] == null || matrix[0].length == 0) {
            throw new IllegalArgumentException("Cannot load an empty or null matrix.");
        }
        // initializing a new vectors array
        SharedVector[] newVectors = new SharedVector[matrix.length];

        // iterating the input matrix and using the constructor to create a new matrix
        for (int i = 0; i < matrix.length; i++) {
            newVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR); 
        }
        // adjust the field
        this.vectors = newVectors;
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
        // input validity check
        if (matrix == null || matrix.length == 0 || matrix[0] == null || matrix[0].length == 0) {
            throw new IllegalArgumentException("Cannot load an empty or null matrix.");
        }
        // initializing a new vectors array
        SharedVector[] newVectors = new SharedVector[matrix.length];        

        // iterating the input matrix and using the constructor to create a new matrix
        for (int i = 0; i < matrix.length; i++) {
            newVectors[i] = new SharedVector(matrix[i], VectorOrientation.COLUMN_MAJOR); 
        }
        // adjust the field
        this.vectors = newVectors;                        
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        double[][] result;
        if (isEmpty()) {
            result = new double[0][0];
            return result;
        }

        int numOfRows;
        int numOfCols;

        if (this.getOrientation() == VectorOrientation.COLUMN_MAJOR) { // complicated case
            numOfCols = this.length();
            numOfRows = this.vectors[0].length();
            result = new double[numOfRows][numOfCols];
            // this loop scan the matrix like it was a row major matrix
            for (int i = 0; i < numOfRows; i++) {
                for (int j = 0; j < numOfCols; j++) { 
                    result[i][j] = vectors[j].get(i);
                }
            }
        }

        else { // matrix is row major
            numOfRows = this.length();
            numOfCols = this.vectors[0].length();
            result = new double[numOfRows][numOfCols];
            for (int i = 0; i < numOfRows; i++) {
                for (int j = 0; j < numOfCols; j++) {
                    result[i][j] = vectors[i].get(j);
                }
            }

        }
        return result;
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        return this.vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        // safety check - do not use class method on an empty object
        if (isEmpty()) {
            return 0;
        }
        else {
            return vectors.length;
        }
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        // if matrix is empty or contains only empty vectors
        if (vectors.length == 0 || vectors[0].length() == 0) {
            return null;
        }
        return vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        if (vecs == null) {
            return;
        }        
        for (int i = 0; i < vecs.length; i++) {
            vectors[i].readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        if (vecs == null) {
            return;
        }        
        for (int i = 0; i < vecs.length; i++) {
            vectors[i].readUnlock();
        }        
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        if (vecs == null) {
            return;
        }        
        for (int i = 0; i < vecs.length; i++) {
            vectors[i].writeLock();
        }        
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        if (vecs == null) {
            return;
        }        
        for (int i = 0; i < vecs.length; i++) {
            vectors[i].writeUnlock();
        }        
    }
    // auxiliary methods
    public boolean isEmpty() {
        return (vectors == null || vectors.length == 0 || vectors[0].length() == 0);
    }
}
