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
import com.simomics.leishsim.test.OutputTest;

/**
 * Tests that we can preprocess MDSL files correctly.
 */
public class PreprocessingTests extends OutputTest {

	@Test
	public void includeTest() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--hours", "100", 
				"--model-file", getModelFilePath("includes.mdsl"),
				"--print", "all",
		});
		
		assertThat("Reactions didn't happen as expected.",
				getLineOfSpeciesFile(-1).get("c"), is(10));
	}
	
	@Test
	public void missingFileTest() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--hours", "100", 
				"--model-file", getModelFilePath("errors/include_missing.mdsl"),
				"--print", "all",
		});
		assertThat("Didn't find missing include file message", 
				readLogFile(LogType.ERROR), containsString("ERROR: Could not read include file."));
	}
}
