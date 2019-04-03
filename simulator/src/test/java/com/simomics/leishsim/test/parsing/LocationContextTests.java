package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;

import com.simomics.leishsim.model.LocatedSpecies;
import com.simomics.leishsim.model.Location;
import com.simomics.leishsim.model.MembraneModel;
import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.model.StoichiometrySpecies;
import com.simomics.leishsim.model.equations.AddExpression;
import com.simomics.leishsim.model.equations.LocatedSpeciesExpression;
import com.simomics.leishsim.model.equations.MultiplyExpression;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.simulation.Membrane;
import com.simomics.leishsim.simulation.Simulation;

public class LocationContextTests {

	private static Membrane membrane_a;
	private static Membrane membrane_b;
	private static Reaction reaction_1;
	private static Reaction reaction_2;
	private static MembraneModel model;
	
	@BeforeClass
	public static void parseFile() {
		try {
			Simulation simulation = new SimulationFactory().createSimulation(LocationContextTests.class.getResourceAsStream("/model_files/location_context.mdsl"), null);
			membrane_a = simulation.getCurrentState().getMatchingMembranes("membrane_a", null).iterator().next();
			membrane_b = simulation.getCurrentState().getMatchingMembranes("membrane_b", null).iterator().next();
			model = simulation.getModel();
			Iterator<Reaction> reactions = model.getReactions().iterator();
			reaction_1 = reactions.next();
			reaction_2 = reactions.next();
		} catch (InvalidModelException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Test that we can still parse a species that isn't in a context, even when a context is present.
	 */
	@Test
	public void speciesOutsideContextTest() {
		assertEquals("Didn't find species", 1, membrane_a.numSpecies(new LocatedSpecies("a", Location.contained, "membrane_a")));
	}
	
	/**
	 * Test that we can parse a species definition that uses a location context block.
	 */
	@Test
	public void speciesUsingContextTest() {
		assertEquals("Didn't find species", 3, membrane_a.numSpecies(new LocatedSpecies("b", Location.contained, "membrane_a")));
	}
	
	/**
	 * Test that we can parse a species definition that comes after a location context block.
	 */
	@Test
	public void speciesAfterContextTest() {
		assertEquals("Didn't find species", 8, membrane_a.numSpecies(new LocatedSpecies("f", Location.under, "membrane_a")));
	}
	
	/**
	 * Test that we can parse a fully located species definition that is within a location context block
	 */
	@Test
	public void locatedSpeciesInsideContextTest() {
		assertEquals("Didn't find species", 5, membrane_a.numSpecies(new LocatedSpecies("c", Location.contained, "membrane_a")));
	}
	
	/**
	 * Test that we can parse a species definition for a different membrane inside a context block.
	 */
	@Test
	public void speciesDifferentMembraneTest() {
		assertEquals("Didn't find species", 6, membrane_b.numSpecies(new LocatedSpecies("d", Location.contained, "membrane_b")));
	}
	
	/**
	 * Test that we can parse a reaction definition containing a species in a context block as a reactant.
	 */
	@Test
	public void reactantsTest() {
		assertThat("Didn't find reactants", reaction_1.getConsumedSpecies(), hasItems(
				new StoichiometrySpecies(1, "a", Location.contained, "membrane_a"),
				new StoichiometrySpecies(2, "b", Location.contained, "membrane_a")));
	}
	
	/**
	 * Test that we can parse a reaction definition containing a species in a context block as a product.
	 */
	@Test
	public void productsTest() {
		assertThat("Didn't find products", reaction_1.getProducedSpecies(), hasItems(
				new StoichiometrySpecies(1, "c", Location.contained, "membrane_a")));
	}
	
	/**
	 * Test that we can still define a parameter when in a context block. 
	 */
	@Test
	public void parameterDefinitionTest() {
		assertThat("Parameter definition incorrect", model.getParameterValue("k_2"), is(4d));
	}
	
	/**
	 * Test that we can still use a parameter when in a context block. 
	 */
	@Test
	public void parameterUseTest() {
		assertThat("Delay (parameter use) incorrect", reaction_1.getDelayHours(), is(4d));
	}
	
	/**
	 * Test that we can use species in a modifier equation, using a context block. 
	 */
	@Test
	public void speciesEquationTest() {
		assertThat(reaction_2.getRateModifier(), is(
				new AddExpression(
						new LocatedSpeciesExpression(new LocatedSpecies("a", Location.contained, "membrane_a")), 
						new MultiplyExpression(
								new LocatedSpeciesExpression(new LocatedSpecies("b", Location.contained, "membrane_a")), 
								new LocatedSpeciesExpression(new LocatedSpecies("c", Location.contained, "membrane_a"))))));
	}
	
}
