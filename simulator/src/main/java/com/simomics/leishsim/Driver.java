package com.simomics.leishsim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.simulation.Simulation;
import com.simomics.leishsim.simulation.Simulation.InvalidSimulationException;

/**
 * Main class that users run from the command line. 
 */
public class Driver {
	@Parameter(names = "--hours", description = "Number of (simulation) hours for which to run the simulation", required = false)
	private Integer numHours = null;
	
	@Parameter(names = "--seconds", description = "Number of (simulation) seconds for which to run the simulation", required = false)
	private Integer numSeconds = null;

	/**
	 * Random number generator takes a Long as its seed, but only uses 32 bits, so have to use an Int.
	 */
	@Parameter(names = "--random-seed", description = "Seed for the random number generator", required = false)
	private Integer randomSeed = null;

	@Parameter(names = "--model-file", description = "MDSL file containing the model to simulate", required = true)
	private String modelFileName;

	@Parameter(names = {"--print", "-p"}, description = "Print this species to the output")
	private List<String> namesToPrint = Arrays.asList(new String[] {"LD", "INFg", "IL10"});

	@Parameter(names = {"--hours-before-print"}, description = "Print out the results in chunks of this many hours", required = false)
	private Integer hoursBeforePrint = null;
	
	@Parameter(names = {"--seconds-before-print"}, description = "Print out the results in chunks of this many seconds", required = false)
	private Integer secondsBeforePrint = null;

	@Parameter(names = "--log-level", description = "How much logging to print. Values are: ERROR, WARNING, WARNING_FIX, PROGRESS, PARAMETERS, TAGS, FULL_STATE_AT_END, PRINTED_SPECIES, PRINTED_SPECIES_PER_MEMBRANE, REACTION_NUMBERS, PRINTED_PROPENSITIES, DETAIL, DEBUG, FULL.", required = false)
	private LogType logLevel = LogType.PRINTED_SPECIES;
	
	@Parameter(names = "--log-dir", description = "The directory into which to write the log files. Any pervious log files in this directory will be deleted.", required = false)
	private String logsDirectoryName = "logs";
	
	/** Off by default, so that running tests doesn't send loads of files to AWS. */
	@Parameter(names = "--run-analysis", description = "Whether to run the analysis script after the simulation finishes")
	private boolean runAnalysis = false;
	
	/** If true, then user has requested a number of hours to run (rather than a number of seconds) */
	private boolean runHours;

	/** If true, then the user has requested a number of hours to print (rather than a number of seconds) */
	private boolean printHours;

	/** A File version of {@link #logsDirectoryName} */
	private File logsDirectory;

	/** The simulation that is running */
	private Simulation simulation;

	/**
	 * Entry point for the program. 
	 * @param args Command line arguments. See the member variables of this class annotated with @Parameter.
	 */
	public static void main(String... args) {

		// Parse command line arguments
		Driver driver = new Driver();
		JCommander commandLineArgs = new JCommander(driver);
		try {
			commandLineArgs.parse(args);
		}
		catch(ParameterException e) {
			// Could not parse command line arguments
			Logging.getLogger(LogType.ERROR).log(e.getMessage());
			commandLineArgs.usage();
			return;
		}
		
		// Start the simulation
		try {
			driver.run();
		} catch (InvalidModelException | InvalidSimulationException e) {
			Logging.getLogger(LogType.ERROR).log(e.getMessage());
		}
	}

