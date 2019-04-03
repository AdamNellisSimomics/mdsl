package com.simomics.leishsim.simulation;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.model.LocatedSpecies;
import com.simomics.leishsim.model.MembraneModel;
import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.model.StoichiometrySpecies;

/**
 * A simulation that uses a table to keep track of the reaction rates in each membrane.
 */
public class ReactionRateTableSimulation extends Simulation {

	/** The reaction that fired in the previous step */
	private Reaction previousReaction;
	private Integer previousReactionId;
	
	/** The location in which the reaction fired in the previous step */
	private Membrane previousMembrane;
	private Integer previousMembraneId;
	
	/** Matrix of rates for each reaction-membrane location. Reactions are the first index; membranes the second index. */
	private Double[][] reactionRates;
	
	private BiMap<Reaction, Integer> reactionIds;
	private BiMap<Membrane, Integer> membraneIds;
	
	/** Which (reaction,membrane) pairs depend on each other */
	private Multimap<ReactionRateIndex, ReactionRateIndex> membraneDependencies;
	
	/**
	 * Creates the table and populates it with the initial reaction rates.
	 * @param model
	 * @param initialConditions
	 * @param randomSeed
	 * @throws InvalidSimulationException
	 */
	public ReactionRateTableSimulation(MembraneModel model, Membrane initialConditions, Integer randomSeed)	throws InvalidSimulationException {
		super(model, initialConditions, randomSeed);
		
		// Initialise data structures
		this.reactionIds = HashBiMap.create(); // TODO: Turn these maps into properties of the membrane and reaction classes?
		this.membraneIds = HashBiMap.create();
		int reactionNum = 0;
		for (Reaction reaction: model.getReactions()) {
			this.reactionIds.put(reaction, reactionNum);
			reactionNum += 1;
		}
		int membraneNum = 0;
		for (Membrane membrane: initialConditions.getAllMembranes()) {
			this.membraneIds.put(membrane, membraneNum);
			membraneNum += 1;
		}
		this.reactionRates = new Double[this.reactionIds.size()][];
		for (int reactionId=0; reactionId<reactionIds.size(); reactionId++) {
			Reaction reaction = reactionIds.inverse().get(reactionId);
			Collection<Membrane> matchingMembranes = initialConditions.getMatchingMembranes(reaction);
			this.reactionRates[reactionId] = new Double[this.membraneIds.size()];
			for (int membraneId=0; membraneId<membraneIds.size(); membraneId++) {
				Membrane membrane = membraneIds.inverse().get(membraneId);
				if (matchingMembranes.contains(membrane) && membrane.stoichiometryMatches(reaction)) {
					this.reactionRates[reactionId][membraneId] = membrane.computeRate(reaction);
				}
				else {
					this.reactionRates[reactionId][membraneId] = 0d;
				}
			}
		}
		
		if (Logging.isLoggable(LogType.FULL)) {
			Logging.getLogger(LogType.FULL).log("Initial reaction rates table:\n" + printReactionRatesTable());
		}
		
		this.membraneDependencies = calculateMembraneDependencies();
	}
	
	/**
	 * Work out which table entries need to have their rates re-calculated when a given reaction fires in a given membrane.
	 * Only needs computing once - doesn't change as the simulation runs.
	 */
	private Multimap<ReactionRateIndex, ReactionRateIndex> calculateMembraneDependencies() {
		Multimap<ReactionRateIndex, ReactionRateIndex> dependencies = HashMultimap.create();
		
		// For each reaction that could fire (and each membrane it could fire in)
		for (int firedReactionId=0; firedReactionId<reactionIds.size(); firedReactionId++) {
			Reaction firedReaction = reactionIds.inverse().get(firedReactionId);
			Set<StoichiometrySpecies> modifiedLocatedSpecies = firedReaction.getModifiedLocatedSpecies();
			Set<Reaction> dependentReactions = model.getDependentReactions(firedReaction);
			for (Membrane firedMembrane: currentState.getMatchingMembranes(firedReaction)) {
				ReactionRateIndex firedReactionIndex = new ReactionRateIndex(reactionIds.get(firedReaction), membraneIds.get(firedMembrane));
				
				// For each reaction that could be dependent on the firing reaction (and each membrane the dependent reaction could fire in)
				for (Reaction dependentReaction: dependentReactions) {
					Set<LocatedSpecies> dependentLocatedSpecies = dependentReaction.getDependentLocatedSpecies();
					for (Membrane dependentMembrane: currentState.getMatchingMembranes(dependentReaction)) {
						ReactionRateIndex dependentReactionIndex = new ReactionRateIndex(reactionIds.get(dependentReaction), membraneIds.get(dependentMembrane));
						
						// Check each species modified by the firing reaction, to see if the dependent reaction needs it
						speciesCheck: for (StoichiometrySpecies modifiedSpecies: modifiedLocatedSpecies) {
							for (LocatedSpecies dependentSpecies: dependentLocatedSpecies) {
								
								// TODO: Could improve this to check the locations and membrane types as well.
								//       Currently, we don't have any species in two multiple membrane types, so not needed
								//   NB: Need to be careful to use parent membrane types for "around" locations, etc.
								if (modifiedSpecies.getSpeciesName().equals(dependentSpecies.getSpeciesName())) {
									dependencies.put(firedReactionIndex, dependentReactionIndex);
									break speciesCheck;
								}
							}
						}
					}
				}
			}
		}
		return dependencies;
	}
	
