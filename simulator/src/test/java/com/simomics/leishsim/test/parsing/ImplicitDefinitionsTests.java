package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.test.OutputTest;

/**
 * Tests of parsing implicit values from MDSL files.
 */
public class ImplicitDefinitionsTests extends OutputTest {
	
	/**
	 * Make sure we don't leave the log level set to something else after running a test. 
	 */
	@Before
	public void resetLogLevel() {
		Logging.resetLogging(new File("test_logs"), LogType.WARNING);
		try {
			new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/implicit_definitions.mdsl"), null);
		} catch (InvalidModelException e) {
			// We know the model is invalid - these tests are about the error messages
		}
	}
	
	/**
	 * Check that we get a warning when defining a species implicitly.
	 */
	@Test
	public void testImplicitSpeciesWarning() throws InvalidModelException, IOException, URISyntaxException {
		assertThat("Didn't find implicit species message", 
				readLogFile(LogType.WARNING), containsString("WARNING: 2 species used in reactions but not listed explicitly: 'species_imp_d contained simulation', 'species_imp_c contained simulation'"));
	}
	
	/**
	 * Check that we get an error when using an undefined parameter.
	 */
	@Test
	public void testImplicitParameterError() throws InvalidModelException, IOException, URISyntaxException {
		assertThat("Didn't find implicit parameter error message", 
				readLogFile(LogType.ERROR), containsString("ERROR: Parameter rate_imp_k used but not listed explicitly"));
	}
	
	/**
	 * Check that we get a warning when defining a species and not using it.
	 */
	@Test
	public void testExplicitSpeciesWarning() throws InvalidModelException, IOException, URISyntaxException {
		assertThat("Didn't find explicit species message", 
				readLogFile(LogType.WARNING), containsString("WARNING: Species 'species_exp_d contained simulation' listed explicitly but not used in reactions"));
	}
	
	/**
	 * Check that we get a warning when defining a parameter and not using it.
	 */
	@Test
	public void testExplicitParameterWarning() throws InvalidModelException, IOException, URISyntaxException {
		assertThat("Didn't find explicit rate message", 
				readLogFile(LogType.WARNING), containsString("WARNING: Parameter rate_exp_rate listed explicitly but not used"));
	}
}
