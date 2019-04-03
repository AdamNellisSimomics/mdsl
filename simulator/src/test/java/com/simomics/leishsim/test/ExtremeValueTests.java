package com.simomics.leishsim.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

import com.simomics.leishsim.Driver;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;

/**
 * Tests involving running the simulator with extreme values, to make sure it doesn't break or slow down.
 */
public class ExtremeValueTests extends OutputTest {

	/**
	 * Running a small network for a large number of (simulation) hours. 
	 */
	@Test
	public void longTimeTest() throws IOException, URISyntaxException {
		// Run the simulation
		Integer numHours = 200_000;
		Integer hoursBeforePrint = 1000;
		Driver.main(new String[]{ 
				"--hours", numHours.toString(), 
				"--model-file", getModelFilePath("constant_speed_network.mdsl"),
				"--hours-before-print", hoursBeforePrint.toString(), 
				"--print", "A", "--print", "B",
		});
		
		// Check that we got enough output (i.e. the simulation actually ran for the requested time)
		int expectedNumOutputLines = numHours / hoursBeforePrint + 2; // +1 for headings, +1 for initial conditions (hour 0)
		int actualNumOutputLines = readLogFile(LogType.PRINTED_SPECIES).split("\n").length;
		assertThat("Didn't find the correct number of newlines in output", actualNumOutputLines, is(expectedNumOutputLines));
	}
	
	/**
	 * Speed test using the redpulp file.
	 */
	@Test(timeout = 100 * 1000)
	public void runRedpulpSpeedTest() throws InvalidModelException, IOException, URISyntaxException {
		// Run simulation
		Driver.main(new String[]{
//				"--hours", "200",
//				"--hours-before-print", "1",
//				"--model-file", getModelFilePath("redpulp_4.mdsl"),
				"--hours", "3",
				"--seconds-before-print", "600",
				"--model-file", getModelFilePath("redpulp_50.mdsl"),
				"--log-level", LogType.PRINTED_PROPENSITIES.name(),
				"--print", "all",
		});
		
		// Check we got the last progress line
		String[] lines = readLogFile(LogType.PROGRESS).split("\n");
		String lastProgressLine = lines[lines.length - 2];
		assertThat("Simulation didn't run for long enough", lastProgressLine, containsString("Run 10800 of 10800 seconds"));
	}
}
