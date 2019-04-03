package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.test.OutputTest;

/**
 * Tests of parsing equations in the files.
 * 
 * These tests have a particular form. 
 * In the MDSL file are reactions of the form:
 * {species} contained simulation modifier {equation} => {something}
 * or {species} contained simulation modifier {equation1} <=> {something} {equation2}
 * 
 * At logging level DEBUG, the reactions are printed out to the log, including their modifier equations.
 * These tests check the equations printed to  the log, to make sure they are correct.
 */
public class EquationParsingTests extends OutputTest {

	/**
	 * Parse the model file before each test.
	 */
	@Before
	public void parseModel() {
		Logging.resetLogging(new File("test_logs"), LogType.FULL);
		try {
			new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/equations_in_modifiers.mdsl"), null);
		} catch (InvalidModelException e) {
			// We know there are errors in the file - they don't matter for this test
		}
	}
	
	/**
	 * Testing that we can read parameters used as reaction rate modifiers.
	 */
	@Test
	public void parametersInModifiersTest() throws IOException, URISyntaxException {
		assertThat("Parameter equation didn't match", 
				getReactionModifierEquation("a"), is("(z)"));
	}

	/**
	 * Testing that we can read equations used as reaction rate modifiers.
	 */
	@Test
	public void equationsInModifiersTest() throws IOException, URISyntaxException {
		assertThat("Equation didn't match", 
				getReactionModifierEquation("b"), is("((x) + (y))"));
	}

	/**
	 * Testing that we can read equations used as modifiers on both sides of an equation
	 */
	@Test
	public void equationsInReversibleReactionsTest() throws IOException, URISyntaxException {
		assertThat("Forward rate in reversible reaction didn't match", 
				getReactionModifierEquation("c"), is("((((h contained simulation) - (a contained simulation)) / ((x) * (b contained simulation))) + (y))"));
		assertThat("Reverse rate in reversible reaction didn't match", 
				getReactionModifierEquation("d"), is("((((a contained simulation) * (x)) + ((b contained simulation) * (y))) / ((z) - (c contained simulation)))"));
	}
	
	/**
	 * Testing that we can read constants in reaction rate modifiers.
	 */
	@Test
	public void constantsInModifiersTest() throws IOException, URISyntaxException {
		assertThat("Equation containing constant didn't match", 
				getReactionModifierEquation("e"), is("(((2) * (b contained simulation)) + (3.4))"));
	}
	
	/**
	 * Testing that we can parse precedence orders correctly.
	 */
	@Test
	public void precedenceOrderTest() throws IOException, URISyntaxException {
		assertThat("First precedence order equation didn't match", 
				getReactionModifierEquation("f"), is("((((a contained simulation) + ((3) * (b contained simulation))) + ((2) / (z))) - (2.345))"));
		assertThat("Second precedence order equation didn't match", 
				getReactionModifierEquation("g"), is("((((a contained simulation) + (3)) * ((b contained simulation) + (2))) / ((z) - (2.345)))"));
	}
	
	/**
	 * Testing that we can correctly parse unary minus applied to a number.
	 * 
	 * Expect this to fail because it hasn't been implemented yet.
	 */
	@Test
	public void FAIL_unaryMinusNumberTest() throws IOException, URISyntaxException {
		try {
			assertThat("Unary minus number equation didn't match", 
					getReactionModifierEquation("h"), is("((-3) * ((b contained simulation) + (2)))"));
			fail("Unexpected pass of test that is expected to fail");
		} catch (AssertionError e) {
			// When the feature is implemented, the assertion above will pass
		}
	}
	
	/**
	 * Testing that we can correctly parse unary minus applied to an expression.
	 * 
	 * Expect this to fail because it hasn't been implemented yet.
	 */
	@Test
	public void FAIL_unaryMinusExpressionTest() throws IOException, URISyntaxException {
		try {
			assertThat("Unary minus expression equation didn't match", 
					getReactionModifierEquation("i"), is("((3) * (-((b contained simulation) + (2))))"));
			fail("Unexpected pass of test that is expected to fail");
		} catch (AssertionError e) {
			// When the feature is implemented, the assertion above will pass
		}
	}
	
	/**
	 * Testing that we can correctly parse located species in equations.
	 */
	@Test
	public void locatedSpeciesTest() throws IOException, URISyntaxException {
		assertThat("Located species equation didn't match", 
				getReactionModifierEquation("j"), is("((a contained simulation) * (y))"));
	}
	
	/**
	 * Testing that we can correctly parse equations that are a single constant.
	 */
	@Test
	public void constantEquationTest() throws IOException, URISyntaxException {
		assertThat("Constant equation didn't match", 
				getReactionModifierEquation("k"), is("(53.7)"));
	}
	
	/**
	 * Testing that we can correctly parse equations containing just constants.
	 */
	@Test
	public void constantsTest() throws IOException, URISyntaxException {
		assertThat("Constants equation didn't match", 
				getReactionModifierEquation("l"), is("(((12) + (3)) * (2))"));
	}
	
	/**
	 * Pull out the rate modifier equation for a given equation, from the log.
	 * @param log The whole log as a single string.
	 * @param speciesName The name of the first species of the reaction. Assumes that each reaction starts with a different species.
	 * @return Just the equation for the reaction modifier.
	 */
	private String getReactionModifierEquation(String speciesName) throws IOException, URISyntaxException {
		String log = readLogFile(LogType.DEBUG);
		String lineStartPattern = String.format("  1 %s contained simulation -> ", speciesName);
		int start = log.indexOf(lineStartPattern);
		if (start < 0) {
			return null;
		}
		int lineEnd = log.indexOf(") (delay: ", start);
		if (lineEnd < 0) {
			return null;
		}
		String line = log.substring(start + lineStartPattern.length(), lineEnd);
		String equationPattern = "forward rate modifier: ";
		int equationStart = line.indexOf(equationPattern);
		if (equationStart < 0) {
			return null;
		}
		return line.substring(equationStart + equationPattern.length());
	}
}
