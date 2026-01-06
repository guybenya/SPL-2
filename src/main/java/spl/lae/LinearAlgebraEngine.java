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

    // public ComputationNode run(ComputationNode computationRoot) {
    //     // TODO: resolve computation tree step by step until final matrix is produced

    //     // make sure tree nodes are nested in a left-associative manner
    //     computationRoot.associativeNesting();

    //     // base case
    //     if (computationRoot.getNodeType() == ComputationNodeType.MATRIX) {
    //         return computationRoot;
    //     }
    //     List<ComputationNode> children = computationRoot.getChildren(); 
    //     // step
    //     // binary operands
    //     if (computationRoot.getNodeType() == ComputationNodeType.MULTIPLY || computationRoot.getNodeType() == ComputationNodeType.ADD) {
    //         // call the method on both left and right children
    //         run(children.get(0));
    //         run(children.get(1));
    //     }
    //     // unary operands
    //     else if (computationRoot.getNodeType() == ComputationNodeType.TRANSPOSE || computationRoot.getNodeType() == ComputationNodeType.NEGATE) {
    //         // call the method on the left child only - there is no right child
    //         run(children.get(0));
    //     }
    //     else {
    //         throw new IllegalArgumentException("undefined operand");
    //     }
    //     // load and compute ensures that the main thread waits until all the tasks are complited
    //     loadAndCompute(computationRoot);

    //     double[][] resultMatrix = this.leftMatrix.readRowMajor();
    //     computationRoot.resolve(resultMatrix);

        
    //     return computationRoot;
    // }


    // edited by sagi - new run function 
    public ComputationNode run(ComputationNode computationRoot) {
        // first of all - making sure the tree is nested
        computationRoot.associativeNesting();

        while (computationRoot.getNodeType() != ComputationNodeType.MATRIX) {
            // finding resolvable node
            ComputationNode resolvable = computationRoot.findResolvable();
            
            if (resolvable == null) break; // if resolveable is null handles the situation and breaks the calculation loop

            loadAndCompute(resolvable);
        }

        try {
            this.executor.shutdown();
        } catch (InterruptedException e) {
            System.out.println("Interrupted run");
        }

        return computationRoot;
    }

    // this method gets a ready to compute node
    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        ComputationNodeType type = node.getNodeType();

        // addition - binary operation
        if (type == ComputationNodeType.ADD) {
            // load left and right matrices as row-major metrices
            this.leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
            this.rightMatrix.loadRowMajor(node.getChildren().get(1).getMatrix());

            // checking dimensions
            if (oneAtLeastIsEmpty()) {
                throw new IllegalArgumentException("cannot perform addition if one of the matrices is empty");
            }

            if (!sameStructure()) {
                throw new IllegalArgumentException("cannot perform addition unless both matrices has the same structure");
            }

            // load add tasks directly to the executer 
            executor.submitAll(createAddTasks()); 
        }

        // multiplication - binary operation
        else if (type == ComputationNodeType.MULTIPLY) {
            // load left as a row-major and right as a column-major
            this.leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
            this.rightMatrix.loadColumnMajor(node.getChildren().get(1).getMatrix());  
            
            // check dimensions
            if (oneAtLeastIsEmpty()) {
                throw new IllegalArgumentException("cannot multiply an empty matrix");
            }

            int leftCols = leftMatrix.get(0).length();
            int rightRows = rightMatrix.get(0).length();

            if (leftCols != rightRows) {
                throw new IllegalArgumentException("dimensions mismatch - leftCols must be equal to rightRows");
            }
            // load multiply tasks directly to the executer
            executor.submitAll(createMultiplyTasks());

        }
        // negation - unary operation
        else if (type == ComputationNodeType.NEGATE) {
            // load left matrix
            this.leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
            
            if (leftMatrix.isEmpty()) {
                throw new IllegalArgumentException("cannot negate an empty matrix");
            }
            // load negation tasks directly to the executer
            executor.submitAll(createNegateTasks());
        }

        // transpose - unary operation
        else if (type == ComputationNodeType.TRANSPOSE) { 
            this.leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());

            if (leftMatrix.isEmpty()) {
                throw new IllegalArgumentException("cannot transpose an empty matrix");
            }
            // load transpose tasks directly to the executer
            executor.submitAll(createTransposeTasks());

        }
        else { // if somehow the node type is matrix
            throw new IllegalArgumentException("Unsupported operation type: " + type);
        } 
        // creating a result matrix to put in the matrix after the calculation ----- added by sagi
        double[][] result = this.leftMatrix.readRowMajor();
        
        node.resolve(result);
    }

public List<Runnable> createAddTasks() {
    // TODO: return tasks that perform row-wise addition

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
        List<Runnable> tasks = new java.util.ArrayList<>(leftMatrix.length());
        int rows = leftMatrix.length();

        for (int i = 0; i < rows; i++) {
            final int rowIndex = i;
            tasks.add(() -> {
                SharedVector leftVector = leftMatrix.get(rowIndex);
                
                // Acquire write lock for the row we are updating in leftMatrix
                leftVector.writeLock();
                try {
                    int rightLen = rightMatrix.length();
                    
                    // Acquire read locks for all vectors in the right matrix
                    // We iterate manually because acquireAllVectorReadLocks is private
                    for (int j = 0; j < rightLen; j++) {
                        rightMatrix.get(j).readLock();
                    }

                    try {
                        // Perform the multiplication (updates leftVector in-place)
                        leftVector.vecMatMul(rightMatrix);
                    } finally {
                        // Release read locks for all vectors in the right matrix
                        for (int j = 0; j < rightLen; j++) {
                            rightMatrix.get(j).readUnlock();
                        }
                    }
                } finally {
                    // Release write lock for the left matrix row
                    leftVector.writeUnlock();
                }
            });
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows

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
        return executor.getWorkerReport();
    }
    // auxilairy methods
    private boolean sameStructure() {
        // 1. Safety check: Handle empty matrices first
        if (oneAtLeastIsEmpty()) {
            // Both must be empty to be considered "same structure"
            return leftMatrix.length() == 0 && rightMatrix.length() == 0;
        }

        int leftNumOfRows;
        int leftNumOfCols;
        int rightNumOfRows;
        int rightNumOfCols;
        
        // measure sizes for left matrix
        if (leftMatrix.getOrientation() == VectorOrientation.ROW_MAJOR) {
            leftNumOfRows = leftMatrix.length();
            leftNumOfCols = leftMatrix.get(0).length();
        } else {
            leftNumOfCols = leftMatrix.length();
            leftNumOfRows = leftMatrix.get(0).length();
        }

        // measure sizes for right matrix
        if (rightMatrix.getOrientation() == VectorOrientation.ROW_MAJOR) {
            rightNumOfRows = rightMatrix.length();
            rightNumOfCols = rightMatrix.get(0).length();
        } else {
            rightNumOfCols = rightMatrix.length();
            rightNumOfRows = rightMatrix.get(0).length();
        }

        // check sizes
        return (leftNumOfRows == rightNumOfRows && leftNumOfCols == rightNumOfCols);
    }

    // isEmpty for binary operands
    private boolean oneAtLeastIsEmpty() {
        return (leftMatrix.isEmpty() || rightMatrix.isEmpty());
    }
}
