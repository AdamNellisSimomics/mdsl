package com.simomics.leishsim.test.parsing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Iterator;

import org.junit.Test;

import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.simulation.Simulation;

/**
 * Tests of parsing reactions from MDSL files.
 */
public class ReactionParsingTests {
	
	/**
	 * Check that reversible reactions are duplicated.
	 */
	@Test
	public void testDuplicatingReactions() throws Exception {
	    Simulation simulation = new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/reversible_reaction.mdsl"), null);
	    assertThat("Reversible reaction not duplicated", simulation.getModel().getNumReactions(), is(2));
	}
	
	/**
	 * Check that one-way reactions are not duplicated.
	 */
	@Test
	public void testOneWayReactions() throws Exception {
	    Simulation simulation = new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/one_way_reaction.mdsl"), null);
	    assertThat("Reversible reaction not duplicated", simulation.getModel().getNumReactions(), is(1));
	}
	
	/**
	 * Check that delay reactions can be parsed.
	 */
	@Test
	public void delayReactions() throws Exception {
	    Simulation simulation = new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/delay_reactions.mdsl"), null);
	    assertThat("Delay reaction not parsed", simulation.getModel().getNumReactions(), is(3));
	    
	    Iterator<Reaction> reactions = simulation.getModel().getReactions().iterator();
	    Reaction reaction1 = reactions.next();
	    assertThat("Delay reaction not parsed successfully", reaction1.toString(), is("1 a contained simulation -> 1 b contained simulation (forward rate modifier: (1)) (delay: 1000.0)"));
	    Reaction reaction2 = reactions.next();
	    assertThat("Delay reaction not parsed successfully", reaction2.toString(), is("1 c contained simulation -> 1 d contained simulation (forward rate modifier: (10)) (delay: 20.0)"));
	    Reaction reaction3 = reactions.next();
	    assertThat("Delay reaction not parsed successfully", reaction3.toString(), is("1 d contained simulation -> 1 c contained simulation (forward rate modifier: (30)) (delay: 40.0)"));
	}
}
