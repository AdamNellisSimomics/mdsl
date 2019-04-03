package com.simomics.leishsim.test.simulation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.simomics.leishsim.Driver;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.test.OutputTest;

/**
 * Tests of printing out simulation results.
 */
public class PrintingTests extends OutputTest {

	/**
	 * Test that "print all" works.
	 */
	@Test
	public void printAllTest() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--seconds", "1",
				"--seconds-before-print", "1",
				"--model-file", getModelFilePath("differentiation_test.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});
		
		List<String> species = Arrays.asList(readLogFile(LogType.PRINTED_SPECIES).split("\n")[0].split(",")); // first line of log
		species = species.stream().map(element -> element.trim()).collect(Collectors.toList()); // remove whitespace
		species.remove(0); // timestamp
		species.remove(species.size() - 1); // "seconds"
		assertThat("Didn't find all species names in output", species, hasItems("Th0_exp", "Th1I_exp", "Th2_exp"));
		assertThat("Found too many species names in output", species.size(), is(3));
	}
}
