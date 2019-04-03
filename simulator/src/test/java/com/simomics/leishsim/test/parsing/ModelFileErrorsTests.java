package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

import com.simomics.leishsim.Driver;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.test.OutputTest;

/**
 * Tests of error messages from invalid model files.
 */
public class ModelFileErrorsTests extends OutputTest {

	/**
	 * Check that we get warnings about parameters that aren't defined.
	 */
	@Test
	public void parameterValueUndefinedTest() throws InvalidModelException, IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--hours", "2", 
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("undefined-parameters.mdsl"),
				"--print", "IFNg",
				"--print", "Jak1",
				"--log-level", LogType.DETAIL.name(),
				"--log-dir", "test_logs",
		});
		
		assertThat("Didn't find parameter without value error message", 
				readLogFile(LogType.ERROR), is(allOf(
						containsString("Line: 591 Species name used in equation without a location: IL-12"),
						containsString("Line: 694 Species name used in equation without a location: TGFb"),
						containsString("ERROR: Failed to parse model file because of a semantic error:"))));
	}
}
