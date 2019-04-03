package com.simomics.leishsim.simulation;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.simomics.leishsim.model.Reaction;

/**
 * Represents one location in which a reaction can fire.
 */
public class LocatedReaction {

	/** Abstract description of the reaction */
	private final Reaction reaction;

	/** The concrete location in which this reaction could happen */
	private final Membrane membrane;
	
	/** The rate at which this reaction fires in the location */
	private final double rate;
	
	public LocatedReaction(Reaction reaction, Membrane membrane, double rate) {
		this.reaction = reaction;
		this.membrane = membrane;
		this.rate = rate;
	}

	public Reaction getReaction() {
		return reaction;
	}

	public Membrane getMembrane() {
		return membrane;
	}

	public double getRate() {
		return rate;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LocatedReaction)) {
			return false;
		}
		LocatedReaction other = (LocatedReaction) obj;
		return reaction.equals(other.reaction) && membrane.equals(other.membrane);
	}
	
	@Override
    public int hashCode() {
		return new HashCodeBuilder()
				.append(reaction.hashCode())
				.append(membrane.hashCode())
				.toHashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder()
		  .append(reaction.toString())
		  .append(" in membrane ")
		  .append(membrane.hashCode()) // TODO: Improve this
		  .append(" with rate ")
		  .append(rate);
		return result.toString();
	}
}
