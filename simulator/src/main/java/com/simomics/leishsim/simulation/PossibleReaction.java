package com.simomics.leishsim.simulation;

import java.util.Map;

import com.simomics.leishsim.model.Reaction;

/**
 * Represents all the different locations in which a reaction could fire.
 * 
 * NB: Equals (and hashCode) for this class are delegated to those for {@link #reaction}.
 */
public class PossibleReaction {
	
	/** Abstract description of the reaction */
	private final Reaction reaction;

	/** The concrete locations in which this reaction could happen, along with their rates */
	private final Map<Membrane, Double> rates;
	
	/**
	 * Create an empty possible reaction.
	 * Add locations in which this reaction can fire using {@link #addLocation(Membrane, double)}.
	 * @param reaction Abstract description of the reaction.
	 * @param rates Concrete locations in which this reaction could happen, along with their rates.
	 */
	public PossibleReaction(Reaction reaction, Map<Membrane, Double> rates) {
		this.reaction = reaction;
		this.rates = rates;
	}
	
	/**
	 * @return The overall rate of the abstract reaction, taking into account all added locations.
	 */
	public double getRate() {
		double rate = 0.0f;
		for (double possibleRate: rates.values()) {
			rate += possibleRate;
		}
		return rate;
	}
	
	/**
	 * @return The abstract model of the reaction.
	 */
	public Reaction getReaction() {
		return reaction;
	}
	
	/**
	 * @return The rates for this reaction in each location.
	 */
	public Map<Membrane, Double> getRates() {
		return rates;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof PossibleReaction)) {
			return false;
		}
		return reaction.equals(((PossibleReaction) other).reaction);
	}
	
	@Override
    public int hashCode() {
		return reaction.hashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder()
		  .append(reaction.toString())
		  .append(" (")
		  .append(rates.size())
		  .append(" locations)");
		return result.toString();
	}
}
