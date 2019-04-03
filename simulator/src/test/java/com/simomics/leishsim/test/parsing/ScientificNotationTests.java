package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.model.MembraneModel;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.simulation.Simulation;

/**
 * Tests that we can parse scientific notation in numbers.
 */
public class ScientificNotationTests {

	private static MembraneModel model;

	@BeforeClass
	public static void parseModel() throws InvalidModelException {
		Logging.resetLogging(new File("test_logs"), LogType.FULL);
		Simulation simulation = new SimulationFactory().createSimulation(ScientificNotationTests.class.getResourceAsStream("/model_files/scientific_notation.mdsl"), null);
		model = simulation.getModel();
	}
	
	/**
	 * Test that we can parse scientific notation in parameter definitions
	 */
	@Test
	public void parameterTest() throws IOException, URISyntaxException {
		assertThat("Parameter value didn't match", model.getParameterValue("p"), is(0.00123d));
		assertThat("Parameter value didn't match", model.getParameterValue("q"), is(4567890000000d));
		assertThat("Parameter value didn't match", model.getParameterValue("r"), is(0.00000000000012345d));
	}
}
