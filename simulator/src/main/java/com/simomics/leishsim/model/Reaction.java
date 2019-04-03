package com.simomics.leishsim.model;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.simomics.leishsim.model.equations.Equation;
import com.simomics.leishsim.model.equations.LocatedSpeciesExpression;
import com.simomics.leishsim.model.equations.MultiplyExpression;
import com.simomics.leishsim.model.equations.OperationExpression;

/**
 * Model describing a reaction.
 */
public class Reaction {

	/** The species on the left hand side of the reaction, that are consumed by the reaction */
	private final Set<StoichiometrySpecies> consumedSpecies;

	/** The species on the right hand side of the reaction, that are produced by the reaction */
	private final Set<StoichiometrySpecies> producedSpecies;

	/** The modifier on the rate at which the reaction happens */
	private final Equation rateModifier;
	
	/** The actual equation that is evaluated to determine the reaction rate */
	private final Equation rateEquation;
	
	/** The delay to apply to this reaction */
	private final double delayHours;

	/** @see #getMembraneType() */
	private final String membraneType;
	
	/** @see #isAroundReaction() */
	private boolean aroundReaction;
	
	/** The text in the MDSL file that created this reaction object */
	private final String mdslLine;
	
	/** The line number of this reaction in the MDSL file */
	private final Integer mdslLineNumber;

	/**
	 * Thrown when a reaction could not be created because the input was invalid.
	 */
	public class InvalidReactionException extends Exception {
		public InvalidReactionException(String message) {
			super("Invalid reaction: " + message);
		}
	}

	/**
	 * Create a reaction, given all the necessary information.
	 * To create a reaction gradually, use the {@link #ReactionBuilder} class.
	 * @throws InvalidReactionException 
	 */
	public Reaction(Set<StoichiometrySpecies> consumedSpecies, Set<StoichiometrySpecies> producedSpecies, 
			Equation rateModifier, Equation simplifiedRateModifier, double delayHours,
			String mdslLine, Integer mdslLineNumber) throws InvalidReactionException {
		this.consumedSpecies = consumedSpecies;
		this.producedSpecies = producedSpecies;
		this.rateModifier = rateModifier;
		this.membraneType = computeMembraneType();
		this.rateEquation = writeRateEquation(simplifiedRateModifier);
		this.delayHours = delayHours;
		this.mdslLine = mdslLine;
		this.mdslLineNumber = mdslLineNumber;
	}
	
	/**
	 * Create the rate equation by multiplying the modifier by the concentrations of the reactants.
	 * e.g. reaction "3 a + 2 b modifier k -> c" has rate equation: "a^3 * b^2 * k"
	 * 
	 * TODO AN: This will become more complicated when/if we allow negative stoichiometries on the RHS.
	 * @param simplifiedRateModifier A simplified version of the rate modifier equation
	 * @return The equation that should be evaluated to calculate the rate of the reaction.
	 */
	private Equation writeRateEquation(Equation simplifiedRateModifier) {
		Equation rateEquation = simplifiedRateModifier;
		for (StoichiometrySpecies stoicSpecies: consumedSpecies) {
			// Multiply rate modifier equation by the numbers of each species on the LHS of the reaction
			rateEquation = new MultiplyExpression(new LocatedSpeciesExpression(stoicSpecies), rateEquation);
		}
		return rateEquation;
	}
	
	public Set<StoichiometrySpecies> getConsumedSpecies() {
		return consumedSpecies;
	}

	public Set<StoichiometrySpecies> getProducedSpecies() {
		return producedSpecies;
	}

	/**
	 * @return The (simplified) equation that will be computed on a given membrane, to calculate the current rate of the reaction.
	 */
	public Equation getRateEquation() {
		return rateEquation;
	}
	
	/**
	 * @return The (not simplified) modifier on the reaction rate.
	 */
	public Equation getRateModifier() {
		return rateModifier;
	}

	/**
	 * @return The delay from executing this reaction before its products appear in the simulation. Measured in hours.
	 */
	public double getDelayHours() {
		return delayHours;
	}
	
	/**
	 * @return The delay from executing this reaction before its products appear in the simulation. Measured in seconds.
	 */
	public double getDelaySeconds() {
		return delayHours * 60 * 60;
	}
	
	/**
	 * @return The name of the type of membrane that this reaction takes place with respect to.
	 * In version 1, we have all reactions happening with respect to just one membrane type.
	 */
	public String getMembraneType() {
		return membraneType;
	}
	
	/**
	 * @return True if the membrane type of this reaction is only used in "around" locations.
	 */
	public boolean isAroundReaction() {
		return aroundReaction;
	}
	
	public String getMdslLine() {
		return mdslLine;
	}

	public Integer getMdslLineNumber() {
		return mdslLineNumber;
	}

