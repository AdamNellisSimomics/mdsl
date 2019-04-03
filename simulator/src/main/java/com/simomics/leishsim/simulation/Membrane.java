package com.simomics.leishsim.simulation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.model.LocatedSpecies;
import com.simomics.leishsim.model.Location;
import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.model.StoichiometrySpecies;
import com.simomics.leishsim.model.equations.Equation.InvalidEquationEvaluationException;
import com.simomics.leishsim.simulation.Simulation.InvalidSimulationException;

/**
 * One instance of a membrane in the simulation.
 * Includes all the membranes and species contained in this membrane, 
 *   and all the species bound on or under this membrane.
 */
public class Membrane {

	/** Map of which index we are up to for each membrane type */
	private final static Map<String,Integer> membraneTypeIndices = new HashMap<>();
	
	/** The name of this membrane's type */
	private final String type;
	
	/** Tags that can be used to refer to this membrane */
	private final Set<String> tags;
	
	/** Tag that uniquely identifies this instance of this membrane type */
	private final String uniqueTag;
	
	/** 
	 * The membrane within which this membrane is contained.
	 * Can be null if this is the top-level simulation membrane.
	 */
	private final Membrane parent;
	
	/** The numbers of each species bound to the outer side of this membrane */
	private final Multiset<String> speciesOn;
	
	/** The numbers of each species bound to the inner side of this membrane */
	private final Multiset<String> speciesUnder;
	
	/** The numbers of each species contained within this membrane */
	private final Multiset<String> speciesContained;
	
	/** The individual membranes contained within this membrane */
	private final Set<Membrane> membranesContained;
	
	/** All of the membranes within this tree (including self and all descendants) */
	private final Multimap<String,Membrane> allMembranes;
	
	/**
	 * @param type
	 * @param parent can be null if this is the top-LogType simulation membrane
	 */
	public Membrane(String type, Collection<String> tags, Membrane parent) {
		this.type = type;
		this.parent = parent;
		this.tags = new LinkedHashSet<>();
		this.uniqueTag = computeUniqueTag();
		// Add tags that are not unique tags
		for (String tag: tags) {
			if (!isUniqueTag(tag)) {
				this.tags.add(tag);
			}
		}
		
		speciesOn = LinkedHashMultiset.create();
		speciesUnder = LinkedHashMultiset.create();
		speciesContained = LinkedHashMultiset.create();
		membranesContained = new LinkedHashSet<>();
		allMembranes = LinkedHashMultimap.create();
		allMembranes.put(type, this);
	}
	
	/**
	 * Forget all the unique tags that we have generated so far.
	 * Newly instantiated Membranes will start indexing their tags from 1.
	 */
	public static void resetUniqueTags() {
		membraneTypeIndices.clear();
	}
	
	/**
	 * @return A new tag for this membrane type, this is different to all other tags.
	 */
	private String computeUniqueTag() {
		Integer membraneTypeIndex = membraneTypeIndices.getOrDefault(type, 0) + 1;
		membraneTypeIndices.put(type, membraneTypeIndex);
		return String.format("%s_%d", type, membraneTypeIndex);
	}
	
