package memory;

import java.util.concurrent.locks.ReadWriteLock;

import javax.management.RuntimeErrorException;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        this.vector = vector;
        this.orientation = orientation;
    }
    // READER
    public double get(int index) {
        // TODO: return element at index (read-locked)
        readLock();
        try {
            return vector[index];
        }
        finally {
            readUnlock();
        }
    }
    // READER
    public int length() {
        // TODO: return vector length
        readLock();
        try {
            return vector.length;
        }
        finally {
            readUnlock();
        }
    }
    // READER
    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        readLock();
        try {
            return orientation;
        }
        finally {
            readUnlock();
        }
    }

    public void writeLock() {
        // TODO: acquire write lock
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        // TODO: release write lock
        lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock
        lock.readLock().lock();
    }

    public void readUnlock() {
        // TODO: release read lock
        lock.readLock().unlock();
    }

    public void transpose() {
        // TODO: transpose vector
        // changing oriantaion field
        writeLock();
        try {
            if (orientation == VectorOrientation.ROW_MAJOR) {
                orientation = VectorOrientation.COLUMN_MAJOR;
            }
            else {
                orientation = VectorOrientation.ROW_MAJOR;
            }
        }
        finally {
            writeUnlock();
        }
    }
    // WRITER
    public void add(SharedVector other) {
        // TODO: add two vectors
        // save the length of this.vector - multiple accesses
        int selfLength = vector.length;
        // using nesting acquire technic for safety
        this.writeLock();
        try {
            other.readLock();
            try {
                // perform a validity check - check if both vectors has the same length
                if (selfLength != other.length()) {
                    throw new IllegalArgumentException("We can only sum two vectors in the same length!");
                }
                // sum both vectors cells into this.vector - using classes fields directly because both vectors are lock
                for (int i = 0; i < selfLength; i++) {
                    this.vector[i] += other.vector[i];
                }                
            }
            finally {
                other.readUnlock();
            }
        }
        finally {
            this.writeUnlock();
        }
    }
    // WRITER
    public void negate() {
        // TODO: negate vector
        writeLock();
        try {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = vector[i] * (-1);
            }
        }
        finally {
            writeUnlock();
        }
        
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        double sum = 0;
        this.readLock();
        try {
            if (this.orientation != VectorOrientation.ROW_MAJOR) {
                throw new IllegalArgumentException("only row · column is possible");
            }
            other.readLock();
            try {
                if (other.vector.length != this.vector.length || other.orientation != VectorOrientation.COLUMN_MAJOR) {
                    throw new IllegalArgumentException("only row · column is possible and both vectors should be in the same length");
                }
                for (int i = 0; i < this.vector.length; i++) {
                    sum += (this.vector[i] * other.vector[i]);
                }
            }
            finally {
                other.readUnlock();
            }
        }
        finally {
            this.readUnlock();
        }
        return sum;
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        // check if the vector is row major
        if (this.orientation != VectorOrientation.ROW_MAJOR) {
            throw new IllegalArgumentException("invalid input");
        }
        // we need to maker sure the number of rows in matrix equales to vector's length 
        // measure matrix rows and columns
        int matRows;
        int matCols;
        VectorOrientation matrixOrientation = matrix.getOrientation();
        // saving run time
        final int binaryOriantaion;

        if (matrixOrientation == VectorOrientation.COLUMN_MAJOR) {
            matCols = matrix.length();
            matRows = matrix.get(0).length();
            binaryOriantaion = 0;
            
        }
        else { // matrix orientation is row major
            matRows = matrix.length();
            matCols = matrix.get(0).length();
            binaryOriantaion = 1;            
        }

        // validation check
        if (this.vector.length != matRows) {
            throw new IllegalArgumentException("cannot perform a calculation");
        }
        // initialize a new vector in with the same length of matCols
        double[] newVector = new double[matCols];

        // we need the condition in order to technically iterate the columns
        // every cell in the new vector will be - (old vector * matching column in the matrix)

        if (binaryOriantaion == 0) { // matrix is col major
            for (int i = 0; i < newVector.length; i++) {
                newVector[i] = this.dot(matrix.get(i));
            }
        }
        else { // matrix is row major
            for (int i = 0; i < newVector.length; i++) {
                for (int j = 0; j < matRows; j++) {
                    newVector[i] += this.vector[j] * matrix.get(j).vector[i];
                }
            }
        }

        // save the result in the share vector
        this.vector = newVector;
    
    }

    
    // auxiliary methods
    // itentionally not a thread safe method!
    public void setVector (double[] arr) { // check with others if they did it with a pointer or deep copy

            // 1. Length check: We can't fit a larger/smaller array into the existing space
            if (this.vector.length != arr.length) {
                throw new IllegalArgumentException("Vector length mismatch. Cannot perform in-place update.");
            }

            // 2. Manual Loop: Copy values one by one (Deep Copy)
            // We overwrite 'this.vector' elements with 'sourceArray' elements.
            for (int i = 0; i < this.vector.length; i++) {
                this.vector[i] = arr[i];
            }
        
    }
}