	/**
	 * Computes the membrane type that this reaction applies to.
	 *   If only one membrane type in LHS, then that's it.
	 *   If type X used in on/under, and type Y used in contained, then must have Y as parent of X
	 *   If type X used in on/under, and type Y used in around, then must have Y as sibling of X
	 *   If type X used in on/under, and type Y used in on/under, then error
	 *   If type X used in contained, and type Y used in contained, then error
	 *   If type X used in contained, and type Y used in around, then error
	 * @return The name of a membrane type
	 * @throws InvalidReactionException
	 */
	private String computeMembraneType() throws InvalidReactionException {
		this.aroundReaction = true;
		String typeX = null;
		String typeY = null;
		Stream<StoichiometrySpecies> involvedSpecies = Stream.concat(consumedSpecies.stream(), producedSpecies.stream());
		for (StoichiometrySpecies stoichiometrySpecies : (Iterable<StoichiometrySpecies>) involvedSpecies::iterator) {
			String type = stoichiometrySpecies.getMembraneName();
			Location location = stoichiometrySpecies.getLocation(); 
			if (location.equals(Location.on) || location.equals(Location.under)) {
				this.aroundReaction = false;
				if (typeX == null || typeX.equals(type)) {
					// First on/under species, or another on/under species of the same type
					typeX = type;
				}
				else {
					// Two on/under species of different types - error
					throw new InvalidReactionException("Multiple 'on' or 'under' locations for different membranes.");
				}
			}
			else {
				if (location.equals(Location.contained)) {
					this.aroundReaction = false;
				}
				if (typeY == null || typeY.equals(type)) {
					// First contained/around species, or another contained/around species of the same type
					typeY = type;
				}
				else {
					// Two contained/around species of different types - error
					throw new InvalidReactionException("Multiple 'contained' or 'around' locations for different membranes.");
				}
			}
		}
		// If we've got here, then the membrane types don't conflict
		if (typeX != null) {
			return typeX;
		}
		else if (typeY != null) {
			return typeY;
		}
		else {
			// No types specified, because no LHS of reaction - error
			throw new InvalidReactionException("No left hand side of reaction");
		}
	}
	
	/**
	 * @return The names of species that are modified by this reaction (either produced or consumed with stoichiometry > 0)
	 */
	public Set<String> getModifiedSpecies() {
		return streamModifiedLocatedSpecies()
				.map(stoichSpecies -> stoichSpecies.getSpeciesName())
				.collect(Collectors.toSet());
	}
	
	/**
	 * @return The species that are modified by this reaction (either produced or consumed with stoichiometry > 0)
	 */
	public Set<StoichiometrySpecies> getModifiedLocatedSpecies() {
		return streamModifiedLocatedSpecies().collect(Collectors.toSet());
	}
	
	/**
	 * @return The species that are modified by this reaction (either produced or consumed with stoichiometry > 0)
	 */
	private Stream<StoichiometrySpecies> streamModifiedLocatedSpecies() {
		return Stream.concat(consumedSpecies.stream(), producedSpecies.stream())
				.filter(stoichSpecies -> stoichSpecies.getStochiometry() != 0);
	}
	
	/**
	 * @return The located species that are involved in this reaction (on the lhs or the rhs)
	 */
	public Stream<LocatedSpecies> streamInvolvedLocatedSpecies() {
		return Stream.concat(consumedSpecies.stream(), producedSpecies.stream());
	}
	
	/**
	 * @return The species that are involved in this reaction (on the lhs or the rhs)
	 */
	public Set<String> getInvolvedSpecies() {
		return streamInvolvedLocatedSpecies()
				.map(locatedSpecies -> locatedSpecies.getSpeciesName())
				.collect(Collectors.toSet());
	}
	
	/**
	 * @return The species that this reaction's rate depends on.
	 */
	public Set<LocatedSpecies> getDependentLocatedSpecies() {
		Set<LocatedSpecies> dependentSpecies = new HashSet<>();
		Stack<Equation> equationsToCheck = new Stack<>();
		equationsToCheck.add(getRateEquation());
		while (!equationsToCheck.isEmpty()) {
			Equation equation = equationsToCheck.pop();
			if (equation instanceof LocatedSpeciesExpression) {
				dependentSpecies.add(((LocatedSpeciesExpression) equation).getLocatedSpecies());
			}
			else if (equation instanceof OperationExpression) {
				equationsToCheck.add(((OperationExpression) equation).getLeft());
				equationsToCheck.add(((OperationExpression) equation).getRight());
			}
		}
		return dependentSpecies;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder()
		  .append(printCombinedSpecies(consumedSpecies))
		  .append(" -> ")
		  .append(printCombinedSpecies(producedSpecies))
		  .append(" (forward rate modifier: ")
		  .append(rateModifier)
		  .append(")")
		  .append(" (delay: ")
		  .append(delayHours)
		  .append(")");
		return result.toString();
	}
	
	/**
	 * We don't have a class representing combinedSpecies objects.
	 * Representing them in here as a set of LocatedSpecies.
	 * @return A string representation of the given combinedSpecies
	 */
	private String printCombinedSpecies(Set<StoichiometrySpecies> combinedSpecies) {
		StringBuilder result = new StringBuilder();
		for (StoichiometrySpecies stoichiometrySpecies: combinedSpecies) {
			result
			  .append(stoichiometrySpecies.toString())
			  .append(" + ");
		}
		if (result.length() >= 3) {
			result.replace(result.length() - 3, result.length(), "");
		}
		return result.toString();
	}
}
