package com.simomics.leishsim.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.simomics.leishsim.Driver;
import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.TextFileLogger;

/**
 * Tests of the different logging levels of the simulator.
 */
public class LoggingTest extends OutputTest {

	/**
	 * PRINTED_SPECIES level: The numbers of each species over time.
	 */
	@Test
	public void printedSpeciesLogTest() throws IOException, URISyntaxException {
		runSimulation(LogType.PRINTED_SPECIES);
		
		assertThat("Didn't find species to print message", 
				readLogFile(LogType.PRINTED_SPECIES), containsString("x,y,z,u,v,w,Seconds"));
		assertThat("Didn't find species numbers printed", 
				readLogFile(LogType.PRINTED_SPECIES), containsString("20,100,300,0,0,0,0"));
	}
	
	/**
	 * PRINTED_SPECIES_PER_MEMBRANE level: The numbers of each species over time, for each membrane.
	 */
	@Test
	public void printedSpeciesPerMembraneLogTest() throws IOException, URISyntaxException {
		runSimulation(LogType.PRINTED_SPECIES_PER_MEMBRANE);
		
		assertThat("Didn't find species to print message", 
				readLogFile(LogType.PRINTED_SPECIES_PER_MEMBRANE), containsString("Seconds,u contained simulation_1,v contained simulation_1,w contained simulation_1,x contained membrane_a_1,y contained membrane_a_1,z contained membrane_a_1,x contained membrane_a_2,y contained membrane_a_2,z contained membrane_a_2,x contained membrane_a_3,y contained membrane_a_3,z contained membrane_a_3,u contained membrane_b_1,v contained membrane_b_1,w contained membrane_b_1,x contained membrane_a_4,y contained membrane_a_4,z contained membrane_a_4,x contained membrane_a_5,y contained membrane_a_5,z contained membrane_a_5,x contained membrane_a_6,y contained membrane_a_6,z contained membrane_a_6,x contained membrane_a_7,y contained membrane_a_7,z contained membrane_a_7,x contained membrane_a_8,y contained membrane_a_8,z contained membrane_a_8,x contained membrane_a_9,y contained membrane_a_9,z contained membrane_a_9,x contained membrane_a_10,y contained membrane_a_10,z contained membrane_a_10"));
		assertThat("Didn't find species numbers printed", 
				readLogFile(LogType.PRINTED_SPECIES_PER_MEMBRANE), containsString("0,0,0,0,10,0,30,10,0,30,0,20,30,0,0,0,0,0,30,0,0,30,0,0,30,0,20,30,0,20,30,0,20,30,0,20,30"));
	}
	
	/**
	 * PRINTED_PROPENSITIES level: The propensities of each reaction over time.
	 */
	@Test
	public void printedPropensitiesLogTest() throws IOException, URISyntaxException {
		runSimulation(LogType.PRINTED_PROPENSITIES);
		
		assertThat("Didn't find propensities headings printed", 
				readLogFile(LogType.PRINTED_PROPENSITIES), containsString("Seconds,reaction0,reaction1,reaction2"));
		assertThat("Didn't find propensities printed", 
				readLogFile(LogType.PRINTED_PROPENSITIES), containsString("0,20.0,100.0,300.0"));
	}
	
	/**
	 * REACTION_NUMBERS level: The numbers of each reaction, for linking to propensities file.
	 */
	@Test
	public void reactionNumbersLogTest() throws IOException, URISyntaxException {
		runSimulation(LogType.REACTION_NUMBERS);
		
		assertThat("Didn't find reaction numbers headings printed", 
				readLogFile(LogType.REACTION_NUMBERS), containsString("Propensities file heading,MDSL line number,reaction"));
		assertThat("Didn't find reaction number line", 
				readLogFile(LogType.REACTION_NUMBERS), containsString("reaction0,15,1 x contained membrane_a -> 1 u around membrane_a (forward rate modifier: (1))"));
	}
	
	/**
	 * FULL_STATE_AT_END level: An initial tree and species definitions for the end point of the simulation.
	 */
	@Test
	public void fullStateAtEndLogTest() throws IOException, URISyntaxException {
		runSimulation(LogType.FULL_STATE_AT_END);
		
		assertThat("Didn't find initial tree line", 
				readLogFile(LogType.FULL_STATE_AT_END), containsString("{ [ membrane_a_2 a_1 ] membrane_a }"));
		assertThat("Didn't find species line", 
				readLogFile(LogType.FULL_STATE_AT_END), containsString("species w contained membrane_b_1"));
	}
	
	/**
	 * DEBUG level: The equation of each reaction in the file.
	 */
	@Test
	public void debugLogTest() throws IOException, URISyntaxException {
		runSimulation(LogType.DEBUG);
		
		assertThat("Didn't find x reaction message", 
				readLogFile(LogType.DEBUG), containsString("1 x contained membrane_a -> 1 u around membrane_a (forward rate modifier: (1))"));
		assertThat("Didn't find y reaction message", 
				readLogFile(LogType.DEBUG), containsString("1 y contained membrane_a -> 1 v around membrane_a (forward rate modifier: (1))"));
	}
	
