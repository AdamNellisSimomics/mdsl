package com.simomics.leishsim.parsing;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.simomics.leishsim.model.LocatedSpecies;
import com.simomics.leishsim.model.Location;
import com.simomics.leishsim.simulation.Membrane;

/**
 * Creates {@link Membrane} objects.
 * This builder allows you to build a tree where the nodes have duplicates.
 * Calling {@link #create()} resolves the duplicates into individual instances.
 */
public class MembraneBuilder {

	private String type;
	private List<String> tags;
	private Multiset<String> speciesOn;
	private Multiset<String> speciesUnder;
	private Multiset<String> speciesContained;
	private Multiset<MembraneBuilder> membranesContained;
	
	/** An actual membrane, rather than a builder, as this is creating the tree of Membrane objects */
	private Membrane parent;
	
	/**
	 * Create a membrane without specifying a type, because we can't parse the type until after creating the children.
	 */
	public MembraneBuilder() {
		tags = new ArrayList<>();
		speciesOn = LinkedHashMultiset.create();
		speciesUnder = LinkedHashMultiset.create();
		speciesContained = LinkedHashMultiset.create();
		membranesContained = LinkedHashMultiset.create();
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	private void setParent(Membrane parent) {
		this.parent = parent;
	}
	
	public void addTag(String tag) {
		this.tags.add(tag);
	}
	
	public void addSpecies(LocatedSpecies locatedSpecies) {
		switch (locatedSpecies.getLocation()) {
		case on:
			speciesOn.add(locatedSpecies.getSpeciesName());
			break;
		case under:
			speciesUnder.add(locatedSpecies.getSpeciesName());
			break;
		case contained:
			speciesContained.add(locatedSpecies.getSpeciesName());
			break;
		case around:
			// TODO AN: Throw error: Can't add around when building - use "contained parent" instead.
			break;
		default:
			// TODO AN: Throw error: Unknown location
			break;
		}
	}
	
	public void addMembrane(MembraneBuilder childMembrane, int howMany) {
		membranesContained.add(childMembrane, howMany);
	}
	
	public Membrane create() {
		// Construct a membrane object
		Membrane thisMembrane = new Membrane(type, tags, parent);
		// Add species
		for (Entry<String> entry: speciesOn.entrySet()) {
			thisMembrane.addSpecies(Location.on, entry.getElement(), entry.getCount());
		}
		for (Entry<String> entry: speciesUnder.entrySet()) {
			thisMembrane.addSpecies(Location.under, entry.getElement(), entry.getCount());
		}
		for (Entry<String> entry: speciesContained.entrySet()) {
			thisMembrane.addSpecies(Location.contained, entry.getElement(), entry.getCount());
		}
		// Add contained membranes
		for (MembraneBuilder childMembrane: membranesContained) {
			childMembrane.setParent(thisMembrane);
			thisMembrane.addMembrane(childMembrane.create());
		}
		return thisMembrane;
	}
}
