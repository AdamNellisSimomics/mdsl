package com.simomics.leishsim.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.simomics.leishsim.model.equations.Equation;
import com.simomics.leishsim.model.equations.LocatedSpeciesExpression;
import com.simomics.leishsim.model.equations.OperationExpression;

/**
 * A model defined by the user. 
 */
public class MembraneModel {

	private final Set<String> membraneNames;
	private final Set<String> speciesNames;
	private final Multiset<LocatedSpecies> initialSpecies;
	private final Map<String, Double> parameterValues;
	private final Set<Reaction> reactions;
	
	private final Multimap<String, String> speciesToTags;
	private final Multimap<String, String> tagsToSpecies;
	
	/** The reactions that must have their rates re-calculated when a reaction fires */
	private final Map<Reaction, Set<Reaction>> reactionDependencies;
	
	public MembraneModel(Set<String> membraneNames, Set<String> speciesNames, Multiset<LocatedSpecies> initialSpecies, Map<String, Double> parameterValues, 
			Set<Reaction> reactions,
			Multimap<String, String> speciesToTags, Multimap<String, String> tagsToSpecies) {
		this.membraneNames = membraneNames;
		this.speciesNames = speciesNames;
		this.initialSpecies = initialSpecies;
		this.parameterValues = parameterValues;
		this.reactions = reactions;
		this.reactionDependencies = calculateReactionDependencies();
		this.speciesToTags = speciesToTags;
		this.tagsToSpecies = tagsToSpecies;
	}
	
	public Set<String> getSpeciesNames() {
		return speciesNames;
	}
	
	public Multiset<LocatedSpecies> getInitialSpecies() {
		return initialSpecies;
	}
	
	public Collection<String> getSpeciesForTag(String tag) {
		return tagsToSpecies.get(tag);
	}
	
	public Collection<String> getTagsForSpecies(String speciesName) {
		return speciesToTags.get(speciesName);
	}
	
	public Set<String> getParameterNames() {
		return parameterValues.keySet();
	}
	
	public int getNumReactions() {
		return reactions.size();
	}
	
	public Set<Reaction> getReactions() {
		return reactions;
	}
	
	/**
	 * @return The value of a particular parameter, or null if that parameter is not defined in this model.
	 */
	public Double getParameterValue(String parameterName) {
		return parameterValues.get(parameterName);
	}
	
	/**
	 * @param firedReaction The reaction that fired in the last step. Can be null if no reactions have fired yet. 
	 * @return The reactions that need their rates to be recomputed after the firing of the given reaction.
	 * If the given reaction is null, then all reactions need their rates re-computing, so all reactions are returned.
	 */
	public Set<Reaction> getDependentReactions(Reaction firedReaction) {
		if (firedReaction == null) {
			return reactions;
		}
		return reactionDependencies.get(firedReaction);
	}
	
	/**
	 * Works out which reactions need to have their rates re-calculated when a given reaction fires.
	 * Only needs computing once - doesn't change as the simulation runs.
	 */
	private Map<Reaction, Set<Reaction>> calculateReactionDependencies() {
		Map<Reaction, Set<Reaction>> dependencies = new HashMap<>();
		for (Reaction firedReaction: reactions) {
			Set<String> modifiedSpecies = firedReaction.getModifiedSpecies();
			Set<Reaction> recalcReactions = new HashSet<>();
			for (Reaction checkReaction: reactions) {
				// If checkReaction uses any modified species in its rate equation, then checkReaction is dependent
				Stack<Equation> equationsToCheck = new Stack<>();
				equationsToCheck.add(checkReaction.getRateEquation());
				while (!equationsToCheck.isEmpty()) {
					Equation equation = equationsToCheck.pop();
					if (equation instanceof LocatedSpeciesExpression) {
						if (modifiedSpecies.contains(((LocatedSpeciesExpression) equation).getLocatedSpecies().getSpeciesName())) {
							recalcReactions.add(checkReaction);
							break;
						}
					}
					else if (equation instanceof OperationExpression) {
						equationsToCheck.add(((OperationExpression) equation).getLeft());
						equationsToCheck.add(((OperationExpression) equation).getRight());
					}
				}
			}
			dependencies.put(firedReaction, recalcReactions);
		}
		return dependencies;
	}
	
	/**
	 * @return All reactions involving species with the given tag.
	 */
	public Collection<Reaction> getReactionsForSpeciesTag(String speciesTag) {
		Set<Reaction> applicableReactions = new HashSet<>();
		Set<String> speciesNames = ImmutableSet.copyOf(tagsToSpecies.get(speciesTag));
		for (Reaction reaction: reactions) {
			if (Sets.intersection(speciesNames, reaction.getInvolvedSpecies()).size() > 0) {
				applicableReactions.add(reaction);
			}
		}
		return applicableReactions;
	}
}
