package com.simomics.leishsim.test.simulation;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Test;

import com.simomics.leishsim.Driver;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.test.OutputTest;

/**
 * Tests using different types of reaction
 */
public class ReactionTests extends OutputTest {

	/**
	 * Test that we can handle reactions that diffuse species across membranes, where both membrane names are used.
	 * 
	 * NB: This is currently not supported, so this test will fail.
	 */
	@Test
	public void FAIL_diffusionAcrossMembranesBothNamesTest() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--hours", "10",
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("diffusion_across_membranes_both_names.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});

		assertThat("Didn't find invalid reaction message: ",
				readLogFile(LogType.ERROR), containsString("ERROR: Failed to parse model file because of a semantic error: \nLine 10: Invalid reaction: Multiple 'contained' or 'around' locations for different membranes."));
		// When the feature is implemented, the above assertion will fail and the simulation will run.
		// Then need to check the concentrations of the species at the end of the run.
	}
	
	/**
	 * Test that we can handle reactions that diffuse species across membranes, where only the inner membrane name is used.
	 */
	@Test
	public void diffusionAcrossMembranesOneNameTest() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--hours", "10",
				"--seconds-before-print", "100",
				"--model-file", getModelFilePath("diffusion_across_membranes_one_name.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});
		assertThat("Error running test", readLogFile(LogType.ERROR), isEmptyString());
		
		Map<String, Integer> initialValues = getLineOfSpeciesFile(1);
		Map<String, Integer> finalValues = getLineOfSpeciesFile(-1);
		
		assertThat(initialValues.get("x"), is(300));
		assertThat(initialValues.get("y"), is(300));
		assertThat(initialValues.get("x_diffused"), is(0));
		assertThat(initialValues.get("y_diffused"), is(0));
		
		assertThat(finalValues.get("x"), is(0));
		assertThat(finalValues.get("y"), is(0));
		assertThat(finalValues.get("x_diffused"), is(300));
		assertThat(finalValues.get("y_diffused"), is(300));
	}
	
	/**
	 * Test that we can correctly simulate reactions with delays.
	 */
	@Test
	public void delayReactionsTest() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--hours", "10",
				"--seconds-before-print", "100",
				"--model-file", getModelFilePath("delay_reactions.mdsl"),
				"--print", "all",
				"--log-level", LogType.FULL.name(),
		});
		assertThat("Error running test", readLogFile(LogType.ERROR), isEmptyString());
		
		assertThat("Didn't find fired reaction message: ",
				readLogFile(LogType.FULL), containsString(" Fired reaction: "));
		assertThat("Didn't find queue use message: ",
				readLogFile(LogType.FULL), containsString(" Executed queue event: "));
	}
	
	/**
	 * Test that delays on reactions don't affect independent reactions.
	 */
	@Test
	public void delayReactionsIndependenceTest() throws IOException, URISyntaxException {
		// Run with delay 20
		Driver.main(new String[]{ 
				"--hours", "1000",
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("delay_reactions_independence_20.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});
		assertThat("Error running test", readLogFile(LogType.ERROR), isEmptyString());
		
		// Work out the half life of species a
		Integer halfLifeDelay20 = measureHalfLife("a");
		
		// Run with delay 80
		Driver.main(new String[]{ 
				"--hours", "1000",
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("delay_reactions_independence_80.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});
		assertThat("Error running test", readLogFile(LogType.ERROR), isEmptyString());
		
		// Work out half life of species a
		Integer halfLifeDelay80 = measureHalfLife("a");
		
		// Check that half lives are the same
		double tolerance = 2.0;
		assertThat("Half lives with different delays were not the same", 
				new Double(halfLifeDelay20), closeTo(halfLifeDelay80, tolerance));
	}
	
	/**
	 * Test that delays on reactions don't affect independent reactions. Ed's version
	 */
	@Test
	public void delayReactionsIndependenceEdTest() throws IOException, URISyntaxException {
		// Run with delay 20
		Driver.main(new String[]{ 
				"--hours", "1000",
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("delay_reactions_independence_20.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});
		assertThat("Error running test", readLogFile(LogType.ERROR), isEmptyString());
		
		// Work out the half life of species a
		Integer halfLifeDelay20 = measureHalfLife("a");
		
		// Run with delay 80
		Driver.main(new String[]{ 
				"--hours", "1000",
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("delay_reactions_independence_ed.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});
		assertThat("Error running test", readLogFile(LogType.ERROR), isEmptyString());
		
		// Work out half life of species a
		Integer halfLifeDelayEd = measureHalfLife("a");

		// Check that half lives are the same
		double tolerance = 2.0;
		assertThat("Half lives with different delays were not the same", 
				new Double(halfLifeDelay20), closeTo(halfLifeDelayEd, tolerance));
	}
	
	/**
	 * Test that the delays on reactions are measured in hours.
	 */
	@Test
	public void delayUnitsTest() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--hours", "1000",
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("delay_reactions_units.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});
		assertThat("Error running test", readLogFile(LogType.ERROR), isEmptyString());
		
		assertThat("Delay wasn't the correct time",
				readLogFile(LogType.PROGRESS), containsString("Simulation stopped early at 36000.0 seconds")); // 10 hours
	}
	
	/**
	 * Test that reaction propensities are recalculated after the result of a delay reaction happens.
	 */
	@Test
	public void delayRecalculationTest() throws IOException, URISyntaxException {
		Driver.main(new String[]{ 
				"--hours", "20",
				"--hours-before-print", "1",
				"--model-file", getModelFilePath("delay_recalculation.mdsl"),
				"--print", "all",
				"--log-level", LogType.PRINTED_SPECIES.name(),
		});
		assertThat("Error running test", readLogFile(LogType.ERROR), isEmptyString());
		
		assertThat("Reaction to produce species c never fired", 
				getLineOfSpeciesFile(-1).get("c"), is(1));
	}
}