	/**
	 * Compute the rates of all viable reactions.
	 * Considers which reactions depend on which others, and the membranes in which they fire.
	 * @throws InvalidSimulationException If any of the reaction rates could not be computed.
	 */
	private void updateViableReactions() throws InvalidSimulationException {
		if (previousReaction == null) {
			return; // Rates have already been initialised, and no reactions fired yet
		}
		// Re-compute rates that might have changed
		ReactionRateIndex previousReactionIndex = new ReactionRateIndex(previousReactionId, previousMembraneId);
		for (ReactionRateIndex index: membraneDependencies.get(previousReactionIndex)) {
			int dependentMembraneId = index.getMembraneId();
			int dependentReactionId = index.getReactionId();
			Membrane dependentMembrane = membraneIds.inverse().get(dependentMembraneId);
			Reaction dependentReaction = reactionIds.inverse().get(dependentReactionId);
			
			// If the reaction could happen here, then calculate its rate
			if (dependentMembrane.stoichiometryMatches(dependentReaction)) {
				reactionRates[dependentReactionId][dependentMembraneId] = dependentMembrane.computeRate(dependentReaction);
			}
			// Otherwise, its rate is zero
			else {
				reactionRates[dependentReactionId][dependentMembraneId] = 0d;
			}
		}

		if (Logging.isLoggable(LogType.FULL)) {
			Logging.getLogger(LogType.FULL).log("Current reaction rates table:\n" + printReactionRatesTable());
		}
	}
	
	/**
	 * Rolls all the random numbers needed for one step. 
	 * Chooses which reaction fires next, in which location and at what time.
	 * @return The next reaction to fire, or null if no reactions are viable.
	 * @throws InvalidSimulationException 
	 */
	@Override
	protected ChosenReaction chooseRandomReaction() throws InvalidSimulationException {
		// Determine which reactions are viable
		updateViableReactions();
				
		// Roll a random number to pick which time the next reaction fires
		double omega = 0;
		for (int reactionId=0; reactionId<reactionIds.size(); reactionId++) { 
			for (int membraneId=0; membraneId<membraneIds.size(); membraneId++) { 
				omega += reactionRates[reactionId][membraneId]; 
			}
		}
		if (omega == 0d) {
			// No viable reactions - simulation finished
			return null;
		}
		double hoursToNextReaction = (double) ((1.0 / omega) * Math.log(1.0 / rand.nextDouble())); // Equation 21a in Gillespie's paper
		double secondsToNextReaction = hoursToNextReaction * 60 * 60;
		
		// Choose which of the reactions fires
		double whichReaction = omega * rand.nextDouble();
		double currentReaction = 0f;
		Reaction chosenReaction = null;
		Membrane chosenMembrane = null;
		outer: for (int reactionId=0; reactionId<reactionIds.size(); reactionId++) { 
			for (int membraneId=0; membraneId<membraneIds.size(); membraneId++) { 
				currentReaction += reactionRates[reactionId][membraneId]; 
				if (whichReaction <= currentReaction) { 
					chosenReaction = reactionIds.inverse().get(reactionId); 
					chosenMembrane = membraneIds.inverse().get(membraneId); 
					break outer; 
				} 
			}
		}
		if (chosenReaction == null) {
			Logging.getLogger(LogType.ERROR).log("Unexpected error! chosenReaction=null");
		}
		else {
			previousReaction = chosenReaction;
			previousMembrane = chosenMembrane;
			previousReactionId = reactionIds.get(chosenReaction);
			previousMembraneId = membraneIds.get(chosenMembrane);
		}
		
		// Collect the results together to return
		return new ChosenReaction(chosenReaction, chosenMembrane, secondsToNextReaction);
	}
	
	/**
	 * Used for debugging.
	 * Print the whole table of reaction rates.
	 */
	private String printReactionRatesTable() {
		StringBuilder result = new StringBuilder();
		for (int membraneId=0; membraneId<membraneIds.size(); membraneId++) {
			result.append(String.format("%s  ", membraneIds.inverse().get(membraneId).getType()));
		}
		result.append("\n");
		for (int reactionId=0; reactionId<reactionIds.size(); reactionId++) {
			for (int membraneId=0; membraneId<membraneIds.size(); membraneId++) {
				result.append(String.format("%e  ", reactionRates[reactionId][membraneId]));
			}
			result.append(String.format("{%s}", reactionIds.inverse().get(reactionId)));
			result.append("\n");
		}
		return result.toString();
	}

	/**
	 * @return The propensity of the given reaction, summed over all membranes.
	 */
	@Override
	protected double getReactionPropensity(Reaction reaction) {
		Double[] ratesPerMembrane = reactionRates[reactionIds.get(reaction)];
		double totalPropensity = 0;
		for (int membraneId = 0; membraneId < ratesPerMembrane.length; membraneId++) {
			totalPropensity += ratesPerMembrane[membraneId];
		}
		return totalPropensity;
	}

	@Override
	protected void recalculatePropensitiesFrom(Reaction reaction, Membrane membrane) throws InvalidSimulationException {
		previousReaction = reaction;
		previousMembrane = membrane;
		previousReactionId = reactionIds.get(reaction);
		previousMembraneId = membraneIds.get(membrane);
		updateViableReactions();
		previousReaction = null;
		previousMembrane = null;
		previousReactionId = null;
		previousMembraneId = null;
	}
}
