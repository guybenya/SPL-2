package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        return null;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        ComputationNodeType type = node.getNodeType();
        
    }

public List<Runnable> createAddTasks() {
    // TODO: return tasks that perform row-wise addition
    // 1. Handle edge cases (empty matrices)
    if (leftMatrix.length() == 0 || rightMatrix.length() == 0) {
        if (leftMatrix.length() != rightMatrix.length()) {
            throw new IllegalArgumentException("Matrix dimension mismatch (empty vs non-empty)");
        }
        return new java.util.ArrayList<>();
    }

    // 2. Validate orientation compatibility
    if (leftMatrix.getOrientation() != rightMatrix.getOrientation()) {
        throw new IllegalStateException("Matrices must have the same orientation");
    }

    // 3. Validate dimension (height) compatibility
    if (leftMatrix.length() != rightMatrix.length()) {
        throw new IllegalArgumentException("Matrix dimensions mismatch: Heights do not match.");
    }

    // Create the task list using the fully qualified name (avoiding extra imports)
    List<Runnable> tasks = new java.util.ArrayList<>(leftMatrix.length());
    int length = leftMatrix.length();

    for (int i = 0; i < length; i++) {
        final int index = i;
        tasks.add(() -> {
            SharedVector v1 = leftMatrix.get(index); // Destination vector (M1)
            SharedVector v2 = rightMatrix.get(index); // Source vector (M2)

            // Acquire write lock on destination (since we modify it)
            v1.writeLock();
            try {
                // Acquire read lock on source
                v2.readLock();
                try {
                    // Perform in-place addition: v1 += v2
                    // Note: SharedVector.add() should handle internal length checks
                    v1.add(v2);
                } finally {
                    v2.readUnlock();
                }
            } finally {
                v1.writeUnlock();
            }
        });
    }
    return tasks;
}

public List<Runnable> createMultiplyTasks() {
    // TODO: return tasks that perform row × matrix multiplication
    List<Runnable> tasks = new java.util.ArrayList<>();
    int rows = leftMatrix.length();

    for (int i = 0; i < rows; i++) {
        final int rowIndex = i;
        tasks.add(() -> {
            SharedVector leftVector = leftMatrix.get(rowIndex);
            
            // 1. Acquire write lock for the row we are updating in leftMatrix
            leftVector.writeLock();
            try {
                // 2. Acquire read locks for all vectors in the right matrix
                // (We iterate manually because acquireAllVectorReadLocks is private)
                int rightLen = rightMatrix.length();
                for (int j = 0; j < rightLen; j++) {
                    rightMatrix.get(j).readLock();
                }

                try {
                    // 3. Perform the multiplication (updates leftVector in-place)
                    leftVector.vecMatMul(rightMatrix);
                } finally {
                    // 4. Release read locks
                    for (int j = 0; j < rightLen; j++) {
                        rightMatrix.get(j).readUnlock();
                    }
                }
            } finally {
                // 5. Release write lock
                leftVector.writeUnlock();
            }
        });
    }
    return tasks;
}

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows

        if (leftMatrix.length() == 0) {
            return new java.util.ArrayList<>();
        }

        List<Runnable> tasks = new java.util.ArrayList<>(leftMatrix.length());
        int length = leftMatrix.length();
        
        for (int i = 0; i < length; i++) {
            final int index = i;
            tasks.add(() -> {
                SharedVector vector = leftMatrix.get(index);
                vector.writeLock();
                try {
                    vector.negate();
                }
                finally {
                    vector.writeUnlock();
                }
            });
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        if (leftMatrix.length() == 0) {
            return new java.util.ArrayList<>();
        }
        List<Runnable> tasks = new java.util.ArrayList<>(leftMatrix.length());
        int length = leftMatrix.length();
        
        for (int i = 0; i < length; i++) {
            final int index = i;
            tasks.add(() -> {
                SharedVector vector = leftMatrix.get(index);
                vector.writeLock();
                try {
                    vector.transpose();
                }
                finally {
                    vector.writeUnlock();
                }
            });
        }
        return tasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return null;
    }
    // auxilairy method
    private boolean dimentionCheck() {
        int leftNumOfRows;
        int leftNumOfCols;
        int rightNumOfRows;
        int rightNumOfCols;
        
        if (leftMatrix.getOrientation() == VectorOrientation.ROW_MAJOR) {
            leftNumOfRows = leftMatrix.length();
            leftNumOfCols = leftMatrix.get(0).length();
        }
        else {
            leftNumOfCols = leftMatrix.length();
            leftNumOfRows = leftMatrix.get(0).length();
        }

        if (rightMatrix.getOrientation() == VectorOrientation.ROW_MAJOR) {
            rightNumOfRows = rightMatrix.length();
            rightNumOfCols = rightMatrix.get(0).length();
        }
        else {
            rightNumOfCols = rightMatrix.length();
            rightNumOfRows = rightMatrix.get(0).length();
        }
        if (leftNumOfRows != rightNumOfRows || leftNumOfCols != rightNumOfCols) {
            return false;
        }
        return true;
        
    }
}
