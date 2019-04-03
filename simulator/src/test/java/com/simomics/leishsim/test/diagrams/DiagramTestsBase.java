package com.simomics.leishsim.test.diagrams;

import java.io.File;

import org.junit.Before;

import com.simomics.leishsim.test.OutputTest;

public class DiagramTestsBase extends OutputTest {
	
	protected static String outputDir = "test_diagrams";
	
	/**
	 * Remove any stale files from previous runs.
	 */
	@Before
	public void clearOutputDir() {

		new File(outputDir).mkdir();

		for(File file: new File(outputDir).listFiles()) {
			file.delete();
		}
	}
}
