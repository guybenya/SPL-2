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
        return null;
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        return this.vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return 0;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        return null;
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
}
