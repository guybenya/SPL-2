package spl.lae;
import java.io.IOException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
      // TODO: main 
      if(args.length !=3){
        System.out.println("Usage: java -jar LAE.jar <numThreads> <inputFilePath> <outputFilePath>");
        return;
      }
      //How do the args work - args[0] - this represesnts the amount of threads working
      //args[1] - this is the input path - the json file input of the of the matrices
      //args[2] - this is the output path - the json output file that has the result output
      String inputPath = args[1];
      String outputPath = args[2];
      
      try{
        // We create an input parser object to read the input file (json type).
        // The function parse find the relevant computation root so that we could start the calculation of the tree
        InputParser inputParser = new InputParser();
        ComputationNode root = inputParser.parse(inputPath);
        int numOfThreads = Integer.parseInt(args[0]);
        
        //Start fixing the tree and organize it before starting calculations.
        //We use the helper method RecursiveAssociative Nesting to do so.
        //root.recursiveAssociativeNesting();
        
        //Create a new LAE and run the engine putting the result in to a ComputationNode
        LinearAlgebraEngine engine = new LinearAlgebraEngine(numOfThreads);
        ComputationNode result = engine.run(root);

        //Relevant reports of the workers (threads)
        //We use the OutputWriter class that has 2 write methods.
        //the first method we use is for the result it receives a matrix and 
        //adds it to the relevant output file 
        System.out.println(engine.getWorkerReport());
        OutputWriter.write(result.getMatrix(),outputPath);
        engine.shutdown(); // inserted by guy
      } catch(Exception e){
        e.printStackTrace();
        //This is the second use of write which is for errors.
        //As Requested in the task intructions we need to add to the file
        //the relevant errors.
        OutputWriter.write(e.getMessage(), outputPath);
      }
      
    }
}