package com.simomics.leishsim.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.model.LocatedSpecies;
import com.simomics.leishsim.model.Location;
import com.simomics.leishsim.model.MembraneModel;
import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.model.StoichiometrySpecies;

import ec.util.MersenneTwisterFast;

/**
 * The dynamic running of the simulation.
 * 
 * Subclass this to implement an algorithm for choosing the next reaction.
 */
public abstract class Simulation {

	/** The model that we are running */
	protected final MembraneModel model;
	
	/** The current state of the membranes and species */
	protected final Membrane currentState;
	
	/** The random number generator used for all random choices */
	protected final MersenneTwisterFast rand;

	/** The queue of events that are scheduled to take place at some point in the future */
	private final PriorityQueue<QueueEvent> eventQueue;
	
	/** The number of seconds for which the simulation has been running */
	private double currentSeconds;
	
	/** 
	 * The number of seconds that have been requested. 
	 * The actual simulation time ({@link #currentSeconds}) will be just larger than {@link #nominalSeconds},
	 * as we keep running reactions until we exceed the requested time.
	 */
	private long nominalSeconds;
	
	/**
	 *  All the membranes in the order that we should print them in the output per membrane file
	 *  @see #speciesNamesToPrint
	 *  @see #locationsToPrint 
	 */
	private List<Membrane> membranesToPrint;
	
	/**
	 *  All the species names in the order that we should print them in the output per membrane file
	 *  @see #membranesToPrint
	 *  @see #locationsToPrint 
	 */
	private List<String> speciesNamesToPrint;
	
	/**
	 *  All the locations of species to print in the order that we should print them in the output per membrane file
     *  @see #membranesToPrint 
	 *  @see #speciesNamesToPrint
	 */
	private List<Location> locationsToPrint;
	
	/** 
	 * Exception thrown if the simulation encountered a problem when running. 
	 */
	@SuppressWarnings("serial")
	public static class InvalidSimulationException extends Exception {
		public InvalidSimulationException(String message, Throwable cause) {
			super(addToMessage(message, cause), cause);
		}
		private static String addToMessage(String message, Throwable cause) {
			message = "Terminating simulation: " + message;
			if (cause != null && cause.getMessage() != null) {
				message += "\n Cause: " + cause.getMessage();
			}
			return message;
		}
	}
	
	/**
	 * An element in the queue of events scheduled for the future;.
	 */
	private abstract class QueueEvent implements Comparable<QueueEvent> {
		
		protected double executionTimeSeconds;
		
		public QueueEvent(double delaySeconds) {
			this.executionTimeSeconds = delaySeconds + currentSeconds;
		}
		
		/**
		 * @return The simulation time (in seconds) at which this event should execute.
		 */
		public double getExecutionTimeSeconds() {
			return executionTimeSeconds;
		}

		/**
		 * Order by execution time.
		 */
		@Override
		public int compareTo(QueueEvent other) {
			return Double.compare(getExecutionTimeSeconds(), other.getExecutionTimeSeconds());
		}
		
		/**
		 * Perform the event.
		 */
		public abstract void execute() throws InvalidSimulationException;
	}
	
	/**
	 * Representation of a collection of species to add to the simulation at some point in the future.
	 * Used to fire a reaction after a specified delay time.
	 */
	private class AddProductsEvent extends QueueEvent {

		private final Set<StoichiometrySpecies> speciesToAdd;
		private final Membrane membrane;
		private final Reaction reaction;
		
		public AddProductsEvent(double delaySeconds, Set<StoichiometrySpecies> speciesToAdd, Membrane membrane, Reaction reaction) {
			super(delaySeconds);
			this.speciesToAdd = speciesToAdd;
			this.membrane = membrane;
			this.reaction = reaction;
		}

