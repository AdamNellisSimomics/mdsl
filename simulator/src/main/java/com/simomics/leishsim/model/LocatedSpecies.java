package com.simomics.leishsim.model;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Model for a species in a location.
 * e.g. "socs1 contained redpulp_macrophage"
 */
public class LocatedSpecies implements Comparable<LocatedSpecies> {

	/** The name of the reactant */
	private final String speciesName;
	
	/** The location of the reactant relative to the membrane */
	private final Location location;
	
	/** The membrane type */
	private final String membraneName;
	
	/**
	 * Create a description of a species in a location.
	 */
	public LocatedSpecies(String speciesName, Location location, String membraneName) {
		this.speciesName = speciesName;
		this.location = location;
		this.membraneName = membraneName;
	}

	public String getSpeciesName() {
		return speciesName;
	}

	public Location getLocation() {
		return location;
	}

	public String getMembraneName() {
		return membraneName;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder()
		  .append(getSpeciesName())
		  .append(' ')
		  .append(getLocation())
		  .append(' ')
		  .append(getMembraneName());
		return result.toString();
	}
	
	public int hashCode() {
		return new HashCodeBuilder()
				.append(speciesName)
				.append(location)
				.append(membraneName)
				.toHashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof LocatedSpecies) {
			LocatedSpecies other = (LocatedSpecies) obj;
			return speciesName.equals(other.speciesName) &&
					location == other.location &&
					membraneName.equals(other.membraneName);
		}
		else {
			return false;
		}
	}

	@Override
	public int compareTo(LocatedSpecies o) {
		int c = membraneName.compareTo(o.membraneName);
		if (c != 0) {
			return c;
		}
		c = location.compareTo(o.location);
		if (c != 0) {
			return c;
		}
		c = speciesName.compareTo(o.speciesName);
		return c;
	}
}
