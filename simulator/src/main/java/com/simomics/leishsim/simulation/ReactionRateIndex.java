package com.simomics.leishsim.simulation;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * An index into the reactionRates table.
 * Used to list all the non-zero table entries to speed up choosing a random reaction.
 * 
 * Not used at the moment, because I couldn't make using it more efficient than just iterating through the whole table. 
 */
public class ReactionRateIndex {
	private final int reactionId;
	private final int membraneId;
	
	public ReactionRateIndex(int reactionId, int membraneId) {
		this.reactionId = reactionId;
		this.membraneId = membraneId;
	}
	
	public int getReactionId() {
		return reactionId;
	}
	
	public int getMembraneId() {
		return membraneId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ReactionRateIndex) {
			return reactionId == ((ReactionRateIndex) obj).reactionId &&
					membraneId == ((ReactionRateIndex) obj).membraneId;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append(reactionId)
				.append(membraneId)
				.toHashCode();
	}
}
