package com.simomics.leishsim.parsing;

import java.util.LinkedHashSet;
import java.util.Set;

import com.simomics.leishsim.model.LocatedSpecies;
import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.model.Reaction.InvalidReactionException;
import com.simomics.leishsim.model.StoichiometrySpecies;
import com.simomics.leishsim.model.equations.Equation;

/**
 * Creates {@link Reaction} objects step-by-step.
 */
public class ReactionBuilder {

	private Set<StoichiometrySpecies> consumedSpecies;
	private Set<StoichiometrySpecies> producedSpecies;
	private Equation rateModifier;
	private Equation simplifiedRateModifier;
	private double delayHours;
	private String mdslLine;
	private Integer mdslLineNumber;
	
	/** Whether we are currently adding species to the left or right hand side of the reaction. */
	private boolean onLeftHandSide;
	
	/**
	 * Begin building a new reaction.
	 */
	public ReactionBuilder() {
		consumedSpecies = new LinkedHashSet<>();
		producedSpecies = new LinkedHashSet<>();
		rateModifier = null;
		delayHours = 0;
		onLeftHandSide = true;
	}
	
	/**
	 * Add a species to the left hand side of the reaction.
	 */
	public void addConsumedSpecies(StoichiometrySpecies species) {
		consumedSpecies.add(species);
	}

	/**
	 * Add a species to the right hand side of the reaction.
	 * @param species
	 */
	public void addProducedSpecies(StoichiometrySpecies species) {
		producedSpecies.add(species);
	}
	
	/**
	 * Add a species to whichever side of the reaction we are currently on (left or right).
	 * Defaults to left, but can be switched with {@link #switchToRightHandSide()}.
	 */
	public void addSpecies(StoichiometrySpecies species) {
		if (onLeftHandSide) {
			addConsumedSpecies(species);
		}
		else {
			addProducedSpecies(species);
		}
	}
	
	/**
	 * Switch to building the right hand side of the reaction.
	 * @see #addSpecies(LocatedSpecies)
	 */
	public void switchToRightHandSide() {
		onLeftHandSide = false;
	}
	
	public void setRateModifier(Equation rateModifier, Equation simplifiedRateModifier) {
		this.rateModifier = rateModifier;
		this.simplifiedRateModifier = simplifiedRateModifier;
	}
	
	public void setDelayHours(double delayHours) {
		this.delayHours = delayHours;
	}
	
	public void setMdslLine(String line, int lineNumber) {
		this.mdslLine = line;
		this.mdslLineNumber = lineNumber;
	}
	
	public Reaction create() throws InvalidReactionException {
		return new Reaction(consumedSpecies, producedSpecies, rateModifier, simplifiedRateModifier, delayHours, mdslLine, mdslLineNumber);
	}
}
