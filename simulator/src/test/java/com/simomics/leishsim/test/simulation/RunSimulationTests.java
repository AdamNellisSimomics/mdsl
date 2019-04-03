package com.simomics.leishsim.test.simulation;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.simomics.leishsim.Driver;
import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.simulation.Simulation;
import com.simomics.leishsim.simulation.Simulation.InvalidSimulationException;
import com.simomics.leishsim.test.OutputTest;

/**
 * Tests of the simulation running.
 */
public class RunSimulationTests extends OutputTest {

	/**
	 * Test that we can run a simulation and see the results changing.
	 */
	@Test
	public void runSimulationTest() throws InvalidModelException, InvalidSimulationException, IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--hours", "30",
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("rates.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});
		
		// Check that B was half its initial concentration at its half life time
		float percentageTolerance = 10f; // percentage of the initial number that we can be off by at the half-life time
		Map<String, Integer> initialState = getLineOfSpeciesFile(1);
		Map<String, Integer> halfTimeState = getLineOfSpeciesFile(12);
		Integer initialB = initialState.get("B");
		Integer halfTimeB = halfTimeState.get("B");
		float tolerance = initialB * percentageTolerance / 100f;
		assertThat("B not decayed according to its half life equation", 
				new Double(halfTimeB), closeTo(initialB / 2, tolerance));
	}
	
	/**
	 * Test that we can run the IFNg pathway file
	 */
	@Test
	public void runIFNgPathwayTest() throws InvalidModelException, IOException, URISyntaxException {
		// Run simulation
		Driver.main(new String[]{ 
				"--hours", "10",
				"--seconds-before-print", "3600",
				"--model-file", getModelFilePath("IFNg_test_pathway_1.mdsl"),
				"--print", "IFNg",
				"--print", "Jak1",
				"--print", "Jak2",
				"--print", "STAT1_inactive", "--print", "STAT1",
				"--print", "socs1", 
				"--print", "NOS2_producing", "--print", "NO", 
				"--print", "ACTIVATE",
				"--log-level", LogType.DETAIL.name(),
		});
		// Check we got the last progress line
		String[] lines = readLogFile(LogType.PROGRESS).split("\n");
		String lastProgressLine = lines[lines.length - 2];
		assertThat("Simulation didn't run for long enough", lastProgressLine, containsString("Run 36000 of 36000 seconds"));
	}
	
	/**
	 * Test how quickly we can run the CD4 test file
	 */
	@Test
	public void runCd4Test() throws InvalidModelException, IOException, URISyntaxException {
		// Run simulation
		Driver.main(new String[]{ 
				"--hours", "50", //"96",
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("cd4_test.mdsl"),
				"--print", "all", 
				"--log-level", LogType.PRINTED_SPECIES.name(),
				"--log-dir", "logs_before",
		});
		// Check we got the last progress line
		String[] progressLines = readLogFile(LogType.PROGRESS).split("\n");
		String lastProgressLine = progressLines[progressLines.length - 2];
		assertThat("Simulation didn't run for long enough", lastProgressLine, containsString("Run 180000 of 180000 seconds (100.0%)"));
		// Check that some reactions fired
		String[] speciesLines = readLogFile(LogType.PRINTED_SPECIES).split("\n");
		String[] first = speciesLines[1].split(",");
		List<String> firstNumbers = Arrays.asList(first).subList(1, first.length);
		String[] last = speciesLines[speciesLines.length - 1].split(",");
		List<String> lastNumbers = Arrays.asList(last).subList(1, last.length);
		assertEquals("Not enough printed species first", firstNumbers.size(), 21);
		assertEquals("Not enough printed species last", firstNumbers.size(), lastNumbers.size());
		assertThat("Values didn't change", firstNumbers, not(lastNumbers));
	}
	
