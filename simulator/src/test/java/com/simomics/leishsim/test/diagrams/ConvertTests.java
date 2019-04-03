package com.simomics.leishsim.test.diagrams;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Test;

import com.simomics.leishsim.DiagramGenerator;

/**
 * Tests of converting graphml files output by yEd.
 */
public class ConvertTests extends DiagramTestsBase {

	/**
	 * Test that we can convert a diagram from yEd's graphml to our graphml.
	 */
	@Test
	public void convertDiagramGraphmlTest() throws IOException, URISyntaxException {
		String outputFileName = "nfkb.graphml";
		DiagramGenerator.main(new String[]{
				"--output-dir", outputDir,
				"--output-filename", outputFileName,
				"convert", 
				//"--input-file", "test/com/simomics/leishsim/test/resources/diagrams/nfkb.graphml",
				"--input-file", getResourcePath("/diagrams/nfkb.graphml"),
		});
		
		assertThat("Output graph file does not contain graphml", 
				readFileFromPath(Paths.get(outputDir, outputFileName), "\n", false), 
				containsString("<graphml xmlns="));
	}
	
	/**
	 * Test that we can convert a diagram from yEd's graphml to D3 JSON.
	 */
	@Test
	public void convertDiagramD3Test() throws IOException, URISyntaxException {
		String fileName = "cell";
		DiagramGenerator.main(new String[]{
				"--output-dir", outputDir,
				"--output-filename", fileName + ".json",
				"convert", 
				"--input-file", getResourcePath("/diagrams/" + fileName + ".graphml"),
		});
		
		assertThat("Output graph file does not contain JSON", 
				readFileFromPath(Paths.get(outputDir, fileName + ".json"), "\n", false), 
				containsString("{\"id\": \"295\", \"group\": 3,"));
	}
}
