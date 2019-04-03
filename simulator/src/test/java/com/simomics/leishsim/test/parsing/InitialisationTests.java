package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.containsString;
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
 * Tests of parsing the initialisation of the simulation from MDSL files.
 */
public class InitialisationTests extends OutputTest {
	
	/**
	 * Test that we can read the initialisation section.
	 */
	@Test
	public void initialisationTest() throws InvalidModelException, IOException, URISyntaxException {
		Logging.resetLogging(new File("logs"), LogType.DEBUG);
	    new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/simple_tree.mdsl"), null);
	    String oracleData = readInitialConditionsFile("/results_initialisation_simple_tree.txt");
	    assertThat("Initial conditions didn't match", readLogFile(LogType.DEBUG), containsString(oracleData));
	}
	
	/**
	 * Test that we can read membrane tags in the initialisation section.
	 */
	@Test
	public void initialisationTagsTest() throws InvalidModelException, IOException, URISyntaxException {
		Logging.resetLogging(new File("logs"), LogType.DEBUG);
	    new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/tagged_membranes.mdsl"), null);
	    String oracleData = readInitialConditionsFile("/results_initialisation_tags.txt");
	    assertThat("Initial conditions didn't match", readLogFile(LogType.DEBUG), containsString(oracleData));
	}
	
	/**
	 * Test that we can read equations in species initialisations.
	 */
	@Test
	public void equationsInSpeciesTest() throws InvalidModelException, IOException, URISyntaxException {
		Logging.resetLogging(new File("logs"), LogType.DEBUG);
	    new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/equations_in_species.mdsl"), null);
	    String oracleData = readInitialConditionsFile("/results_equations_in_species.txt");
	    assertThat("Initial conditions didn't match", readLogFile(LogType.DEBUG), containsString(oracleData));
	}
}
