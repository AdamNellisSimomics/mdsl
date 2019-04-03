package com.simomics.leishsim.test.diagrams;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Test;

import com.simomics.leishsim.DiagramGenerator;

/**
 * Tests of drawing diagrams programmatically from MDSL files.
 */
public class DiagramTests extends DiagramTestsBase {
	
	/**
	 * Test that we can draw a diagram from a simple MDSL file.
	 */
	@Test
	public void simpleDiagramTest() throws IOException, URISyntaxException {
		DiagramGenerator.main(new String[]{
				"--output-dir", outputDir,
				"create", 
				//"--model-file", getModelFilePath("differentiation_test.mdsl"),
				"--model-file", getModelFilePath("IFNg_test_pathway_1.mdsl"),
		});
		
		assertThat("Output file was not created", 
				readFileFromPath(Paths.get(outputDir, "diagram.xml"), "\n", false), containsString("<?xml"));
	}
	
	/**
	 * Test that we can output a diagram in JSON format.
	 */
	@Test
	public void jsonDiagramTest() throws IOException, URISyntaxException {
		DiagramGenerator.main(new String[]{ 
				"--output-dir", outputDir,
				"--output-filename", "cells_only.json",
				"create", 
				//"--model-file", getModelFilePath("differentiation_test.mdsl"),
				//"--model-file", getModelFilePath("IFNg_test_pathway_1.mdsl"),
				//"--model-file", getModelFilePath("cells.mdsl"),
				"--model-file", getModelFilePath("cells_only.mdsl"),
		});
		
		assertThat("Output file was not created", 
				readFileFromPath(Paths.get(outputDir, "cells_only.json"), "\n", false), containsString("{\n"));
	}
	
	/**
	 * Test that we can output a diagram in graphml format.
	 */
	@Test
	public void graphmlDiagramTest() throws IOException, URISyntaxException {
		DiagramGenerator.main(new String[]{ 
				"--output-dir", outputDir,
				"--output-filename", "cells_only.graphml",
				"create", 
				//"--model-file", getModelFilePath("differentiation_test.mdsl"),
				//"--model-file", getModelFilePath("IFNg_test_pathway_1.mdsl"),
				//"--model-file", getModelFilePath("cells.mdsl"),
				"--model-file", getModelFilePath("cells_only.mdsl"),
		});
		
		assertThat("Output file was not created", 
				readFileFromPath(Paths.get(outputDir, "cells_only.graphml"), "\n", false), containsString("<graphml xmlns="));
	}
	
	/**
	 * Test that we can output a diagram restricted by tag.
	 */
	@Test
	public void tagDiagramTest() throws IOException, URISyntaxException {
		String tag = "NFkb";
		//String tag = "receptor";
		String graphFileName = tag + ".graphml";
		DiagramGenerator.main(new String[]{ 
				"--output-dir", outputDir,
				"--output-filename", graphFileName,
				//"--output-filename", "all.graphml",
				"create", 
				"--model-file", getModelFilePath("spleenV2.mdsl"),
				//"--model-file", getModelFilePath("spleenV2_fewer_tags.mdsl"),
				"--tag", tag,
				//"--tag", "intracellular",
				//"--tag", "cell",
		});
		
		assertThat("Output graph file does not contain a node for the requested tag", 
				readFileFromPath(Paths.get(outputDir, graphFileName), "\n", false), 
				containsString("<y:NodeLabel>" + tag + "</y:NodeLabel>"));
	}
}