	/**
	 * Starts the simulation running.
	 * @throws InvalidModelException if the model file could not be parsed.
	 * @throws InvalidSimulationException If anything went wrong when running the simulation.
	 */
	private void run() throws InvalidModelException, InvalidSimulationException {
		logsDirectory = new File(logsDirectoryName);
		Logging.resetLogging(logsDirectory, logLevel);
		
		// Check the input parameters
		tidyInputParameters();
		logInputParameters();
		
		// Preprocess the input file if necessary
		File mdslFile = preprocessMdslFile(new File(modelFileName));
		
		// Read the model file and initialise the simulation
		simulation = new SimulationFactory().createSimulation(mdslFile, randomSeed);

		// Update any parameters after parsing the model file
		tidySimulationParameters();
		
		// Print out reaction numbers for linking to propensities file
		if (Logging.isLoggable(LogType.REACTION_NUMBERS)) {
			Logger reactionNumbersLogger = Logging.getLogger(LogType.REACTION_NUMBERS);
			reactionNumbersLogger.log("Propensities file heading,MDSL line number,reaction");
			int reactionNumber = -1;
			for (Reaction reaction: simulation.getModel().getReactions()) {
				reactionNumber += 1;
				reactionNumbersLogger.log(String.format("reaction%d,%d,%s", reactionNumber, reaction.getMdslLineNumber(), reaction));
			}
		}
		
		// Run the simulation
		Logging.getLogger(LogType.PROGRESS).log("Starting simulation");
		boolean stoppedEarly = simulation.runSeconds(numSeconds, secondsBeforePrint, namesToPrint, printHours);
		
		if (stoppedEarly) {
			Logging.getLogger(LogType.PROGRESS).log("Simulation stopped early at " + simulation.getCurrentSeconds() + " seconds");
		}
		else {
			Logging.getLogger(LogType.PROGRESS).log("Simulation finished at " + simulation.getCurrentSeconds() + " seconds");
		}
		
		// Print out state of simulation for restarting later
		if (Logging.isLoggable(LogType.FULL_STATE_AT_END)) {
			Logging.getLogger(LogType.FULL_STATE_AT_END).log(String.format("\n# State of simulation at %f seconds:\n\n%s",
					simulation.getCurrentSeconds(), simulation.getCurrentState().toFullStateString()));
		}
		
		// Analyse the simulation results
		if (runAnalysis) {
			runAnalysisScript();
		}
	}
	
