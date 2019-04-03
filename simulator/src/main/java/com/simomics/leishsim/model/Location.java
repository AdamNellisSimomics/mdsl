package com.simomics.leishsim.model;

/**
 * A location in which a reaction can happen (relative to a membrane).
 */
public enum Location {
	
	/** Attached to the membrane, pointing outwards. */
	on,
	
	/** Attached to the membrane, pointing inwards. */
	under,
	
	/** Free-floating, inside the membrane. */
	contained,
	
	/** Free-floating, contained within the membrane. */
	around
}
