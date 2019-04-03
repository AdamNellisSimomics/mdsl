package com.simomics.leishsim.diagramming;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.simomics.leishsim.DiagramGenerator;
import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.model.StoichiometrySpecies;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.simulation.Simulation;

/**
 * Transform an MDSL file into a diagram file.
 * Can do simple layout, but can also output yEd graphml so that yEd can be used for more complex layouts.
 */
@Parameters(commandDescription = "Create a diagram from an MDSL file") 
public class CommandCreate implements DiagramCommand {
	
	@Parameter(names = "--model-file", description = "MDSL file containing the model to convert to a diagram", required = true)
	private String modelFileName;

	@Parameter(names = "--tag", description = "Only show nodes with the given tag(s)")
	private List<String> tags = new ArrayList<>();
	
	/**
	 * Run the command.
	 */
	@Override
	public void run(DiagramGenerator diagramGenerator) {
		// Set up logging
		File outputDir = new File(diagramGenerator.outputDirectory);
		Logging.resetLogging(outputDir, LogType.PROGRESS);
		Logging.getLogger(LogType.PROGRESS).log("Diagram generation tool.");
		
		// Process input parameters
		File modelFile = new File(modelFileName);
		File outputFile = new File(diagramGenerator.outputDirectory, diagramGenerator.outputFileName);
		
		// Print out input parameters
		Logging.getLogger(LogType.PROGRESS).log("Parameters:");
		Logging.getLogger(LogType.PROGRESS).log("  Model file: " + modelFileName);
		Logging.getLogger(LogType.PROGRESS).log("  Output directory: " + diagramGenerator.outputDirectory);
		Logging.getLogger(LogType.PROGRESS).log("  Output file: " + diagramGenerator.outputFileName);
		
		// Parse model file
		Simulation simulation;
		try {
			simulation = new SimulationFactory().createSimulation(modelFile, null);
		} catch (InvalidModelException e) {
			Logging.getLogger(LogType.ERROR).log("Could not parse log file: " + e.getMessage());
			return;
		}
		
		// Create graph
		Graph graph;
		if (tags.isEmpty()) {
			graph = Graph.create(simulation.getModel());
		}
		else {
			graph = Graph.create(simulation.getModel(), tags);
		}
		graph.layoutCircularClusters();
		graph.layoutShift(50, 50);
		
		// Write diagram file
		if (outputFile.getName().endsWith(".xml")) {
			writeXmlFile(modelFile, outputFile, simulation);
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

	/**
	 * Write out the diagram as an XML file, for importing into Artoo.
	 * 
	 * Not put this in the Graph class, as it uses edges with multiple sources and destinations
	 *   Artoo supports this, but the Graph class does not.
	 */
	private void writeXmlFile(File modelFile, File outputFile, Simulation simulation) {
		Logging.getLogger(LogType.PROGRESS).log("Creating diagram " + outputFile.getAbsolutePath());
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, false), "utf-8"))) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<diagrams>\n");
			writer.write("  <diagram type=\"MDSL-platform-model\" id=\"0\" name=\"" + modelFile.getName() + "\">\n");
			writer.write("    <description><![CDATA[<p>A <strong>really good</strong> description will go here at <em>some</em> point...</p>]]></description>\n");
			
			// Create a node for each species in the simulation
			// Lay out nodes on a circle for now
			Map<String, Integer> speciesIndexes = new LinkedHashMap<>();
			Integer currentSpeciesIndex = -1;
			double layoutRadius = 300d;
			double xOffset = 400d;
			double yOffset = 400d;
			Set<String> speciesNames = simulation.getModel().getSpeciesNames();
			for (String speciesName: speciesNames) {
				currentSpeciesIndex += 1;
				speciesIndexes.put(speciesName, currentSpeciesIndex);
				
				double layoutAngle = 2 * Math.PI * currentSpeciesIndex / speciesNames.size();
				double layoutX = layoutRadius * Math.cos(layoutAngle);
				double layoutY = layoutRadius * Math.sin(layoutAngle);
				writer.write(String.format("    <node type=\"species\" id=\"%d\" x=\"%d\" y=\"%d\">\n", 
						speciesIndexes.get(speciesName), Math.round(layoutX + xOffset), Math.round(layoutY + yOffset)));
				writer.write("      <content><![CDATA[<p>" + speciesName + "</p>]]></content>\n");
				writer.write("    </node>\n");
			}
			
			// Create an edge for each reaction in the simulation
			Map<Reaction, Integer> reactionIndexes = new LinkedHashMap<>();
			Integer currentReactionIndex = currentSpeciesIndex;
			Set<Reaction> reactions = simulation.getModel().getReactions();
			for (Reaction reaction: reactions) {
				currentReactionIndex += 1;
				reactionIndexes.put(reaction, currentReactionIndex);
				
				writer.write(String.format("    <connection type=\"multiple\" id=\"%d\">\n", reactionIndexes.get(reaction)));
				writer.write("      <label><![CDATA[<p>" + reaction.getRateModifier() + "</p>]]></label>\n");
				writer.write("      <content><![CDATA[<p>" + reaction + "</p>]]></content>\n");
				
				// Create a connection end for each species in this reaction 
				// TODO: Ignoring locations and stoichiometries for now
				for (StoichiometrySpecies stoichSpecies: reaction.getConsumedSpecies()) {
					writer.write(String.format("      <end type=\"from\" nodeid=\"%d\" />\n", 
							speciesIndexes.get(stoichSpecies.getSpeciesName())));
				}
				for (StoichiometrySpecies stoichSpecies: reaction.getProducedSpecies()) {
					writer.write(String.format("      <end type=\"to\" nodeid=\"%d\" />\n", 
							speciesIndexes.get(stoichSpecies.getSpeciesName())));
				}
				
				writer.write("    </connection>\n");
			}
			
			writer.write("  </diagram>\n");
			writer.write("</diagrams>\n");
			
			Logging.getLogger(LogType.PROGRESS).log("Diagram created.");
		} catch (IOException e) {
			// Failed to write to the file
			Logging.getLogger(LogType.ERROR).log("Problem writing to output file " + outputFile, e);
		}
	}
}
