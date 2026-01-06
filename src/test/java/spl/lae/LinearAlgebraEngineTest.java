package spl.lae;

import org.junit.jupiter.api.Test;
import parser.ComputationNode;
import parser.ComputationNodeType;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class LinearAlgebraEngineTest {

    @Test
    public void testIntegrationSimpleAddition() {
        System.out.println("Test Integration: Simple Matrix Addition (2x2).");
        
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{5, 6}, {7, 8}};
        
        // Construct Tree: ADD(Matrix, Matrix)
        ComputationNode node1 = new ComputationNode(m1);
        ComputationNode node2 = new ComputationNode(m2);
        List<ComputationNode> children = new ArrayList<>();
        children.add(node1);
        children.add(node2);
        
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, children);
        
        LinearAlgebraEngine engine = new LinearAlgebraEngine(4);
        ComputationNode result = engine.run(root);
        
        double[][] resMatrix = result.getMatrix();
        assertEquals(6.0, resMatrix[0][0]);
        assertEquals(8.0, resMatrix[0][1]);
        assertEquals(10.0, resMatrix[1][0]);
        assertEquals(12.0, resMatrix[1][1]);
    }

    @Test
    public void testIntegrationTransposeAndMultiply() {
        System.out.println("Test Integration: Multiply(Matrix, Transpose(Matrix)).");
        // A * B^T
        double[][] a = {{1, 2}, {3, 4}};
        double[][] b = {{1, 0}, {0, 1}}; // Identity
        
        ComputationNode nodeA = new ComputationNode(a);
        ComputationNode nodeB = new ComputationNode(b);
        
        // Transpose Node
        List<ComputationNode> tChildren = new ArrayList<>();
        tChildren.add(nodeB);
        ComputationNode nodeT = new ComputationNode(ComputationNodeType.TRANSPOSE, tChildren);
        
        // Multiply Node
        List<ComputationNode> mChildren = new ArrayList<>();
        mChildren.add(nodeA);
        mChildren.add(nodeT);
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, mChildren);
        
        LinearAlgebraEngine engine = new LinearAlgebraEngine(4);
        ComputationNode result = engine.run(root);
        
        // Result should be same as A since B is identity
        assertEquals(1.0, result.getMatrix()[0][0]);
        assertEquals(4.0, result.getMatrix()[1][1]);
    }

    @Test
    public void testIntegrationDimensionMismatchThrows() {
        System.out.println("Test Integration: Dimension Mismatch throws Exception.");
        double[][] m1 = {{1, 2}};
        double[][] m2 = {{1, 2, 3}};
        
        ComputationNode n1 = new ComputationNode(m1);
        ComputationNode n2 = new ComputationNode(m2);
        List<ComputationNode> children = new ArrayList<>();
        children.add(n1);
        children.add(n2);
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, children);
        
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        
        // Expect runtime exception due to validation inside createAddTasks/submitAll
        // Note: The engine might catch and print, or throw. Based on code, it throws IllegalArgumentException inside tasks
        // or during task creation.
        assertThrows(RuntimeException.class, () -> engine.run(root));
    }
}