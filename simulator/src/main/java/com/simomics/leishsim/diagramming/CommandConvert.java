package com.simomics.leishsim.diagramming;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.simomics.leishsim.DiagramGenerator;
import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;

/**
 * Transform a yEd output graphml file into another format. 
 */
@Parameters(commandDescription = "Create a diagram from a yEd output graphml file") 
public class CommandConvert implements DiagramCommand {

	@Parameter(names = "--input-file", description = "Name of the yEd graphml file to convert", required = true)
	private String inputFileName;
	
	@Override
	public void run(DiagramGenerator diagramGenerator) {
		
		// Run python script to convert .graphml into temporary .graph file
		File tempFile = new File(diagramGenerator.outputDirectory, "temp.graph");
		try {
			Process convertProcess = new ProcessBuilder("python", "convert_diagram.py", inputFileName, tempFile.getAbsolutePath())
					.inheritIO()
					.start();
			int retCode = convertProcess.waitFor();
			if (retCode != 0) {
				Logging.getLogger(LogType.ERROR).log(String.format("Failed to run convert_diagram.py Return code: %s", retCode));
			}
		} catch (IOException | InterruptedException e) {
			Logging.getLogger(LogType.ERROR).log("Failed to run convert_diagram.py", e);
		}
		
		// Read .graph file
		Graph graph = Graph.readFromGraphFile(tempFile);
		
		// Write graph out to whichever file format was chosen
		File outputFile = new File(diagramGenerator.outputDirectory, diagramGenerator.outputFileName);
		if (outputFile.getName().endsWith(".xml")) {
			// TODO
			Logging.getLogger(LogType.ERROR).log("Not yet implemented converting to Artoo format!");
		}
		else if (outputFile.getName().endsWith(".json")) {
			graph.writeJsonFile(outputFile);
		}
		else if (outputFile.getName().endsWith(".graphml")) {
			graph.writeGraphmlFile(outputFile);
		}
		else {
			Logging.getLogger(LogType.ERROR).log("Unknown output file extension: " + diagramGenerator.outputFileName);
		}
	}
}
