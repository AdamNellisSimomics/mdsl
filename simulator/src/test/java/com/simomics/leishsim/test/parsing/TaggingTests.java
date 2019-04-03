package com.simomics.leishsim.test.parsing;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.simomics.leishsim.model.MembraneModel;
import com.simomics.leishsim.parsing.SimulationFactory;
import com.simomics.leishsim.parsing.SimulationFactory.InvalidModelException;
import com.simomics.leishsim.simulation.Simulation;

/**
 * Tests of parsing tags from MDSL files.
 */
public class TaggingTests {

	/** All tests in this class use the same parsed file */
	private MembraneModel model;

	/** Parse the test file */
	@Before
	public void parseFile() throws InvalidModelException {
		Simulation simulation = new SimulationFactory().createSimulation(getClass().getResourceAsStream("/model_files/tags.mdsl"), null);
		model = simulation.getModel();
	}
	
	/** Check that we can parse one tag */
	@Test
	public void testSingleTag() {
	    assertThat("Failed to parse tag", model.getTagsForSpecies("a"), containsInAnyOrder("tag1"));
	}
	
	/** Check that we can parse two tags */
	@Test
	public void testTwoTags() {
	    assertThat("Failed to parse two tags", model.getTagsForSpecies("b"), containsInAnyOrder("tag2", "tag3"));
	}
	
	/** Check that we can parse a quoted tag containing a space */
	@Test
	public void testQuotedTag() {
	    assertThat("Failed to parse quoted tag", model.getTagsForSpecies("d"), containsInAnyOrder("tag1 tag2", "tag3"));
	}
	
	/** Check that we can parse tags containing special characters */
	@Test
	public void testSpecialCharactersInTags() {
	    assertThat("Failed to parse species characters in tags", model.getTagsForSpecies("e"), containsInAnyOrder("tag_1", "tag2-3"));
	}
	
	/** Check that we can parse species that don't have tags */
	@Test
	public void testNoTags() {
	    assertThat("Failed to parse species without tags", model.getTagsForSpecies("f"), empty());
	}
	
	/** Check that we can list species for tags */
	@Test
	public void testSpeciesForTag() {
	    assertThat("Failed to compute species for a tag", model.getSpeciesForTag("tag1"), containsInAnyOrder("a", "c"));
	}
	
	/** Check that we can list species for a tag containing spaces */
	@Test
	public void testSpeciesForTagWithSpaces() {
	    assertThat("Failed to compute species for a tag containing spaces", model.getSpeciesForTag("tag1 tag2"), containsInAnyOrder("d"));
	}
	
	/** Check that we can handle looking up an unknown tag */
	@Test
	public void testUnknownTag() {
	    assertThat("Failed to look up an unknown tag", model.getSpeciesForTag("unknown tag"), empty());
	}
}