	/**
	 * @return true if the given tag is in the format of a unique tag, or false if not.
	 */
	private boolean isUniqueTag(String tag) {
		if (tag.startsWith(type + "_")) {
			String numberPart = tag.substring(type.length() + 1);
			if (StringUtils.isNumeric(numberPart)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return The name of this membrane type.
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @return The tag that uniquely identifies this membrane in the simulation.
	 */
	public String getUniqueTag() {
		return uniqueTag;
	}
	
	/**
	 * @param tag A tag name to check against this membrane.
	 * @return True if this membrane matches the given tag, or false if not.
	 */
	public boolean matchesTag(String tag) {
		return tags.contains(tag) || uniqueTag.equals(tag);
	}
	
	/**
	 * @return All the membranes directly contained within this membrane.
	 */
	public Collection<Membrane> getContainedMembranes() {
		return membranesContained;
	}
	
	/**
	 * @return All the membranes contained in this tree, including this membrane.
	 */
	public Collection<Membrane> getAllMembranes() {
		return allMembranes.values();
	}
	
	/**
	 * @param membraneName A membrane type or a tag
	 * @param location 
	 * @return All the membranes matching the given location relative to the given name, contained anywhere within this tree.
	 *         Includes this membrane, if it matches.  
	 */
	public Collection<Membrane> getMatchingMembranes(String membraneName, Location location) {
		Collection<Membrane> membranesSameName = allMembranes.get(membraneName);
		if (membranesSameName.isEmpty()) {
			// Check if membraneName is a tag
			membranesSameName = new HashSet<>(); // Don't change allMembranes map
			for (Membrane membrane: allMembranes.values()) {
				if (membrane.matchesTag(membraneName)) {
					membranesSameName.add(membrane);
				}
			}
		}
		if (location == Location.around) {
			return membranesSameName.stream()
					.map(membrane -> membrane.parent)
					.filter(membrane -> membrane != null)
					.collect(Collectors.toSet());
		}
		else {
			return membranesSameName;
		}
	}
	
	/**
	 * Cache for {@link #getMatchingMembranes(Reaction)}.
	 */
	private Map<Reaction, Collection<Membrane>> matchingMembranes = new HashMap<>();
	
	/**
	 * @return All the membranes matching the given reaction, contained anywhere within this tree.
	 *         Includes this membrane, if its name matches.  
	 */
	public Collection<Membrane> getMatchingMembranes(Reaction reaction) {
		Collection<Membrane> result = matchingMembranes.get(reaction);
		if (result == null) {
			result = computeMatchingMembranes(reaction);
			matchingMembranes.put(reaction, result);
		}
		return result;
	}
	
	private Collection<Membrane> computeMatchingMembranes(Reaction reaction) {
		if (reaction.isAroundReaction()) {
			return getMatchingMembranes(reaction.getMembraneType(), Location.around);
		}
		else {
			return allMembranes.get(reaction.getMembraneType());
		}
	}
	
	/**
	 * @return The membrane containing this membrane. Can be null if this is the top-level simulation membrane.
	 */
	public Membrane getParentMembrane() {
		return parent;
	}
	
	/**
	 * Adds a species to this membrane only (not to its children)
	 * @param locatedSpecies Can have a membrane name that is not the same as this membrane
	 */
	public void addSpecies(LocatedSpecies locatedSpecies, int howMany) {
		Location location = locatedSpecies.getLocation();
		String membraneName = locatedSpecies.getMembraneName();
		// If the membrane type is me, then add relative to me
		if (membraneName.equals(type) || matchesTag(membraneName)) {
			addSpecies(location, locatedSpecies.getSpeciesName(), howMany);
		}
		// "contained" locations are allowed to reference my parent
		else if (location == Location.contained) {
			if (parent != null && 
				(membraneName.equals(parent.getType()) || parent.matchesTag(membraneName))) {
				parent.addSpecies(location, locatedSpecies.getSpeciesName(), howMany);
			}
			else {
				// I'm not contained within the correct parent type
				Logging.getLogger(LogType.ERROR).log(String.format("Tried to add (%s) to an invalid membrane of type %s", locatedSpecies, type));
			}
		}
		// "around" locations are allowed to reference my children
		else if (location == Location.around) {
			// If I have a direct child of the given type, then add to me contained
			if (membranesContained.stream().anyMatch(membrane -> membrane.getType().equals(locatedSpecies.getMembraneName()))) {
				addSpecies(Location.contained, locatedSpecies.getSpeciesName(), howMany);
			}
			else {
				// TODO AN: Implement siblings if/when we need it
				Logging.getLogger(LogType.ERROR).log(String.format(
						"Could not find location to add: %s, %s\nNot yet implemented: Reactions using 'around' to reference a sibling membrane", 
						locatedSpecies, this));
			}
		}
		// "on" or "under" location for a membrane type other than me - invalid
		else {
			Logging.getLogger(LogType.ERROR).log(String.format("Location (%s) is not relative to membrane %s", locatedSpecies, type));
		}
	}
	
	/**
	 * Adds a species to this membrane only (not to its children)
	 */
	public void addSpecies(Location location, String speciesName, int howMany) {
		Multiset<String> set = null;
		switch (location) {
		case on:
			set = speciesOn;
			break;
		case under:
			set = speciesUnder;
			break;
		case contained:
			set = speciesContained;
			break;
		case around:
			if (parent == null) {
				Logging.getLogger(LogType.ERROR).log("Tried to add species to 'around' for the top-LogType membrane");
				return;
			}
			parent.addSpecies(Location.contained, speciesName, howMany);
			return;
		default:
			Logging.getLogger(LogType.ERROR).log(String.format("Unknown location : %s", location));
			return;
		}
		if (howMany > 0) {
			set.add(speciesName, howMany);
		}
		else {
			set.remove(speciesName, -howMany);
		}
	}
	
	/**
	 * Puts the given membrane within this membrane. 
	 * @param membrane
	 */
	public void addMembrane(Membrane membrane) {
		membranesContained.add(membrane);
		
		// Collect a reference to each membrane anywhere in the tree, indexed by name
		Stack<Membrane> currentMembranes = new Stack<>();
		currentMembranes.add(membrane);
		while (!currentMembranes.isEmpty()) {
			Membrane currentMembrane = currentMembranes.pop();
			allMembranes.put(currentMembrane.getType(), currentMembrane);
			currentMembranes.addAll(currentMembrane.getContainedMembranes());
		}
	}
	
	/**
	 * @param reaction
	 * @return The rate at which this reaction will proceed relative to this membrane.
	 * Assumes that the given reaction is relative to this membrane.
	 * @throws InvalidSimulationException 
	 */
	public double computeRate(Reaction reaction) throws InvalidSimulationException {
		try {
			double rate = reaction.getRateEquation().evaluate(this);
			if (rate < 0) {
				throw new InvalidSimulationException(String.format("Negative reaction rate: %s for reaction: %s", rate, reaction.toString()), null);
			}
			return rate;
		} catch (InvalidEquationEvaluationException e) {
			throw new InvalidSimulationException(String.format("Could not evaluate rate for reaction: %s", reaction.toString()), e);
		}
	}
	
	/**
	 * Check the stoichiometry of a reaction against this membrane.
	 * @param reaction A reaction that might be able to fire.
	 * @return true if the given reaction can fire relative to this membrane, or false if not.
	 * Assumes that the given reaction is relative to this membrane.
	 */
	public boolean stoichiometryMatches(Reaction reaction) {
		// Check every species on the LHS of the reaction
		for (StoichiometrySpecies stoichSpecies: reaction.getConsumedSpecies()) {
			if (numSpecies(stoichSpecies) < stoichSpecies.getStochiometry()) {
				return false;
			}
		}
		// All species matched
		return true;
	}
	
	/**
	 * @param locatedSpecies Can have a membrane name that is not the same as this membrane
	 * @return The number of species in the given location relative to this membrane.
	 */
	public int numSpecies(LocatedSpecies locatedSpecies) {
		Location location = locatedSpecies.getLocation();
		String membraneName = locatedSpecies.getMembraneName();
		// If the membrane type is me, then check relative to me
		if (membraneName.equals(type)) {
			return numSpecies(locatedSpecies.getSpeciesName(), location);
		}
		// "contained" locations are allowed to reference my parent
		else if (location == Location.contained) {
			if (membraneName.equals(parent.getType())) {
				return parent.numSpecies(locatedSpecies.getSpeciesName(), location);
			}
			else {
				// I'm not contained within the correct parent type
				return 0;
			}
		}
		// "around" locations are allowed to reference my children
		else if (location == Location.around) {
			
			// If I have a direct child of the given type, then return "me contained"
			if (membranesContained.stream().anyMatch(membrane -> membrane.getType().equals(locatedSpecies.getMembraneName()))) {
				return numSpecies(locatedSpecies.getSpeciesName(), Location.contained);
			}
			else {
				// TODO AN: Implement siblings if/when we need it
				Logging.getLogger(LogType.ERROR).log(String.format(
						"Could not find location: %s, %s\nNot yet implemented: Reactions using 'around' to reference a sibling membrane", 
						locatedSpecies, this));
				return 0;
			}
		}
		// "on" or "under" location for a membrane type other than me - invalid
		else {
			Logging.getLogger(LogType.ERROR).log(String.format("Location (%s) is not relative to membrane %s", locatedSpecies, type));
			return 0;
		}
	}
	
	/**
	 * @param speciesName
	 * @param location
	 * @return The number of species in the given location relative to this membrane
	 */
	public int numSpecies(String speciesName, Location location) {
		switch (location) {
		case around:
			if (parent == null) {
				Logging.getLogger(LogType.ERROR).log("Tried to query 'around' for the top-level membrane");
				return 0;
			}
			return parent.speciesContained.count(speciesName);
		case contained:
			return speciesContained.count(speciesName);
		case on:
			return speciesOn.count(speciesName);
		case under:
			return speciesUnder.count(speciesName);
		default:
			Logging.getLogger(LogType.ERROR).log(String.format("Unknown location : %s", location));
			return 0;
		}
	}
	
	/**
	 * @param speciesName
	 * @return The total number of the given species in the whole tree.
	 * TODO AN: This is inefficient to call regularly. Could replace with a cache in simulation, updated when reactions fire.
	 */
	public int getAllSpecies(String speciesName) {
		int total = 0;
		total += numSpecies(speciesName, Location.contained);
		total += numSpecies(speciesName, Location.on);
		total += numSpecies(speciesName, Location.under);
		for (Membrane child: membranesContained) {
			total += child.getAllSpecies(speciesName);
		}
		return total;
	}
	
	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();
		toString(2, output);
		return output.toString();
	}
	
	/**
	 * Print out a human-readable representation of the membrane
	 * @param indent Amount to indent by, so that we can display the tree in text form
	 * @param output Use the same StringBuilder, for efficiency
	 */
	private void toString(int indent, StringBuilder output) {
		String indentStr = StringUtils.repeat(' ', indent);
		output
		  .append(indentStr)
		  .append(type);
		if (!tags.isEmpty()) {
			output.append(" [");
			for (String tag: tags) {
				output.append(tag).append(", ");
			}
			output.setLength(output.length() - 2);
			output.append("]");
		}
		appendSpeciesLocation(speciesOn, "On", indent, output);
		appendSpeciesLocation(speciesUnder, "Under", indent, output);
		appendSpeciesLocation(speciesContained, "Contained", indent, output);
		for (Membrane childMembrane: membranesContained) {
			output.append('\n');
			childMembrane.toString(indent + 2, output);
		}
	}

	/**
	 * If there are any species present in the given location, then print them to the given StringBuilder.
	 */
	private void appendSpeciesLocation(Multiset<String> speciesLocation, String locationName, int indent, StringBuilder output) {
		if (!speciesLocation.isEmpty()) {
			output
			  .append('\n')
			  .append(StringUtils.repeat(' ', indent + 2))
			  .append(locationName)
			  .append(": ");
			for (Entry<String> entry: speciesLocation.entrySet()) {
				output
				  .append(entry.getCount())
				  .append(' ')
				  .append(entry.getElement())
				  .append(", ");
			}
			output.setLength(output.length() - 2);
		}
	}
	
	/**
	 * @return The full state of the membrane, as MDSL code for an initial tree and species definitions.
	 */
	public String toFullStateString() {
		StringBuilder outputTree = new StringBuilder();
		StringBuilder outputSpecies = new StringBuilder();
		outputTree.append("initial tree \n");
		toFullStateStringTree(2, outputTree, outputSpecies);
		return outputTree.toString() + "\n\n" + outputSpecies.toString();
	}
	
	/**
	 * Recursive call to add the section of the initial tree for this membrane and its descendants.
	 * @param indent The current indentation level, for human readability of the output.
	 * @param membraneTypeIndices Map of which index we are up to for each membrane type.
	 * @param outputTree Cumulative output so far for the initial tree.
	 * @param outputSpecies Cumulative output so far for the species lines.
	 */
	private void toFullStateStringTree(int indent, StringBuilder outputTree, StringBuilder outputSpecies) {
		writeSpeciesLines(speciesOn, Location.on, outputSpecies);
		writeSpeciesLines(speciesUnder, Location.under, outputSpecies);
		writeSpeciesLines(speciesContained, Location.contained, outputSpecies);
		
		String indentStr = StringUtils.repeat(' ', indent);
		outputTree
		  .append(indentStr)
		  .append("{ [ ")
		  .append(uniqueTag);
		for (String tag: tags) {
			outputTree
			  .append(" ")
			  .append(tag);
		}
		outputTree.append(" ] ")
		  .append(type);
		for (Membrane childMembrane: getContainedMembranes()) {
			outputTree.append("\n");
			childMembrane.toFullStateStringTree(indent + 2, outputTree, outputSpecies);
		}
		outputTree.append(" }");
	}
	
	/**
	 * Add the species lines for the given location.
	 * @param speciesLocation The species at this location.
	 * @param location Which location.
	 * @param membraneIndexTag The unique tag for this membrane.
	 * @param output Cumulative output so far, that we should write to.
	 */
	private void writeSpeciesLines(Multiset<String> speciesLocation, Location location, StringBuilder output) {
		for (Entry<String> entry: speciesLocation.entrySet()) {
			output
			  .append("species ")
			  .append(entry.getElement())
			  .append(" ")
			  .append(location)
			  .append(" ")
			  .append(uniqueTag)
			  .append(" = ")
			  .append(entry.getCount())
			  .append(" units\n");
		}
	}
}
