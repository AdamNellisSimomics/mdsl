package com.simomics.leishsim.model;

/**
 * Model for a number of a species in a location.
 * e.g. "3 socs1 contained redpulp_macrophage"
 */
public class StoichiometrySpecies extends LocatedSpecies {

	/** The number of copies of the species */
	private final int stochiometry;
	
	public StoichiometrySpecies(int stochiometry, String speciesName, Location location, String membraneName) {
		super(speciesName, location, membraneName);
		this.stochiometry = stochiometry;
	}

	public int getStochiometry() {
		return stochiometry;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder()
		  .append(stochiometry)
		  .append(' ')
		  .append(super.toString());
		return result.toString();
	}
}
