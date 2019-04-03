package com.simomics.leishsim.simulation;

import com.simomics.leishsim.model.Reaction;

/**
 * Represents which reaction has been chosen during one step of the Gillespie algorithm.
 */
public class ChosenReaction {

	/** The abstract description of the reaction */
	private final Reaction reaction;
	
	/** The location in which the reaction will fire */
	private final Membrane location;
	
	/** The number of seconds until the reaction will fire */
	private final double secondsToFiring;
	
	public ChosenReaction(Reaction reaction, Membrane location, double secondsToFiring) {
		this.reaction = reaction;
		this.location = location;
		this.secondsToFiring = secondsToFiring;
	}
	
	public Membrane getLocation() {
		return location;
	}
	
	public Reaction getReaction() {
		return reaction;
	}
	
	public double getSecondsToFiring() {
		return secondsToFiring;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder()
		  .append("---\n")
		  .append(secondsToFiring)
		  .append("\n")
		  .append(reaction.toString())
		  .append("\n")
		  .append(location.toString())
		  .append("\n---");
		return result.toString();
	}
}