	/**
	 * FULL level: The rate of each reaction as the simulation runs.
	 */
	@Test
	public void fullLogTest() throws IOException, URISyntaxException {
		runSimulation(LogType.FULL);
		
		assertThat("Didn't find enough formula messages",
				StringUtils.countMatches(readLogFile(LogType.FULL), "(forward rate modifier: (1)) (delay: 0.0) in membrane: membrane_a"), greaterThan(30));
		
	}
	
	/**
	 * Test that the FULL_STATE_AT_END log can be re-run as input to the simulator.
	 * 
	 * This code for re-writing the MDSL file assumes that the initial tree is before the species lines.  
	 */
	@Test
	public void reRunFromLogTest() throws IOException, URISyntaxException {
		// Run simulation for a time
		String initialModelFilePath = getModelFilePath("tagged_membranes.mdsl");
		runSimulation(LogType.PRINTED_SPECIES, initialModelFilePath); // need PRINTED_SPECIES level to check that the simulation ran
		
		// Parse log file and re-write input file
		Path logFilePath = ((TextFileLogger) Logging.getLogger(LogType.FULL_STATE_AT_END)).getLogFile().toPath();
		String logFileDirectory = ((TextFileLogger) Logging.getLogger(LogType.FULL_STATE_AT_END)).getLogFile().getParent();
		String newModelFilePath = Paths.get(logFileDirectory, "tagged_membranes_new.mdsl").toAbsolutePath().toString();
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newModelFilePath, false), "utf-8"))) {
			writer.write("#\n# New MDSL file written by LoggingTest.reRunFromLogTest()\n#\n");
			for (String line: Files.readAllLines(logFilePath)) {
				writer.write(line);
				writer.write('\n');
			}
			writer.write("# Reactions and parameters from original file:\n");
			boolean inSpecies = false;
			boolean afterInitialTree = false;
			for (String line: Files.readAllLines(Paths.get(initialModelFilePath))) {
				if (line.startsWith("species")) {
					inSpecies = true;
				}
				else {
					if (inSpecies) {
						afterInitialTree = true;
					}
					if (afterInitialTree) {
						writer.write(line);
						writer.write('\n');
					}
				}
			}
		}
		
		Map<String, Integer> simulationStateAfterFirstRun = getLineOfSpeciesFile(-1);
		
		// Run simulation again
		runSimulation(LogType.PRINTED_SPECIES, newModelFilePath); // need PRINTED_SPECIES level to check that the simulation ran
		
		Map<String, Integer> simulationStateAfterSecondRun = getLineOfSpeciesFile(-1);
		
		// Check that state is different after second run than after first run
		assertThat("Not got any species concentrations output from first run", 
				simulationStateAfterFirstRun.isEmpty(), is(false));
		for (String key: simulationStateAfterFirstRun.keySet()) {
			if (key.equals("Seconds")) {
				assertThat("Simulation time should be greater than zero", 
						new Integer(simulationStateAfterSecondRun.get(key)), greaterThan(0));
				assertThat("Simulation times should be the same, but they are not",
						simulationStateAfterSecondRun.get(key), is(simulationStateAfterFirstRun.get(key)));
			}
			else {
				assertThat("Concentrations of species " + key + " are the same - should be different",
						simulationStateAfterSecondRun.get(key), is(not(simulationStateAfterFirstRun.get(key))));
			}
		}
		
		// Check that unique tags didn't duplicate when written out the second time
		String[] lines = readLogFile(LogType.FULL_STATE_AT_END).split("\n");
		assertThat("Didn't find initial tree in FULL_STATE_AT_END output file", 
				lines.length, greaterThan(0));
		for (String line: lines) {
			if (line.trim().startsWith("{")) {
				// assume: initial tree line for a single membrane
				String tagsString = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
				String[] tagsArray = tagsString.trim().split(" ");
				Set<String> tagsNoDuplicates = new HashSet<>();
				tagsNoDuplicates.addAll(Arrays.asList(tagsArray));
				
				assertThat("Didn't find any tags for membrane: " + line,
						tagsArray.length, greaterThan(0));
				assertThat("Duplicate tag found for membrane: " + line,
						tagsArray.length, is(tagsNoDuplicates.size()));
			}
		}
	}
	
	private void runSimulation(LogType logType) {
		runSimulation(logType, getModelFilePath("tagged_membranes.mdsl"));
	}
	
	private void runSimulation(LogType logType, String modelFilePath) {
		Driver.main(new String[]{ 
				"--hours", "1", 
				"--seconds-before-print", "100",
				"--model-file", modelFilePath,
				"--print", "all", 
				"--log-level", logType.name(),
		});
	}
}
