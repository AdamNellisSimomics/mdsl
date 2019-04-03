package com.simomics.leishsim.test.simulation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

import com.simomics.leishsim.Driver;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.test.OutputTest;

/**
 * Testing that the simulator works properly with models containing multiple membranes.
 */
public class MultiMembranesTests extends OutputTest {

	/**
	 * Test that species are initialised the same when they are specified using "around membrane" vs. "contained simulation".
	 */
	@Test
	public void initialisationContainedVsAroundTest() throws InvalidModelException, IOException, URISyntaxException {
		// Run simulation
		Driver.main(new String[]{ 
				"--seconds", "1",
				"--random-seed", "42", 
				"--seconds-before-print", "10", 
				"--model-file", getModelFilePath("contained_around.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
				"--log-dir", "logs_test",
		});
		// Check initialisation line from log file
		String log = readLogFile(LogType.PRINTED_SPECIES);
		String[] line = log.split("\n")[1].split(",");
		String speciesAInitialisation = line[1].trim();
		String speciesBInitialisation = line[2].trim();
		assertThat("Species not initialised the same", speciesBInitialisation, is(speciesAInitialisation));
	}
	
	/**
	 * Test that reactions have the same rate when they are specified using "around membrane" vs. "contained simulation".
	 */
	@Test
	public void runContainedVsAroundTest() throws InvalidModelException, IOException, URISyntaxException {
		// Run simulation
		Driver.main(new String[]{ 
				"--seconds", "2500",
				"--seconds-before-print", "10", 
				"--model-file", getModelFilePath("contained_around.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
				"--log-dir", "logs_test",
		});
		// Check that the numbers of each species follow the same pattern
		String[] lines = readLogFile(LogType.PRINTED_SPECIES).split("\n");
		long totalSquaredDifference = 0;
		int lastDifference = -1;
		for (int i=1; i<lines.length; i++) {
			String[] cols = lines[i].split(",");
			int numA = Integer.parseInt(cols[1].trim());
			int numB = Integer.parseInt(cols[2].trim());
			lastDifference = Math.abs(numA - numB);
			totalSquaredDifference += Math.pow(lastDifference, 2);
		}
		double averageSquaredDifference = totalSquaredDifference / (lines.length - 1);
		assertThat("Species did not end the same", lastDifference, is(0));
		assertThat("Species deviated from each other by too much", averageSquaredDifference, lessThan(200d));
	}
	
	/**
	 * Test that rates are recalculated correctly for each membrane.
	 * This is a regression test for a bug where mass was being created by rates not being updated.
	 */
	@Test
	public void rateRecalculationTest() throws InvalidModelException, IOException, URISyntaxException {
		// Run simulation
		Driver.main(new String[]{
				"--seconds", "100",
				"--seconds-before-print", "1",
				"--model-file", getModelFilePath("massconbug.mdsl"),
				"--log-level", LogType.PRINTED_SPECIES.name(),
				"--print", "Th1I",
				"--print", "Th1I_exp",
		});
		// Check the mass at each output time
		String[] lines = readLogFile(LogType.PRINTED_SPECIES).split("\n");
		for (int i=1; i<lines.length; i++) {
			String[] entries = lines[i].split(",");
			int th1I = Integer.parseInt(entries[1].trim());
			int th1I_exp = Integer.parseInt(entries[2].trim());
			String time = entries[3].trim();
			assertThat("Mass not conserved at time " + time, th1I + th1I_exp, is(10));
		}
	}
}
