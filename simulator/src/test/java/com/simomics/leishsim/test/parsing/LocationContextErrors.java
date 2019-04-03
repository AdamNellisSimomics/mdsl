package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.test.OutputTest;

/**
 * Tests of error messages when misusing location contexts.
 */
public class LocationContextErrors extends OutputTest {

	/**
	 * Test that we get an error message when trying to next location contexts.
	 */
	@Test
	public void nestedLocationContexts() throws IOException, URISyntaxException {
		Logging.resetLogging(new File("logs"), LogType.DETAIL);
		try {
			new SimulationFactory().createSimulation(LocationContextTests.class.getResourceAsStream("/model_files/errors/location_contexts_nested.mdsl"), null);
		} catch (InvalidModelException e) {
			assertThat("Didn't find error message", 
					e.getMessage(), is(allOf(
							containsString("failed to parse at line 8 due to extraneous input 'contained'"),
							containsString("Failed to parse model file because of a grammar error"))));
		}
	}
	
	/**
	 * Test that we get an error message when missing out a location context
	 * 
	 */
	@Test
	public void missingLocationContexts() throws IOException, URISyntaxException {
		Logging.resetLogging(new File("logs"), LogType.DETAIL);
		try {
			new SimulationFactory().createSimulation(LocationContextTests.class.getResourceAsStream("/model_files/errors/location_context_missing.mdsl"), null);
		} catch (InvalidModelException e) {
			assertThat("Didn't find error message", 
					e.getMessage(), is(allOf(
							containsString("Line 11: Species definition is missing location: species b = 3 units"),
							containsString("Failed to parse model file because of a semantic error"))));
		}
	}
}