	/**
	 * Preprocess an MDSL file, to remove all includes.
	 * @param mdslFile The file to preprocess.
	 * @return The location of the preprocessed file.
	 * @throws InvalidModelException If the preprocessing failed.
	 */
	private File preprocessMdslFile(File mdslFile) throws InvalidModelException {
		String fileName = mdslFile.getName();
		if (fileName.endsWith(".mdsl")) {
			fileName = fileName.substring(0, fileName.length() - 5) + ".preprocessed.mdsl";
		}
		else {
			fileName = "preprocessed.mdsl";
		}
		File preprocessedFile = new File(logsDirectory, fileName);
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(preprocessedFile, false), "utf-8"))) {
	        includeMdslFile(mdslFile, writer);
	        return preprocessedFile;
	    } catch (IOException e) {
	        Logging.getLogger(LogType.ERROR).log("Could not write to file: " + preprocessedFile, e);
	        return null;
	    }
	}
	
	private void includeMdslFile(File fileToInclude, Writer outputFile) throws InvalidModelException {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileToInclude))) {
	        String line = null;
	        while ((line = reader.readLine()) != null) {
	        	if (line.trim().toLowerCase().startsWith("include")) {
	        		// Include directive, so do the include
	        		String fileName = line.trim().substring(7).trim();
	        		File childIncludeAbsolute = new File(fileName);
	        		if (childIncludeAbsolute.exists()) {
	        			// Include using an absolute path
	        			includeMdslFile(childIncludeAbsolute, outputFile);
	        		}
	        		else {
	        			File childIncludeRelative = new File(fileToInclude.getParentFile(), fileName);
	        			if (childIncludeRelative.exists()) {
	        				// Include using a relative path
	        				includeMdslFile(childIncludeRelative, outputFile);
	        			}
	        			else {
	        				// Couldn't find include file
	        				throw new InvalidModelException(String.format("Could not read include file. Tried '%s' and '%s'",
	        						childIncludeAbsolute, childIncludeRelative), null);
	        			}
	        		}
	        	}
	        	else {
	        		// Not a preprocessor directive, so just copy the line
		        	outputFile.write(line);
		        	outputFile.write("\n");
	        	}
	        }
	    } catch (IOException e) {
	    	throw new InvalidModelException("Could not find file: " + fileToInclude, e);
	    }
	}
	
	/**
	 * Compute any input parameters that depend on other input parameters.
	 */
	private void tidyInputParameters() {
		runHours = false;
		if (numHours != null && numSeconds != null) {
			Logging.getLogger(LogType.ERROR).log("Can't specify both --num-hours and --num-seconds");
			return;
		}
		if (numHours == null && numSeconds == null) {
			// Not specified how long to run for - use default values
			numHours = 30;
			numSeconds = numHours * 60 * 60;
			runHours = true;
		}
		else if (numSeconds == null) {
			numSeconds = numHours * 60 * 60;
			runHours = true;
		}
		
		printHours = false;
		if (hoursBeforePrint != null && secondsBeforePrint != null) {
			Logging.getLogger(LogType.ERROR).log("Can't specify both --hours-before-print and --seconds-before-print");
			return;
		}
		if (hoursBeforePrint == null && secondsBeforePrint == null) {
			// Not specified how often to print - use default values
			if (runHours) {
				hoursBeforePrint = 1;
				secondsBeforePrint = hoursBeforePrint * 60 * 60;
				printHours = true;
			}
			else {
				secondsBeforePrint = 5;
			}
		}
		else if (secondsBeforePrint == null) {
			secondsBeforePrint = hoursBeforePrint * 60 * 60;
			printHours = true;
		}
	}
	
	/**
	 * Compute any input parameters that depend on having parsed the model file
	 */
	private void tidySimulationParameters() {
		// "Print all"
		if (namesToPrint.size() == 1 && namesToPrint.get(0).equalsIgnoreCase("all")) {
			namesToPrint.clear();
			namesToPrint.addAll(simulation.getModel().getSpeciesNames());
		}
	}
	
	/**
	 * Print all the input parameters to the progress log.
	 */
	private void logInputParameters() {
		Logging.getLogger(LogType.PROGRESS).log("Input parameters: ");
		Logging.getLogger(LogType.PROGRESS).log("  Model file: " + modelFileName);
		if (runHours) {
			Logging.getLogger(LogType.PROGRESS).log("  Number of hours: " + numHours);
		}
		else {
			Logging.getLogger(LogType.PROGRESS).log("  Number of seconds: " + numSeconds);
		}
		if (printHours) {
			Logging.getLogger(LogType.PROGRESS).log("  Hours before printing: " + hoursBeforePrint);	
		}
		else {
			Logging.getLogger(LogType.PROGRESS).log("  Seconds before printing: " + secondsBeforePrint);
		}
		Logging.getLogger(LogType.PROGRESS).log("  Names to print: " + StringUtils.join(namesToPrint, ", "));
		Logging.getLogger(LogType.PROGRESS).log("  Log directory: " + logsDirectoryName);
		Logging.getLogger(LogType.PROGRESS).log("  Log level: " + logLevel.name());
	}
	
	private void runAnalysisScript() {
		List<String> command = readPythonCommand();
		command.add(new File("analyse_results.py").getAbsolutePath());
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(logsDirectory);
		try {
			Process process = processBuilder.start();

			// Print the output of the analysis script
			BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
			for(String outputLine; (outputLine = output.readLine()) != null; ) {
				Logging.getLogger(LogType.PROGRESS).log(outputLine);
			}
			BufferedReader errors = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			for(String errorLine; (errorLine = errors.readLine()) != null; ) {
				Logging.getLogger(LogType.ERROR).log(errorLine);
			}

		} catch (IOException e) {
			Logging.getLogger(LogType.ERROR).log("Failed to run analysis script", e);
		}
	}
	
	/**
	 * Reads the file containing the command to run Python.
	 * @return A string array suitable for passing to ProcessBuilder.
	 */
	private List<String> readPythonCommand() {
		final String fileName = "python_command.txt";
		List<String> fileContents;
		try {
			fileContents = Files.readAllLines(Paths.get(fileName));
		} catch (IOException e) {
			Logging.getLogger(LogType.WARNING).log(
					String.format("Could not read %s file. Defaulting to 'python'.", fileName), 
					e);
			return Arrays.asList(new String[] { "python" });
		}
		List<String> command = new ArrayList<>();
		for (String line: fileContents) {
			for (String part: line.split("\\s")) {
				part = part.trim();
				if (!part.isEmpty()) {
					command.add(part);
				}
			}
		}
		if (command.isEmpty()) {
			Logging.getLogger(LogType.WARNING).log(String.format("%s file is empty", fileName));
		}
		return command;
	}
}
