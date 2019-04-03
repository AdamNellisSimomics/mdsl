package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.model.equations.Equation.InvalidEquationEvaluationException;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.simulation.Simulation;

/**
 * Tests that we can correctly evaluate equations defined in models.
 *
 * These tests have a particular form. 
 * In the MDSL file are reactions of the form:
 * {species} contained simulation modifier {equation} => {something}
 * or {species} contained simulation modifier {equation1} <=> {something} {equation2}
 * 
 * These tests evaluate all the equations against the initial conditions, and check that each value is correct.
 */
public class EquationEvaluatingTests {

	/** The values of each equation, indexed by their key species */
	private final Map<String, Double> equationValues = new HashMap<>();

	/**
	 * Parse the model file before each test, and evaluate each of the equations on the initial conditions.
	 */
	@Before
	public void parseModel() throws InvalidModelException, InvalidEquationEvaluationException {
		Logging.setLogLevel(LogType.DEBUG);
		Simulation simulation = new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/equations_in_modifiers.mdsl"), null);
		for (Reaction reaction: simulation.getModel().getReactions()) {
			String speciesName = reaction.getConsumedSpecies().iterator().next().getSpeciesName();
			double equationValue = reaction.getRateEquation().evaluate(simulation.getCurrentState());
			equationValues.put(speciesName, equationValue);
		}
	}
	
	/**
	 * Test that we can correctly evaluate an equation containing a single constant.
	 */
	@Test
	public void constantEquationTest() {
		assertThat("Constant equation value didn't match", equationValues.get("k"), is(53.7d));
	}
	
	/**
	 * Test that we can correctly evaluate an equation containing constants.
	 */
	@Test
	public void constantsTest() {
		assertThat("Constants equation value didn't match", equationValues.get("l"), is(30d));
	}
}