	/**
	 * Test that we can predict species numbers.
	 * Runs the differentiation test model and measures the ratio of the two species at the end.
	 * Repeats the measurement a number of times to average out random numbers.
	 */
	@Test
	public void predictSpeciesRatiosTest() throws InvalidModelException, InvalidSimulationException {
		Logging.setLogLevel(LogType.WARNING);
		
		// Names of species and rates in the model file
		String modelFileName = "/model_files/differentiation_test.mdsl";
		String speciesName1 = "Th1I_exp";
		String speciesName2 = "Th2_exp";
		String species1Rate = "Th0_exp_Th1I_rate";
		String species2Rate = "Th0_exp_Th2_rate";

		// Tolerance on how accurate the result should be
		int numRepeats = 200;
		double tolerance = 0.02d;
		
		// Run the simulation multiple times and measure the ratio of the two species at the end
		double totalRatio = 0;
		Simulation simulation = null;
		for (int i=0; i<numRepeats; i++) {
			simulation = new SimulationFactory().createSimulation(getClass().getResourceAsStream(modelFileName), null);
			simulation.runForSeconds(30 * 60 * 60); // Simulation finishes within one hour, so anything over one hour is large enough
			double ratio = (double) simulation.getCurrentState().getAllSpecies(speciesName1) / simulation.getCurrentState().getAllSpecies(speciesName2);
			totalRatio += ratio;
		}
		
		// Check that the measured ratio matches the ratio of the reaction rates
		// The ratio of the species at the end should be the same as the ratio of the reaction rates
		double measuredRatio = totalRatio / numRepeats;
		double expectedRatio = (double) simulation.getModel().getParameterValue(species1Rate) / simulation.getModel().getParameterValue(species2Rate);
		assertThat("Measured ratio doesn't match expected ratio", 
				measuredRatio, closeTo(expectedRatio, tolerance));
	}
	
	/**
	 * Test that we can predict end times.
	 * Runs the differentiation test model and measures the end time.
	 * Repeats the measurement a number of times to average out random numbers.
	 */
	@Test
	public void predictEndTimesTest() throws InvalidModelException, InvalidSimulationException {
		Logging.setLogLevel(LogType.WARNING);

		// Names of species and rates in the model file
		String modelFileName = "/model_files/differentiation_test.mdsl";
		String species0Name = "Th0_exp";
		String species1Rate = "Th0_exp_Th1I_rate";
		String species2Rate = "Th0_exp_Th2_rate";
		
		// Tolerance on how accurate the result should be
		int numRepeats = 200;
		double tolerance = 0.04d;

		// Collect all tend times for drawing histogram
		List<Double> endTimes = new ArrayList<>();
		
		// Run the simulation multiple times and measure the end time
		Simulation simulation = null;
		int species0Number = 0;
		for (int i=0; i<numRepeats; i++) {
			simulation = new SimulationFactory().createSimulation(getClass().getResourceAsStream(modelFileName), null);
			species0Number = simulation.getCurrentState().getAllSpecies(species0Name);
			simulation.runForSeconds(30 * 60 * 60);
			endTimes.add(simulation.getCurrentHours());
		}
		
		// Print out all the end times for drawing the histogram
		Logging.setLogLevel(LogType.DETAIL);
		for (Double endTime: endTimes) {
			Logging.getLogger(LogType.DETAIL).log(String.format("%s", endTime));
		}
		
		// Calculate the measured end time
		double totalEndTimes = 0;
		for (Double endTime: endTimes) {
			totalEndTimes += endTime;
		}
		double measuredEndTime = totalEndTimes / numRepeats;

		// Calculate the predicted end time
		// This is the time until the exponential distribution goes to 0, with a half life of ln(2)/lambda 
		//  where: lambda is the total rate of all reactions
		//         number of half lives for exponential distribution to run out is ceil(log2(initial number of species 0) + 1)
		double totalRate = simulation.getModel().getParameterValue(species1Rate) + simulation.getModel().getParameterValue(species2Rate);
		double halfLife = Math.log(2) / totalRate;
		double predictedEndTime = halfLife * Math.ceil(Math.log(species0Number) / Math.log(2) + 1);
		
		// Check that the measured end time matches the predicted end time
		assertThat("Measured end time doesn't match expected end time", 
				measuredEndTime, closeTo(predictedEndTime, tolerance));
	}
	
	/**
	 * Test that the spleen file prints out tag information.
	 */
	@Test
	public void runSpleenTagsTest() throws InvalidModelException, IOException, URISyntaxException {
		// Run simulation
		Driver.main(new String[]{ 
				"--hours", "0",
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("spleenV2.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});
		// Check we got some tags.
		String[] lines = readLogFile(LogType.TAGS).split("\n");
		assertThat("Didn't print out tags file", lines.length, greaterThan(100));
	}
}
