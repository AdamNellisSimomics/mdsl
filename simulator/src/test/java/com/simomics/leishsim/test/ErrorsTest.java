package com.simomics.leishsim.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import com.simomics.leishsim.Driver;
import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;


/**
 * Tests of the simulator's error conditions.
 * All these tests are expected to print an error or warning message.
 */
public class ErrorsTest extends OutputTest {

	@Before
	public void setDetailLogLevel() {
		Logging.resetLogging(new File("logs"), LogType.DETAIL);
	}
	
	/**
	 * A model file that doesn't exist.
	 */
	@Test
	public void missingModelFile() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--model-file", "Missing_File.mdsl",
		});
		assertThat("Didn't find missing file message", 
				readLogFile(LogType.ERROR), containsString("ERROR: Could not find file: Missing_File.mdsl"));
	}
	
	/**
	 * A file that doesn't contain valid MDSL.
	 */
	@Test
	public void malformedModelFile() throws IOException, URISyntaxException {
		try {
			new SimulationFactory().createSimulation(this.getClass().getResourceAsStream("/model_files/errors/malformed.mdsl"), null);
			fail("Successfully parsed model file (expected this to fail).");
		} catch (InvalidModelException e) {
			assertThat("Didn't find malformed file message", 
					e.getMessage(), containsString("Failed to parse model file because of a grammar error"));
		}
	}
	
	/**
	 * A file that contains an invalid model.
	 * A species definition without a location.
	 */
	@Test
	public void speciesMissingLocation() throws IOException, URISyntaxException {
		try {
			new SimulationFactory().createSimulation(this.getClass().getResourceAsStream("/model_files/errors/species_missing_location.mdsl"), null);
			fail("Successfully parsed model file (expected this to fail).");
		} catch (InvalidModelException e) {
			assertThat("Didn't find missing location message", 
					e.getMessage(), containsString("Failed to parse model file because of a grammar error: failed to parse at line 8 due to mismatched input"));
			assertThat("Didn't find missing location message", 
					e.getMessage(), containsString("expecting '='"));
		}
	}
	
	/**
	 * A file that contains an invalid model.
	 * Two species with the same name.
	 * 
	 * Expect this to fail, because it hasn't been implemented yet.
	 */
	@Test
	public void duplicateSpecies() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--seconds", "1", 
				"--model-file", getModelFilePath("errors/duplicate_species.mdsl"),
		});
		assertThat("Didn't find duplicate species message", 
				readLogFile(LogType.ERROR), containsString("Duplicate species definition: species a contained simulation"));
	}
	
	/**
	 * A file that contains an invalid model.
	 * Two parameters with the same name.
	 * 
	 * Expect this to fail, because it hasn't been implemented yet.
	 */
	@Test
	public void FAIL_duplicateParameters() throws IOException, URISyntaxException {
		try {
			new SimulationFactory().createSimulation(this.getClass().getResourceAsStream("/model_files/errors/duplicate_parameter.mdsl"), null);
			// When the feature is implemented, it will throw InvalidModelException here
			//fail("Successfully parsed model file (expected this to fail).");
		} catch (InvalidModelException e) {
			assertThat("Didn't find duplicate parameters message", 
					e.getMessage(), containsString("Duplicate parameter definition: param1"));
			fail("Unexpected pass of test that is expected to fail");
		}
	}
	
	/**
	 * A file that contains an invalid model.
	 * Two reactions that are identical.
	 * 
	 * Expect this to fail, because it hasn't been implemented yet.
	 */
	@Test
	public void FAIL_duplicateReactions() {
		try {
			new SimulationFactory().createSimulation(this.getClass().getResourceAsStream("/model_files/errors/duplicate_reaction.mdsl"), null);
			// When the feature is implemented, it will throw InvalidModelException here
			//fail("Successfully parsed model file (expected this to fail).");
		} catch (InvalidModelException e) {
			assertThat("Didn't find duplicate reaction message", 
					e.getMessage(), containsString("Duplicate reactions: a contained simulation modifier param1 => b contained simulation"));
			fail("Unexpected pass of test that is expected to fail");
		}
	}
	
	/**
	 * Asking to print a species that does not exist in the model.
	 * 
	 * Expect this to fail, because it hasn't been implemented yet.
	 */
	@Test
	public void FAIL_printPlaceNotExisting() throws IOException, URISyntaxException {
		Driver.main(new String[]{
				"--hours", "1", 
				"--model-file", getModelFilePath("differentiation_test.mdsl"),
				"--print", "Z", 
		});
		try {
			assertThat("Didn't find invalid place to print message: ",
					readLogFile(LogType.ERROR), containsString("ERROR: Requested printing of a species that does not exist: Z"));
			fail("Unexpected pass of test that is expected to fail");
		} catch (AssertionError e) {
			// When the feature is implemented, the assertion above will pass
		}
	}
}
