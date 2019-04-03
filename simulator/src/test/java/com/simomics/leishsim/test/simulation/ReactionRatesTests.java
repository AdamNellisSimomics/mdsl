package com.simomics.leishsim.test.simulation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.junit.Test;

import com.simomics.leishsim.Driver;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.test.OutputTest;

/**
 * Tests of calculating reaction rates.
 */
public class ReactionRatesTests extends OutputTest {

	/**
	 * Test that reaction rates are calculated correctly for different stoichiometries of reactants.
	 * @throws URISyntaxException | IOException 
	 */
	@Test
	public void stoichiometryRates() throws IOException, URISyntaxException {
		// Run simulation
		Driver.main(new String[]{ 
				"--seconds", "1",
				"--model-file", getModelFilePath("stoichiometries.mdsl"),
				"--print", "all",
				"--log-level", LogType.FULL.name(),
		});
		// Read table of initial reaction rates from log file
		final int numReactions = 5;
		String[] tableLines = new String[numReactions];
		Double[] observedRates = new Double[numReactions];
		int rowNum = 0;
		for (String line: readLogFile(LogType.FULL).split("\n")) {
			line = line.trim();
			if (line.endsWith("}")) {
				tableLines[rowNum] = line;
				observedRates[rowNum] = new Double(line.substring(0, line.indexOf(" ")));
				rowNum += 1;
			}
			if (line.isEmpty()) {
				break;
			}
		}
		// Check that initial rates match what we get from doing the combinatorics equations the hard way
		// nCk = (n!/(n-k)!) / k!
		double a = 11, b = 13, c = 17, d = 19, e = 23;
		Double[] expectedRates = new Double[] {
			/* 2a */         a * (a - 1) / 2,
			/* 3b */         b * (b - 1) * (b - 2) / (2 * 3),
			/* 5a and 7b */ (a * (a - 1) * (a - 2) * (a - 3) * (a - 4) / (5 * 4 * 3 * 2)) 
			              * (b * (b - 1) * (b - 2) * (b - 3) * (b - 4) * (b - 5) * (b - 6) / (7 * 6 * 5 * 4 * 3 * 2)),
			/* 1c */         c,
			/* 1d and 1e */  d * e
		};
		assertThat("Rates were not calculated correctly", expectedRates, is(observedRates));
	}
}