		@Override
		public void execute() throws InvalidSimulationException {
			for (StoichiometrySpecies stoichSpecies: speciesToAdd) {
				if (stoichSpecies.getStochiometry() != 0) {
					membrane.addSpecies(stoichSpecies, stoichSpecies.getStochiometry());
				}
			}
			Simulation.this.recalculatePropensitiesFrom(reaction, membrane);
		}
		
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("At time %f, Add products: (", executionTimeSeconds));
			String separator = "";
			for (StoichiometrySpecies stoichSpecies: speciesToAdd) {
				result.append(separator);
				separator = ", ";
				result.append(stoichSpecies);
			}
			result.append(String.format(") to membrane: %s", membrane.getType()));
			return result.toString();
		}
	}
	
	/**
	 * Used for printing out propensities files.
	 * @return The propensity of this reaction given the current state of the simulation.
	 */
	protected abstract double getReactionPropensity(Reaction reaction);
	
	/**
	 * Create a new simulation, set up with the given initial conditions.
	 * @param model
	 * @param initialConditions
	 * @param randomSeed can be null, to not set the seed
	 * @throws InvalidSimulationException If the simulation could not be created.
	 */
	public Simulation(MembraneModel model, Membrane initialConditions, Integer randomSeed) throws InvalidSimulationException {
		this.model = model;
		this.currentState = initialConditions;
		
		if (randomSeed == null) {
			randomSeed = Math.abs(new MersenneTwisterFast().nextInt());
		}
		Logging.getLogger(LogType.PROGRESS).log("Random seed is " + randomSeed);
		this.rand = new MersenneTwisterFast(randomSeed);
		
		this.eventQueue = new PriorityQueue<>();
	}
	
	/**
	 * @return The model that this simulation is running.
	 */
	public MembraneModel getModel() {
		return model;
	}
	
	/**
	 * @return The current membranes and species in the simulation.
	 */
	public Membrane getCurrentState() {
		return currentState;
	}
	
	public double getCurrentSeconds() {
		return currentSeconds;
	}
	
	public double getCurrentHours() {
		return currentSeconds / (60 * 60);
	}
	
	/**
	 * @return The current state of the membranes and species, as a human-readable string.
	 */
	public String printCurrentState() {
		return currentState.toString();
	}
	
	/**
	 * Run the simulation for a given length of time.
	 * @param runHours true if we should print out time in hours, or false for seconds.
	 * @return true if the simulation finished early, or false if the requested number of seconds were run.
	 * @throws InvalidSimulationException If anything went wrong when running the simulation.
	 */
	public boolean runSeconds(long numSeconds, long secondsBeforePrint, List<String> namesToPrint, boolean runHours) throws InvalidSimulationException {	
		if (Logging.isLoggable(LogType.DETAIL)) {
			Logging.getLogger(LogType.DETAIL).log(String.format("Seconds: %.3f", currentSeconds));
			Logging.getLogger(LogType.DETAIL).log("\n" + currentState.toString());
		}
		
		// Print out headings for the output
		printHeadings(namesToPrint, runHours);

		// Run the simulation
		currentSeconds = 0f;
		nominalSeconds = 0;
		printSpecies(namesToPrint, runHours); // print out time 0
		boolean stoppedEarly = false;
		
		Logging.getLogger(LogType.PROGRESS).log(String.format("Running simulation for %d seconds", numSeconds));
		
		// Run in chunks of how often we are printing
		int numUpdates = (int) Math.ceil((double)numSeconds / secondsBeforePrint);
		for (int i = 0; i < numUpdates; i++) {
			// Run one chunk
			stoppedEarly = runForSeconds(secondsBeforePrint);
			
			if (Logging.isLoggable(LogType.DETAIL)) {
				Logging.getLogger(LogType.DETAIL).log(String.format("Seconds: %.3f", currentSeconds));
				Logging.getLogger(LogType.DETAIL).log("\n" + currentState.toString());
			}
			
			// print out species at time n
			printSpecies(namesToPrint, runHours);
			if (stoppedEarly) {
				break;
			}
			
			long secondsRunSoFar = (i + 1) * secondsBeforePrint;
			Logging.getLogger(LogType.PROGRESS).log(String.format("Run %d of %d seconds (%.1f%%)", 
					secondsRunSoFar, numSeconds, 100f * secondsRunSoFar / numSeconds));
		}
		
		return stoppedEarly;
	}
	
	/**
	 * Print out the headings for all the requested places.
	 */
	private void printHeadings(List<String> namesToPrint, boolean runHours) {
		if (Logging.isLoggable(LogType.PRINTED_SPECIES)) {
			printHeadingsAggregated(namesToPrint, runHours);
		}
		if (Logging.isLoggable(LogType.PRINTED_SPECIES_PER_MEMBRANE)) {
			printHeadingsPerMembrane(namesToPrint, runHours);
		}
		if (Logging.isLoggable(LogType.PRINTED_PROPENSITIES)) {
			printHeadingsPropensities(runHours);
		}
	}
	
	/**
	 * Print out the values of all the requested species.
	 */
	private void printSpecies(List<String> namesToPrint, boolean runHours) {
		if (Logging.isLoggable(LogType.PRINTED_SPECIES)) {
			printSpeciesAggregated(namesToPrint, runHours);
		}
		if (Logging.isLoggable(LogType.PRINTED_SPECIES_PER_MEMBRANE)) {
			printSpeciesOneMembrane(namesToPrint, runHours);
		}
		if (Logging.isLoggable(LogType.PRINTED_PROPENSITIES)) {
			printPropensities(runHours);
		}
	}
	
	/**
	 * Print the headings for the aggregated species file.
	 */
	private void printHeadingsAggregated(List<String> namesToPrint, boolean runHours) {
		StringBuilder headingLine = new StringBuilder();
		for (String placeName: namesToPrint) {
			headingLine.append(placeName);
			headingLine.append(",");
		}
		if (runHours) {
			headingLine.append("Hours");	
		}
		else {
			headingLine.append("Seconds");
		}
		Logging.getLogger(LogType.PRINTED_SPECIES).log(headingLine.toString());
	}
	
	/**
	 * Print the species concentrations for the aggregated species file.
	 */
	private void printSpeciesAggregated(List<String> namesToPrint, boolean runHours) {
		StringBuilder speciesLine = new StringBuilder();
		for (String placeName: namesToPrint) {
			speciesLine.append(currentState.getAllSpecies(placeName));
			speciesLine.append(",");
		}
		if (runHours) {
			speciesLine.append(nominalSeconds / (60 * 60));
		}
		else {
			speciesLine.append(nominalSeconds);
		}
		Logging.getLogger(LogType.PRINTED_SPECIES).log(speciesLine.toString());
	}
	
	/**
	 * Print the headings for the species file per membrane.
	 */
	private void printHeadingsPerMembrane(List<String> namesToPrint, boolean runHours) {
		StringBuilder headingLine = new StringBuilder();
		if (runHours) {
			headingLine.append("Hours");	
		}
		else {
			headingLine.append("Seconds");
		}
		Map<String, Set<LocatedSpecies>> speciesPerMembrane = computeSpeciesPerMembrane();
		membranesToPrint = new ArrayList<>();
		speciesNamesToPrint = new ArrayList<>();
		locationsToPrint = new ArrayList<>();
		printHeadingsOneMembrane(currentState, speciesPerMembrane, headingLine);
		Logging.getLogger(LogType.PRINTED_SPECIES_PER_MEMBRANE).log(headingLine.toString());
	}
	
	/**
	 * Accumulate the headings from this membrane and all its descendants into the given StringBuilder.
	 */
	private void printHeadingsOneMembrane(Membrane membrane, Map<String, Set<LocatedSpecies>> speciesPerMembrane, StringBuilder output) {
		Set<LocatedSpecies> species = speciesPerMembrane.get(membrane.getType());
		if (species != null) {
			for (LocatedSpecies locatedSpecies: species) {
				output
				  .append(",")
				  .append(locatedSpecies.getSpeciesName())
				  .append(" ")
				  .append(locatedSpecies.getLocation())
				  .append(" ")
				  .append(membrane.getUniqueTag());
				membranesToPrint.add(membrane);
				speciesNamesToPrint.add(locatedSpecies.getSpeciesName());
				locationsToPrint.add(locatedSpecies.getLocation());
			}
		}
		for (Membrane childMembrane: membrane.getContainedMembranes()) {
			printHeadingsOneMembrane(childMembrane, speciesPerMembrane, output);
		}
	}

	/**
	 * Print the species concentrations for the species file per membrane.
	 */
	private void printSpeciesOneMembrane(List<String> namesToPrint, boolean runHours) {
		StringBuilder speciesLine = new StringBuilder();
		if (runHours) {
			speciesLine.append(nominalSeconds / (60 * 60));
		}
		else {
			speciesLine.append(nominalSeconds);
		}
		for (int i = 0; i < membranesToPrint.size(); i++) {
			speciesLine.append(",");
			speciesLine.append(membranesToPrint.get(i).numSpecies(speciesNamesToPrint.get(i), locationsToPrint.get(i)));
		}
		Logging.getLogger(LogType.PRINTED_SPECIES_PER_MEMBRANE).log(speciesLine.toString());
	}
	
	/**
	 * @return All the locations that species could be, for each membrane type. Map key is a membrane type.
	 */
	private Map<String, Set<LocatedSpecies>> computeSpeciesPerMembrane() {
		Map<String, Set<LocatedSpecies>> results = new HashMap<>();
		// Add species defined in initial conditions
		for (LocatedSpecies locatedSpecies: model.getInitialSpecies().elementSet()) {
			for (LocatedSpecies resolvedLocatedSpecies: resolveAroundSpecies(locatedSpecies)) {
				Set<LocatedSpecies> species = results.get(resolvedLocatedSpecies.getMembraneName());
				if (species == null) {
					species = new TreeSet<>();
					results.put(resolvedLocatedSpecies.getMembraneName(), species);
				}
				species.add(resolvedLocatedSpecies);
			}
		}
		// Add species inferred from reactions
		for (Reaction reaction: getModel().getReactions()) {
			for (LocatedSpecies locatedSpecies: (Iterable<LocatedSpecies>)reaction.streamInvolvedLocatedSpecies()::iterator) {
				for (LocatedSpecies resolvedLocatedSpecies: resolveAroundSpecies(locatedSpecies)) {
					Set<LocatedSpecies> species = results.get(resolvedLocatedSpecies.getMembraneName());
					if (species == null) {
						species = new TreeSet<>();
						results.put(resolvedLocatedSpecies.getMembraneName(), species);
					}
					species.add(resolvedLocatedSpecies);
				}
			}
		}
		return results;
	}
	
	/**
	 * Transforms "around" species into "contained" species for the parent membrane.
	 * If the initial tree implies that multiple types of membrane could be the parent, then all are returned.
	 * @param locatedSpecies A species that could be an "around" species.
	 * @return A collection of species that are not "around" species.
	 */
	private Collection<LocatedSpecies> resolveAroundSpecies(LocatedSpecies locatedSpecies) {
		List<LocatedSpecies> results = new ArrayList<>();
		if (locatedSpecies.getLocation() != Location.around) {
			results.add(locatedSpecies); 
		}
		else {
			for (Membrane membrane: currentState.getMatchingMembranes(locatedSpecies.getMembraneName(), Location.around)) {
				results.add(new LocatedSpecies(locatedSpecies.getSpeciesName(), Location.contained, membrane.getType()));
			}
		}
		return results;
	}
	
	/**
	 * Print the headings for the reaction propensities output file.
	 */
	private void printHeadingsPropensities(boolean runHours) {
		StringBuilder headingLine = new StringBuilder();
		if (runHours) {
			headingLine.append("Hours");	
		}
		else {
			headingLine.append("Seconds");
		}
		for (int i = 0; i < model.getNumReactions(); i++) {
			headingLine.append(",");
			headingLine.append("reaction" + i);
		}
		Logging.getLogger(LogType.PRINTED_PROPENSITIES).log(headingLine.toString());
	}
	
	/**
	 * Print the propensities of each reaction to the output file.
	 */
	private void printPropensities(boolean runHours) {
		StringBuilder propensitiesLine = new StringBuilder();
		if (runHours) {
			propensitiesLine.append(nominalSeconds / (60 * 60));
		}
		else {
			propensitiesLine.append(nominalSeconds);
		}
		for (Reaction reaction: model.getReactions()) {
			propensitiesLine.append(",");
			propensitiesLine.append(getReactionPropensity(reaction));
		}
		Logging.getLogger(LogType.PRINTED_PROPENSITIES).log(propensitiesLine.toString());
	}
	
	/**
	 * Run a given number of seconds of the simulation.
	 * @param numSeconds
	 * @return false if we reached the time, or true if we ran out of viable reactions before reaching the number of seconds.
	 * @throws InvalidSimulationException If anything went wrong when running the simulation.
	 */
	public boolean runForSeconds(long numSeconds) throws InvalidSimulationException {
		nominalSeconds += numSeconds;
		while (currentSeconds < nominalSeconds) {
			Double secondsElapsed = step();
			if (secondsElapsed == null) {
				Logging.getLogger(LogType.DETAIL).log(String.format("No more viable reactions. Stopping simulation early at time %s", currentSeconds));
				return true;
			}
			currentSeconds += secondsElapsed;
			
			if (Logging.isLoggable(LogType.FULL)) {
				Logging.getLogger(LogType.FULL).log("Current time: " + currentSeconds);
				Logging.getLogger(LogType.FULL).log("Current state:\n" + currentState.toString());
			}
		}
		return false;
	}

	/**
	 * Run one step of the simulation.
	 * @return the number of seconds elapsed during this step, or null if no more reactions are viable
	 * @throws InvalidSimulationException If anything went wrong when running the simulation.
	 */
	private Double step() throws InvalidSimulationException {
		// Choose a random reaction to fire
		ChosenReaction chosenReaction = chooseRandomReaction();

		// See if we should execute the reaction or the next event on the queue
		if ((chosenReaction != null) && 
				(eventQueue.isEmpty() ||
						currentSeconds + chosenReaction.getSecondsToFiring() < eventQueue.peek().getExecutionTimeSeconds())) {
			// Execute the reaction
			Membrane membrane = chosenReaction.getLocation();
			Reaction reaction = chosenReaction.getReaction();
			// Remove reactants immediately
			for (StoichiometrySpecies stoichSpecies: reaction.getConsumedSpecies()) {
				if (stoichSpecies.getStochiometry() != 0) {
					membrane.addSpecies(stoichSpecies, -stoichSpecies.getStochiometry());
				}
			}
			// If this reaction has a delay then add its products to the queue, otherwise add them immediately
			double reactionDelaySeconds = reaction.getDelaySeconds();
			if (reactionDelaySeconds > 0) {
				eventQueue.add(new AddProductsEvent(reactionDelaySeconds, reaction.getProducedSpecies(), membrane, reaction));
			}
			else {
				for (StoichiometrySpecies stoichSpecies: reaction.getProducedSpecies()) {
					if (stoichSpecies.getStochiometry() != 0) {
						membrane.addSpecies(stoichSpecies, stoichSpecies.getStochiometry());
					}
				}
			}
			
			if (Logging.isLoggable(LogType.FULL)) {
				Logging.getLogger(LogType.FULL).log(String.format("Fired reaction: %s in membrane: %s\n", 
						reaction, chosenReaction.getLocation().getType()));
			}
			
			return chosenReaction.getSecondsToFiring();
		}
		else if (!eventQueue.isEmpty()) {
			// Execute the event at the top of the queue
			QueueEvent event = eventQueue.remove();
			event.execute();
			
			if (Logging.isLoggable(LogType.FULL)) {
				Logging.getLogger(LogType.FULL).log(String.format("Executed queue event: %s\n", event));
			}
			
			return event.getExecutionTimeSeconds() - currentSeconds;
		}
		else {
			// No reaction to fire and nothing on the queue
			return null;
		}
	}
	
	/**
	 * Rolls all the random numbers needed for one step. 
	 * Chooses which reaction fires next, in which location and at what time.
	 * @return The next reaction to fire, or null if no reactions are viable.
	 */
	protected abstract ChosenReaction chooseRandomReaction() throws InvalidSimulationException;
	
	/**
	 * Recalculates the reaction propensities, as if the given reaction had just fired in the given membrane.
	 * @param reaction 
	 * @param membrane
	 */
	protected abstract void recalculatePropensitiesFrom(Reaction reaction, Membrane membrane) throws InvalidSimulationException;
}
