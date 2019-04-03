package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.simomics.leishsim.Driver;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.test.OutputTest;

/**
 * Tests of the output file of MDSL code generated to fix warning messages.
 */
public class WarningFixTests extends OutputTest {

	@BeforeClass
	public static void runSimulation() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--seconds", "1",
				"--model-file", getModelFilePath("errors/fixable_warnings.mdsl"),
				"--print", "all",
				"--log-level", LogType.WARNING_FIX.name(),
		});
	}

	/**
	 * Check that the warning fix file contains definitions of undefined species
	 */
	@Test
	public void speciesDefinitionsTest() throws IOException, URISyntaxException {
		assertThat("Didn't find species definition in warning fix file", 
				readLogFile(LogType.WARNING_FIX), containsString("species undefined_species_1 contained m = 0 units"));
		assertThat("Didn't find species definition in warning fix file", 
				readLogFile(LogType.WARNING_FIX), containsString("species undefined_species_2 on m = 0 units"));
		assertThat("Didn't find species definition in warning fix file", 
				readLogFile(LogType.WARNING_FIX), containsString("species undefined_species_3 around m = 0 units"));
	}
	
	/**
	 * Check that the warning fix file contains definitions of undefined parameters
	 */
	@Test
	public void parameterDefinitionsTest() throws IOException, URISyntaxException {
		assertThat("Didn't find parameter definition in warning fix file", 
				readLogFile(LogType.WARNING_FIX), containsString("parameter undefined_parameter_1 = 0 units"));
		assertThat("Didn't find parameter definition in warning fix file", 
				readLogFile(LogType.WARNING_FIX), containsString("parameter undefined_parameter_2 = 0 units"));
	}

}
