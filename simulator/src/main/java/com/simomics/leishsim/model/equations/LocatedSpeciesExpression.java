package com.simomics.leishsim.model.equations;

import org.apache.commons.math3.util.CombinatoricsUtils;

import com.simomics.leishsim.model.LocatedSpecies;
import com.simomics.leishsim.model.StoichiometrySpecies;
import com.simomics.leishsim.simulation.Membrane;

public class LocatedSpeciesExpression implements Equation {

	private final LocatedSpecies locatedSpecies;
	private final int stoichiometry;

	public LocatedSpeciesExpression(LocatedSpecies locatedSpecies) {
		this.locatedSpecies = locatedSpecies;
		this.stoichiometry = 1;
	}
	
	public LocatedSpeciesExpression(StoichiometrySpecies locatedSpecies) {
		this.locatedSpecies = locatedSpecies;
		this.stoichiometry = locatedSpecies.getStochiometry();
	}
	
	public LocatedSpecies getLocatedSpecies() {
		return locatedSpecies;
	}
	
	@Override
	public String toString() {
		return '(' + locatedSpecies.toString() + ')';
	}

	@Override
	public double evaluate(Membrane membrane) {
		int numSpecies = membrane.numSpecies(locatedSpecies);
		switch (stoichiometry) {
		case 0: // Genes and catalysts have order 1 in the rate equation, but 0 when adding/removing species
		case 1:
			return numSpecies;
		case 2:
			return numSpecies * (numSpecies - 1) / 2;
		default:
			return CombinatoricsUtils.binomialCoefficientDouble(numSpecies, stoichiometry);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != getClass()) { return false; }
		LocatedSpeciesExpression other = (LocatedSpeciesExpression) obj;
		return (locatedSpecies.equals(other.locatedSpecies)) && (stoichiometry == other.stoichiometry);
	}
}
