package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.simulation.Simulation;

/**
 * Tests that we can correctly evaluate equations in parameter values.
 *
 * These tests have a particular form. 
 * In the MDSL file are parameter definitions of the form:
 * parameter {name} = {equation} units
 * 
 * These tests evaluate all the equations, and check that each parameter value is correct.
 */
public class ParameterEvaluatingTests {

	/** The values of each parameter, indexed by their key species */
	private final Map<String, Double> parameterValues = new HashMap<>();
	
	/**
	 * Parse the model file before each test, and evaluate each of the equations.
	 */
	@Before
	public void parseModel() throws InvalidModelException {
		Logging.setLogLevel(LogType.DEBUG);
		Simulation simulation = new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/equations_in_parameters.mdsl"), null);
		for (String parameterName: simulation.getModel().getParameterNames()) {
			double parameterValue = simulation.getModel().getParameterValue(parameterName);
			parameterValues.put(parameterName, parameterValue);
		}
	}
	
	/**
	 * Test that we can correctly evaluate a parameter containing a single constant.
	 */
	@Test
	public void constantTest() {
		assertThat("Constant parameter value didn't match", parameterValues.get("a"), is(5d));
	}
	
	/**
	 * Test that we can correctly evaluate a parameter containing a parameter value.
	 */
	@Test
	public void parameterTest() {
		assertThat("Parameter parameter value didn't match", parameterValues.get("b"), is(5d));
	}
	
	/**
	 * Test that we can correctly evaluate a parameter containing an equation.
	 */
	@Test
	public void equationTest() {
		assertThat("Equation parameter value didn't match", parameterValues.get("c"), is(19d));
	}
}
