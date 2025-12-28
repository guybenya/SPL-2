package spl.lae;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import parser.ComputationNode;
import parser.InputParser;
import parser.OutputWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ProjectIntegrationTest {

    private LinearAlgebraEngine lae;
    private InputParser inputParser;
    private final int NUM_THREADS = 4;
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path tempDir; // JUnit creates a temporary directory for files

    @BeforeEach
    void setUp() {
        lae = new LinearAlgebraEngine(NUM_THREADS);
        inputParser = new InputParser();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (lae != null) {
            lae.shutdown();
        }
    }

    // --- Helper Methods ---

    private File createInputFile(String jsonContent) throws IOException {
        File file = tempDir.resolve("input.json").toFile();
        mapper.writeValue(file, mapper.readTree(jsonContent));
        return file;
    }

    private JsonNode readOutputFile(File outputFile) throws IOException {
        assertTrue(outputFile.exists(), "Output file was not created");
        return mapper.readTree(outputFile);
    }

    private void runEngineEndToEnd(File inputFile, File outputFile) throws Exception {
        // This simulates the flow in Main.java
        ComputationNode root = inputParser.parse(inputFile.getAbsolutePath());
        ComputationNode resultNode = lae.run(root);
        
        // Main.java logic handles writing result OR error. 
        // Since LAE.run throws exception on logical error, we catch it here to simulate Main's behavior
        // or we check how Main handles it. 
        // Assuming Main catches exceptions and writes error file, or LAE handles it.
        // Based on instructions, LAE logic usually throws, and Main catches.
        
        // For the test, we write the result manually if successful
        OutputWriter.write(resultNode.getMatrix(), outputFile.getAbsolutePath());
    }

    // --- Tests ---

    @Test
    void testEndToEndSimpleAddition() throws Exception {
        // Scenario: [[1,2]] + [[3,4]] = [[4,6]]
        String json = """
            {
                "operator": "+",
                "operands": [
                    [[1, 2]],
                    [[3, 4]]
                ]
            }
        """;

        File input = createInputFile(json);
        File output = tempDir.resolve("output.json").toFile();

        // Run the flow
        runEngineEndToEnd(input, output);

        // Verify output
        JsonNode resultJson = readOutputFile(output);
        assertTrue(resultJson.has("result"), "Output should contain 'result' field");
        JsonNode matrix = resultJson.get("result");
        
        assertEquals(4.0, matrix.get(0).get(0).asDouble());
        assertEquals(6.0, matrix.get(0).get(1).asDouble());
    }

    @Test
    void testEndToEndComplexExpression() throws Exception {
        // Scenario: (A * B) - C
        // A=2x2, B=2x2, C=2x2
        // [[1,0],[0,1]] * [[2,2],[2,2]] = [[2,2],[2,2]]
        // Minus [[1,1],[1,1]] = [[1,1],[1,1]]
        
        String json = """
            {
                "operator": "-",
                "operands": [
                    {
                        "operator": "*",
                        "operands": [
                             [[1, 0], [0, 1]],
                             [[2, 2], [2, 2]]
                        ]
                    },
                    [[1, 1], [1, 1]]
                ]
            }
        """;

        File input = createInputFile(json);
        File output = tempDir.resolve("complex_out.json").toFile();

        runEngineEndToEnd(input, output);

        JsonNode resultJson = readOutputFile(output);
        JsonNode matrix = resultJson.get("result");
        
        assertEquals(1.0, matrix.get(0).get(0).asDouble(), 0.001);
        assertEquals(1.0, matrix.get(1).get(1).asDouble(), 0.001);
    }

    @Test
    void testErrorHandlingDimensionMismatch() throws IOException {
        // Scenario: Add 1x1 to 1x2 -> Should fail
        String json = """
            {
                "operator": "+",
                "operands": [
                    [[1]],
                    [[1, 2]]
                ]
            }
        """;

        File input = createInputFile(json);
        File output = tempDir.resolve("error_out.json").toFile();

        // We expect LAE to throw an exception, which Main would catch and write to file.
        // Here we simulate that catch block.
        Exception exception = null;
        try {
            ComputationNode root = inputParser.parse(input.getAbsolutePath());
            lae.run(root);
        } catch (Exception e) {
            exception = e;
            OutputWriter.write(e.getMessage(), output.getAbsolutePath());
        }

        assertNotNull(exception, "Should have thrown an exception for dimension mismatch");
        
        // Verify output file contains error
        JsonNode resultJson = readOutputFile(output);
        assertTrue(resultJson.has("error"), "Output should contain 'error' field");
        assertFalse(resultJson.has("result"), "Output should NOT contain 'result' field");
        System.out.println("Caught expected error: " + resultJson.get("error").asText());
    }

    @Test
    void testParsingErrorInvalidJson() {
        // Create an invalid file (empty array where object expected)
        String json = "[]"; 
        
        File input;
        try {
            input = createInputFile(json);
            // The parser might throw ParseException or IllegalArgumentException
            assertThrows(Exception.class, () -> inputParser.parse(input.getAbsolutePath()));
        } catch (IOException e) {
            fail("Failed to write test file");
        }
    }

    @Test
    void testHeavyLoadForConcurrency() throws Exception {
        // Generate a large JSON input dynamically
        // (A + B) * C^T with large matrices
        int size = 50;
        double[][] m1 = generateMatrix(size);
        double[][] m2 = generateMatrix(size); // m1 + m2
        double[][] m3 = generateMatrix(size); // m3 to transpose
        
        // Construct JSON structure manually or via objects
        // We'll trust the engine logic and just use LAE directly with nodes to save string building mess,
        // but since this is integration test, let's keep it close to reality.
        
        ComputationNode nodeA = new ComputationNode(m1);
        ComputationNode nodeB = new ComputationNode(m2);
        ComputationNode nodeC = new ComputationNode(m3);
        
        ComputationNode add = new ComputationNode(parser.ComputationNodeType.ADD, java.util.List.of(nodeA, nodeB));
        ComputationNode trans = new ComputationNode(parser.ComputationNodeType.TRANSPOSE, java.util.List.of(nodeC));
        ComputationNode root = new ComputationNode(parser.ComputationNodeType.MULTIPLY, java.util.List.of(add, trans));

        // Use assertTimeout to detect deadlocks
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            ComputationNode result = lae.run(root);
            assertNotNull(result.getMatrix());
            assertEquals(size, result.getMatrix().length);
        }, "Execution took too long - possible Deadlock!");
        
        // Check Worker Report
        String report = lae.getWorkerReport();
        System.out.println("Worker Report:\n" + report);
        assertNotNull(report);
        assertTrue(report.contains("Worker Id"), "Report should contain worker details");
    }

    // --- Stress Helper ---
    private double[][] generateMatrix(int size) {
        double[][] m = new double[size][size];
        for(int i=0; i<size; i++) {
            for(int j=0; j<size; j++) {
                m[i][j] = Math.random();
            }
        }
        return m;
    }
}