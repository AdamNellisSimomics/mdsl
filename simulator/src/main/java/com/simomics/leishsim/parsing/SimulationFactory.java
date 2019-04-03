package com.simomics.leishsim.parsing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.Interval;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.model.LocatedSpecies;
import com.simomics.leishsim.model.Location;
import com.simomics.leishsim.model.MembraneModel;
import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.model.Reaction.InvalidReactionException;
import com.simomics.leishsim.model.StoichiometrySpecies;
import com.simomics.leishsim.model.equations.AddExpression;
import com.simomics.leishsim.model.equations.DivideExpression;
import com.simomics.leishsim.model.equations.Equation;
import com.simomics.leishsim.model.equations.Equation.InvalidEquationEvaluationException;
import com.simomics.leishsim.model.equations.LocatedSpeciesExpression;
import com.simomics.leishsim.model.equations.MultiplyExpression;
import com.simomics.leishsim.model.equations.NaturalLogExpression;
import com.simomics.leishsim.model.equations.NumberExpression;
import com.simomics.leishsim.model.equations.OperationExpression;
import com.simomics.leishsim.model.equations.ParameterExpression;
import com.simomics.leishsim.model.equations.RoundExpression;
import com.simomics.leishsim.model.equations.SubtractExpression;
import com.simomics.leishsim.model.equations.UnaryOperationExpression;
import com.simomics.leishsim.simulation.Membrane;
import com.simomics.leishsim.simulation.ReactionRateTableSimulation;
import com.simomics.leishsim.simulation.Simulation;
import com.simomics.leishsim.simulation.Simulation.InvalidSimulationException;

/**
 * Creates MembraneModel objects from *.mdsl files.
 */
public class SimulationFactory {

	/** Species explicitly listed in the species section */
	private final Set<LocatedSpecies> speciesLocationsListed = new LinkedHashSet<>();
	private final Set<String> allSpeciesNames = new LinkedHashSet<>();
	
	/** Species used in reactions */
	private final Set<LocatedSpecies> speciesLocationsInferred = new LinkedHashSet<>();
	
	/** Membrane names used in reactions. We don't have a membrane section yet... */
	private final Set<String> membraneNamesInferred = new LinkedHashSet<>();
	
	/** The equations of parameters explicitly listed in the parameters section */
	private final Map<String, Equation> parameterEquations = new LinkedHashMap<>();
	
	/** The values of parameters explicitly listed in the parameters section */
	private final Map<String, Double> parameterValues = new LinkedHashMap<>();
	
	/** The units of parameters explicitly listed in the parameters section */
	private final Map<String, String> parameterUnits = new LinkedHashMap<>();
	
	/** Parameters used in reactions and equations */
	private final Set<String> parameterNamesInferred = new LinkedHashSet<>();
	
	/** All the parsed reactions in the file */
	private final Set<Reaction> reactions = new LinkedHashSet<>();
	
	/** The initial tree of membranes and species */
	private Membrane initialConditions;
	
	/** The initialisation data for each species */
	private final Multiset<LocatedSpecies> initialSpecies = LinkedHashMultiset.create();
	
	/** Map of species name to the tags applied to that species name. Inverse of {@link #tagsToSpecies} */
	private final Multimap<String, String> speciesToTags = HashMultimap.create();
	
	/** Map of tag to the species with that tag. Inverse of {@link #speciesToTags} */
	private final Multimap<String, String> tagsToSpecies = HashMultimap.create();
	
	/** True if there was a model error when running the parser, or false if not. */
	private List<String> parserErrors;
	
	/** Exception thrown when the model could not be created. */
	@SuppressWarnings("serial")
	public static class InvalidModelException extends Exception {
		public InvalidModelException(String message, Throwable cause) {
			super(message, cause);
		}
	}
	
	/**
	 * Parses the given file and creates a simulation from its contents.
	 * @param file
	 * @param randomSeed The seed for the simlator's random number generator. Can be null to not set the seed.
	 * @return The parsed model.
	 */
	public Simulation createSimulation(File file, Integer randomSeed) throws InvalidModelException {
		Logging.getLogger(LogType.PROGRESS).log("Reading model file: " + file.getAbsolutePath());
		try {
			return createSimulation(new FileInputStream(file), randomSeed);
		} catch (FileNotFoundException e) {
			throw new InvalidModelException("Could not find file: " + file, e);
		}
	}
	
	/**
	 * Reads from the given input stream and creates a model from its data.
	 * @param input
	 * @param randomSeed The seed for the simlator's random number generator. Can be null to not set the seed.
	 * @return The parsed model.
	 */
	public Simulation createSimulation(InputStream input, Integer randomSeed) throws InvalidModelException {
		// Create the parser
		MembraneModelParser parser = createParser(input);
	    // Run the parser
		runParser(parser);
		// Transform the parsed syntax tree into a Java object
		Simulation simulation;
		try {
			simulation = constructSimulation(randomSeed);
		} catch (InvalidSimulationException e) {
			throw new InvalidModelException("Could not create simulation", e);
		}
	    return simulation;
	}

	/**
	 * Creates the parser 
	 * @param input The contents of the input file
	 * @return The parser object (not yet run).
	 * @throws IOException If there were problems reading from the input stream.
	 */
	private MembraneModelParser createParser(InputStream input) throws InvalidModelException {
		MembraneModelLexer lexer;
		try {
			lexer = new MembraneModelLexer(new ANTLRInputStream(input));
		} catch (IOException e) {
			throw new InvalidModelException("Could not read input stream", e);
		}
		MembraneModelParser parser = new MembraneModelParser(new CommonTokenStream(lexer));
	    parser.addErrorListener(new BaseErrorListener() {
	        @Override
	        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
	            throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
	        }
	    });
	    
	    if (Logging.isLoggable(LogType.FULL)) {
	    	// Print out which rules are being matched
	    	parser.setTrace(true);
	    }
	    
	    parser.addParseListener(new MembraneModelBaseListener() {
	    	/** The reaction that is currently being parsed */
			private ReactionBuilder currentReaction;
			
			/** The current path through the membrane initialisation tree */
			private Stack<MembraneBuilder> currentMembraneTree;
			
			/** The current path through the current equation */
			private Stack<Equation> currentEquation = new Stack<>();
			
			/** The current location context that we are in */
			private Location contextLocation;
			private String contextMembrane;

			@Override
			public void enterInitialTreeDefinition(MembraneModelParser.InitialTreeDefinitionContext ctx) {
				currentMembraneTree = new Stack<>();
				// Create a new membrane for the whole simulation
				currentMembraneTree.push(new MembraneBuilder());
			}
			
			@Override
			public void enterInitialMembraneDefinition(MembraneModelParser.InitialMembraneDefinitionContext ctx) {
				// Add single new membrane to the tree, as a placeholder for the actual number
				MembraneBuilder parentMembrane = currentMembraneTree.peek();
				MembraneBuilder thisMembrane = new MembraneBuilder();
				parentMembrane.addMembrane(thisMembrane, 1);
				currentMembraneTree.push(thisMembrane);
			}
			
			@Override
			public void exitInitialMembraneDefinition(MembraneModelParser.InitialMembraneDefinitionContext ctx) {
				// Add the correct number of new membranes to the tree (minus the one we added as a placeholder)
				MembraneBuilder thisMembrane = currentMembraneTree.pop();
				MembraneBuilder parentMembrane = currentMembraneTree.peek();
				thisMembrane.setType(ctx.membraneName.getText());
				int howMany = 1; // default if not set
				if (ctx.howMany != null) {
					howMany = Integer.parseInt(ctx.howMany.getText());
				}
				// Record any tags
				if (ctx.tagsList != null) {
					for (MembraneModelParser.TagContext tagCtx: ctx.tagsList.tag()) {
						String tagValue = tagCtx.value.getText();
						// Remove any surrounding quote marks
						if (tagValue.startsWith("'") && tagValue.endsWith("'")) {
							tagValue = tagValue.substring(1, tagValue.length() - 1);
						}
						thisMembrane.addTag(tagValue);
					}
				}
				parentMembrane.addMembrane(thisMembrane, howMany - 1);
			}
			
			@Override
			public void exitInitialTreeDefinition(MembraneModelParser.InitialTreeDefinitionContext ctx) {
				// Check that we have exactly one membrane in the tree
				if (currentMembraneTree.isEmpty()) {
					Logging.getLogger(LogType.WARNING).log("No initial membrane tree specified");
					return;
				}
				else if (currentMembraneTree.size() > 1) {
					Logging.getLogger(LogType.ERROR).log("Invalid membrane tree specified");
					return;
				}
				// Create the initial tree
				MembraneBuilder simulationMembrane = currentMembraneTree.pop();
				simulationMembrane.setType(ctx.simulationName.getText());
				// Record any tags
				if (ctx.tagsList != null) {
					for (MembraneModelParser.TagContext tagCtx: ctx.tagsList.tag()) {
						String tagValue = tagCtx.value.getText();
						// Remove any surrounding quote marks
						if (tagValue.startsWith("'") && tagValue.endsWith("'")) {
							tagValue = tagValue.substring(1, tagValue.length() - 1);
						}
						simulationMembrane.addTag(tagValue);
					}
				}
				Membrane.resetUniqueTags(); // start unique tag numbering from the beginning
				initialConditions = simulationMembrane.create();
			}
			
			@Override
			public void exitLocationContextBlockStart(MembraneModelParser.LocationContextBlockStartContext ctx) {
				if (this.contextLocation != null || this.contextMembrane != null) {
					parserErrors.add(String.format("Line %d: Cannot nest location blocks", ctx.start.getLine()));
					return;
				}
				this.contextLocation = Location.valueOf(ctx.location.getText());
				this.contextMembrane = ctx.membrane.getText();
			}
			
			@Override
			public void exitLocationContextBlock(MembraneModelParser.LocationContextBlockContext ctx) {
				this.contextLocation = null;
				this.contextMembrane = null;
			}
			
			@Override
			public void exitSpeciesDefinition(MembraneModelParser.SpeciesDefinitionContext ctx) {
				// Get location of species from either the definition or the current location block
				Location location = null;
				String membrane = null;
				if (ctx.location == null && ctx.membrane == null) {
					location = this.contextLocation;
					membrane = this.contextMembrane;
				}
				else {
					if (ctx.location != null) {
						location = Location.valueOf(ctx.location.getText());
					}
					if (ctx.membrane != null) {
						membrane = ctx.membrane.getText();
					}
				}
				
				// Check that we have all the required fields
				boolean missingFields = false;
				if (location == null) {
					parserErrors.add(String.format("Line %d: Species definition is missing location: %s", ctx.start.getLine(), getOriginalText(ctx)));
					missingFields = true;
				}
				if (membrane == null) {
					parserErrors.add(String.format("Line %d: Species definition is missing membrane: %s", ctx.start.getLine(), getOriginalText(ctx)));
					missingFields = true;
				}
				if (ctx.howMany == null) {
					parserErrors.add(String.format("Line %d: Species definition is missing initialisation: %s", ctx.start.getLine(), getOriginalText(ctx)));
					missingFields = true;
				}
				if (missingFields) {
					return;
				}
				String speciesName = ctx.name.getText();
				LocatedSpecies locatedSpecies = new LocatedSpecies(speciesName, location, membrane);
				
				// Capture species given in the file
				speciesLocationsListed.add(locatedSpecies);
				allSpeciesNames.add(speciesName);
				
				// Add initial conditions to membrane tree
				Integer numSpecies = simplifyEquationToInteger(currentEquation.pop());
				if (numSpecies == null) {
					parserErrors.add(String.format("Line %d: Could not evaluate species initialisation equation: %s", ctx.start.getLine(), getOriginalText(ctx)));
					return;
				}
				int numBefore = initialSpecies.add(locatedSpecies, numSpecies.intValue());
				if (numBefore != 0) {
					parserErrors.add(String.format("Line %d: Duplicate species definition: %s", ctx.start.getLine(), getOriginalText(ctx)));
					return;
				}
				
				// Record any tags
				if (ctx.tagsList != null) {
					for (MembraneModelParser.TagContext tagCtx: ctx.tagsList.tag()) {
						String tagValue = tagCtx.value.getText();
						// Remove any surrounding quote marks
						if (tagValue.startsWith("'") && tagValue.endsWith("'")) {
							tagValue = tagValue.substring(1, tagValue.length() - 1);
						}
						speciesToTags.put(speciesName, tagValue);
						tagsToSpecies.put(tagValue, speciesName);
					}
				}
			}

			@Override
			public void exitParameterDefinition(MembraneModelParser.ParameterDefinitionContext ctx) {
				// Evaluate equation for parameter value
				if (currentEquation.size() != 1) {
					Logging.getLogger(LogType.ERROR).log(String.format("Line %d: Failed to parse equation for parameter", ctx.start.getLine()));
					return;
				}
				Equation parameterEquation = currentEquation.pop();
				Double parameterValue = simplifyEquationToDouble(parameterEquation);
				// To be a valid equation, it must simplify to a number
				if (parameterValue == null) {
					Logging.getLogger(LogType.ERROR).log(String.format("Line %d: Failed to evaluate equation for parameter", ctx.start.getLine()));
					return;
				}
				String parameterName = ctx.name.getText();
				parameterValues.put(parameterName, parameterValue);
				parameterEquations.put(parameterName, parameterEquation);
				parameterUnits.put(parameterName, ctx.parameterUnits.getText());
			}
			
			@Override
			public void enterReactionDefinition(MembraneModelParser.ReactionDefinitionContext ctx) {
				// Start creating a new reaction object 
				currentReaction = new ReactionBuilder();
			}

			@Override
			public void exitStoichiometrySpecies(MembraneModelParser.StoichiometrySpeciesContext ctx) {
				if (ctx.locSpecies == null) {
					parserErrors.add(String.format("Line %d: Failed to parse species with stoichiometry: %s", ctx.start.getLine(), getOriginalText(ctx)));
					return;
				}
				
				// Get location of species from either the definition or the current location block
				Location speciesLocation = null;
				String membraneName = null;
				if (ctx.locSpecies.location == null && ctx.locSpecies.membrane == null) {
					speciesLocation = this.contextLocation;
					membraneName = this.contextMembrane;
				}
				else {
					if (ctx.locSpecies.location != null) {
						speciesLocation = Location.valueOf(ctx.locSpecies.location.getText());
					}
					if (ctx.locSpecies.membrane != null) {
						membraneName = ctx.locSpecies.membrane.getText();
					}
				}
				
				if (speciesLocation == null || membraneName == null) {
					parserErrors.add(String.format("Line %d: Species is missing a location: %s", ctx.start.getLine(), getOriginalText(ctx)));
					return;
				}
				
				String speciesName = ctx.locSpecies.species.getText();

				// Infer names of species and membranes from reactions
				speciesLocationsInferred.add(new LocatedSpecies(speciesName, speciesLocation, membraneName));
				allSpeciesNames.add(speciesName);
				membraneNamesInferred.add(membraneName);
				
				// Add locatedSpecies to the current reaction
				int stoichiometryInt = 1;
				if (ctx.stoich != null) {
					String stoichiometryString = ctx.stoich.getText();
					if (stoichiometryString.equals("gene") || stoichiometryString.equals("catalyst")) {
						stoichiometryInt = 0;
					}
					else {
						stoichiometryInt = Integer.valueOf(stoichiometryString);
					}
				}
				StoichiometrySpecies stoichiometrySpecies = new StoichiometrySpecies(stoichiometryInt, speciesName, speciesLocation, membraneName);
				currentReaction.addSpecies(stoichiometrySpecies);
			}

			@Override
			public void exitArrow(MembraneModelParser.ArrowContext ctx) {
				currentReaction.switchToRightHandSide();
			}

			@Override
			public void exitReactionDefinition(MembraneModelParser.ReactionDefinitionContext ctx) {
				Equation forwardRateModifier = null;
				Equation reverseRateModifier = null;
				int reactionLineNumber = ctx.start.getLine();
				
				// Check we have parsed enough equations
				int expectedNumEquations = 1 + (ctx.forwardDelay != null? 1 : 0) + (ctx.reverseDelay != null ? 1 : 0) + (ctx.reverseModifier != null ? 1 : 0);
				if (currentEquation.size() < expectedNumEquations) {
					Logging.getLogger(LogType.ERROR).log(String.format("Line %d: Not enough equations for reaction modifiers/delays. Expected %d, but found %d", reactionLineNumber, expectedNumEquations, currentEquation.size()));
					return;
				}
				else if (currentEquation.size() > expectedNumEquations) {
					Logging.getLogger(LogType.ERROR).log(String.format("Line %d: Too many equations for reaction modifiers/delays. Expected %d, but found %d", reactionLineNumber, expectedNumEquations, currentEquation.size()));
					return;
				}
				
				// Pull out modifier and delay equations - these are in reverse order on the currentEquation stack
				double forwardDelayHours = 0;
				double reverseDelayHours = 0;
				if (ctx.reverseModifier != null) {
					// Reaction has a reverse reaction as well 
					if (ctx.reverseDelay != null) {
						reverseDelayHours = simplifyEquationToDouble(currentEquation.pop());
					}
					reverseRateModifier = currentEquation.pop();
				}
				if (ctx.forwardDelay != null) {
					forwardDelayHours = simplifyEquationToDouble(currentEquation.pop());
				}
				forwardRateModifier = currentEquation.pop();
				
				// Finish creating this reaction object
				String mdslLine = getOriginalText(ctx);
				currentReaction.setMdslLine(mdslLine, reactionLineNumber);
				currentReaction.setRateModifier(forwardRateModifier, simplifyEquation(forwardRateModifier));
				currentReaction.setDelayHours(forwardDelayHours);
				Reaction forwardReaction;
				try {
					forwardReaction = currentReaction.create();
					reactions.add(forwardReaction);
				} catch (InvalidReactionException e) {
					parserErrors.add(String.format("Line %d: %s", reactionLineNumber, e.getMessage()));
					return;
				}
				
				// Duplicate reversible reactions
				if (ctx.reactionType != null && ctx.reactionType.getText().equals("<=>")) {
					if (ctx.reverseModifier == null) {
						// No rate modifier for reverse reaction
						Logging.getLogger(LogType.WARNING).log(String.format("Line %d: No reverse rate modifier specified for reversible reaction: %s", reactionLineNumber, mdslLine));
					}
					
					else {
						try {
							Reaction reverseReaction = new Reaction(forwardReaction.getProducedSpecies(), forwardReaction.getConsumedSpecies(), 
									reverseRateModifier, simplifyEquation(reverseRateModifier), reverseDelayHours, mdslLine, reactionLineNumber);
							reactions.add(reverseReaction);
						} catch (InvalidReactionException e) {
							parserErrors.add(String.format("Line %d: %s", reactionLineNumber, e.getMessage()));
							return;
						}
						
					}
				}
				
				
			}
			
			@Override
			public void exitNumberExpression(MembraneModelParser.NumberExpressionContext ctx) {
				currentEquation.push(new NumberExpression(Double.parseDouble(ctx.number.getText())));
			}
			
			@Override
			public void exitParameterOrSpeciesExpression(MembraneModelParser.ParameterOrSpeciesExpressionContext ctx) {
				String itemName = ctx.parameterOrSpecies.getText();
				
				if (allSpeciesNames.contains(itemName)) {
					// This is the name of a species - so to be valid we must be within a location block
					if (this.contextLocation != null && this.contextMembrane != null) {
						// Inside a location block
						LocatedSpecies locatedSpecies = new LocatedSpecies(itemName, this.contextLocation, this.contextMembrane);
						speciesLocationsInferred.add(locatedSpecies);
						currentEquation.push(new LocatedSpeciesExpression(locatedSpecies));						
					}
					else {
						// Outside of a location block
						parserErrors.add(String.format("Line: %d Species name used in equation without a location: %s", 
								ctx.getStart().getLine(), itemName));
						return;
					}
				}
				else {
					// This name hasn't been used previously as a species, so assume it is a parameter
					// Infer names of parameters from reactions
					parameterNamesInferred.add(itemName);
					currentEquation.push(new ParameterExpression(itemName));					
				}
			}
			
			@Override
			public void exitOperationExpression(MembraneModelParser.OperationExpressionContext ctx) {
				// Check that we managed to parse the operands
				if (currentEquation.size() < 2) {
					Logging.getLogger(LogType.ERROR).log(String.format("Line: %d Error parsing equation", ctx.getStart().getLine()));
					return;
				}
				Equation right = currentEquation.pop();
				Equation left = currentEquation.pop();
				String operation = ctx.operation.getText();
				Equation equation;
				switch (operation.charAt(0)) {
				case '+':
					equation = new AddExpression(left, right);
					break;
				case '-':
					equation = new SubtractExpression(left, right);
					break;
				case '*':
					equation = new MultiplyExpression(left, right);
					break;
				case '/':
					equation = new DivideExpression(left, right);
					break;
				default:
					Logging.getLogger(LogType.ERROR).log(String.format("Line: %d Unknown operation type %s", ctx.getStart().getLine(), operation));
					return;
				}
				currentEquation.push(equation);
			}
			
			@Override
			public void exitMinusExpression(MembraneModelParser.MinusExpressionContext ctx) {
				// Don't have an equation type for unary minus yet, so fake it with (0 - x)
				if (currentEquation.size() < 1) {
					Logging.getLogger(LogType.ERROR).log(String.format("Line: %d Error parsing equation", ctx.getStart().getLine()));
					return;
				}
				currentEquation.push(new SubtractExpression(new NumberExpression(0f), currentEquation.pop()));
			}
			
			@Override
			public void exitLocatedSpeciesExpression(MembraneModelParser.LocatedSpeciesExpressionContext ctx) {
				// Don't need to check for being in a location block, because in these cases ParameterOrSpeciesExpression matches first
				LocatedSpecies locatedSpecies = new LocatedSpecies(ctx.locSpecies.species.getText(), Location.valueOf(ctx.locSpecies.location.getText()), ctx.locSpecies.membrane.getText());
				currentEquation.push(new LocatedSpeciesExpression(locatedSpecies));
			}
			
			@Override
			public void exitRoundExpression(MembraneModelParser.RoundExpressionContext ctx) {
				currentEquation.push(new RoundExpression(currentEquation.pop()));
			}
			
			@Override
			public void exitNaturalLogExpression(MembraneModelParser.NaturalLogExpressionContext ctx) {
				currentEquation.push(new NaturalLogExpression(currentEquation.pop()));
			}

			/**
			 * @param ctx A rule in the parse tree.
			 * @return The text that was parsed into this rule, before whitespace was removed.
			 */
			private String getOriginalText(ParserRuleContext ctx) {
				int startIndex = ctx.start.getStartIndex();
			    int stopIndex = ctx.stop.getStopIndex();
			    return ctx.start.getInputStream().getText(new Interval(startIndex,stopIndex));
			}
	    });
	    
		return parser;
	}
	
	/**
	 * Simplify an equation by replacing all its parameters with their values, 
	 *   and applying all operations where possible.
	 * @return A simplified version of the given equation.
	 */
	private Equation simplifyEquation(Equation equation) {
		if (equation instanceof NumberExpression) {
			// Can't simplify a number - it's already simple
			return equation;
		}
		else if (equation instanceof LocatedSpeciesExpression) {
			// Can't simplify a located species - it changes each time
			return equation;
		}
		else if (equation instanceof ParameterExpression) {
			// Can simplify a parameter - replace it with its value
			String parameterName = ((ParameterExpression) equation).getParameterName();
			Double parameterValue = parameterValues.get(parameterName);
			if (parameterValue == null) {
				parameterValue = Double.NEGATIVE_INFINITY; // This makes the reaction meaningless, but will be picked up when checking that all the parameters have values
			}
			return new NumberExpression(parameterValue);
		}
		else if (equation instanceof UnaryOperationExpression) {
			// Can maybe simplify a unary operation
			UnaryOperationExpression unaryOpEquation = (UnaryOperationExpression) equation;
			Equation simpleSubexpression = simplifyEquation(unaryOpEquation.getSubexpression());
			if (simpleSubexpression instanceof NumberExpression) {
				// Can apply this operation
				try {
					return new NumberExpression(unaryOpEquation.evaluate(simpleSubexpression.evaluate(null)));
				} catch (InvalidEquationEvaluationException e) {
					Logging.getLogger(LogType.ERROR).log(String.format("Unexpected error applying a unary operator to a number expression: %s", unaryOpEquation), e);
					return null;
				}
			}
			else {
				// Can't apply this operation, so create it again with simplified subexpression
				if (unaryOpEquation instanceof RoundExpression) {
					return new RoundExpression(simpleSubexpression);
				}
				else if (unaryOpEquation instanceof NaturalLogExpression) {
					return new NaturalLogExpression(simpleSubexpression);
				}
				else {
					Logging.getLogger(LogType.ERROR).log("Unknown unary operation type for simplification: " + unaryOpEquation.getClass().getSimpleName());
					return null;
				}
			}
		}
		else if (equation instanceof OperationExpression) {
			// Can maybe simplify an operation
			OperationExpression operationEquation = (OperationExpression) equation;
			Equation simpleLeft = simplifyEquation(operationEquation.getLeft());
			Equation simpleRight = simplifyEquation(operationEquation.getRight());
			if (simpleLeft instanceof NumberExpression && simpleRight instanceof NumberExpression) {
				// Can apply this operation
				try {
					return new NumberExpression(operationEquation.evaluate(simpleLeft.evaluate(null), simpleRight.evaluate(null)));
				} catch (InvalidEquationEvaluationException e) {
					Logging.getLogger(LogType.ERROR).log(String.format("Unexpected error applying an operator to two number expressions: %s", operationEquation), e);
					return null;
				}
			}
			else {
				// Can't apply this operation, so create it again with simplified left and right
				if (operationEquation instanceof AddExpression) {
					return new AddExpression(simpleLeft, simpleRight);
				}
				else if (operationEquation instanceof SubtractExpression) {
					return new SubtractExpression(simpleLeft, simpleRight);
				}
				else if (operationEquation instanceof MultiplyExpression) {
					return new MultiplyExpression(simpleLeft, simpleRight);
				}
				else if (operationEquation instanceof DivideExpression) {
					return new DivideExpression(simpleLeft, simpleRight);
				}
				else {
					Logging.getLogger(LogType.ERROR).log("Unknown operation type for simplification: " + operationEquation.getClass().getSimpleName());
					return null;
				}
			}
		}
		else {
			Logging.getLogger(LogType.ERROR).log("Unknown equation type for simplification: " + equation.getClass().getSimpleName());
			return null;
		}
	}
	
	/**
	 * Simplify an equation all the way to an actual number.
	 * Equation must not contain any references to species numbers, and all its referenced parameters must have been parsed already.
	 * @return The value of the equation, or null on error.
	 */
	private Double simplifyEquationToDouble(Equation equation) {
		Equation simplifiedEquation = simplifyEquation(equation);
		// To be a valid equation, it must simplify to a number
		if (!(simplifiedEquation instanceof NumberExpression)) {
			Logging.getLogger(LogType.ERROR).log("Failed to simplify equation to a number");
			return null;
		}
		try {
			return simplifiedEquation.evaluate(null);
		} catch (InvalidEquationEvaluationException e) {
			Logging.getLogger(LogType.ERROR).log("Failed to evaluate equation", e);
			return null;
		}
	}
	
	/**
	 * Simplify an equation all the way to an actual integer.
	 * Equation must not contain any references to species numbers, and all its referenced parameters must have been parsed already.
	 * @return The value of the equation, or null on error.
	 */
	private Integer simplifyEquationToInteger(Equation equation) {
		Double value = simplifyEquationToDouble(equation);
		if (value == null) {
			return null;
		}
		int intValue = (int) value.doubleValue();
		if (value != intValue) {
			Logging.getLogger(LogType.ERROR).log(String.format("Equation does not evaluate to an integer: %s = %f", equation, value));
			return null;
		}
		return intValue;
	}
	
	/**
	 * Runs the parser, creating temporary Java objects containing the parsed results.
	 * @param parser
	 * @throws InvalidModelException If we could not parse the model file
	 */
	private void runParser(MembraneModelParser parser) throws InvalidModelException {
		speciesLocationsListed.clear();
		allSpeciesNames.clear();
	    speciesLocationsInferred.clear();
	    membraneNamesInferred.clear();
	    parameterValues.clear();
	    parameterNamesInferred.clear();
	    reactions.clear();
	    initialConditions = null;
	    initialSpecies.clear();
	    
	    try {
	    	parserErrors = new ArrayList<>();
	    	parser.model();
	    }
	    catch (IllegalStateException e) {
	    	throw new InvalidModelException("Failed to parse model file because of a grammar error: " + e.getMessage(), e);
	    }
	    if (!parserErrors.isEmpty()) {
	    	throw new InvalidModelException("Failed to parse model file because of a semantic error: \n" + StringUtils.join(parserErrors, "\n"), null);
	    }
	}
	
	/**
	 * Assumes: The model file has been parsed.
	 * Performs semantic checks on the data parsed from the model file. 
	 * Throws warnings and errors for any semantic check failures. 
	 * @throws InvalidModelException If the model cannot be run because it is invalid.
	 */
	private void performSemanticChecks() throws InvalidModelException {
		boolean modelInvalid = false;
		
		// Warn about species that are used in reactions but not listed explicitly
		Set<LocatedSpecies> speciesNotListed = new LinkedHashSet<>();
    	speciesNotListed.addAll(speciesLocationsInferred);
    	speciesNotListed.removeAll(speciesLocationsListed);
    	if (!speciesNotListed.isEmpty()) {
    		if (speciesNotListed.size() == 1) {
    			Logging.getLogger(LogType.WARNING).log(String.format("Species '%s' used in reactions but not listed explicitly", 
    					speciesNotListed.iterator().next()));
    		}
    		else {
    			Logging.getLogger(LogType.WARNING).log(String.format("%d species used in reactions but not listed explicitly: '%s'", 
    					speciesNotListed.size(), StringUtils.join(speciesNotListed, "', '")));
    		}
    		// Offer fixes for missing species
    		for (LocatedSpecies locatedSpecies: speciesNotListed) {
    			Logging.getLogger(LogType.WARNING_FIX).log(String.format("species %s %s %s = 0 units", 
    					locatedSpecies.getSpeciesName(), locatedSpecies.getLocation(), locatedSpecies.getMembraneName()));
    		}
    	}
    	
    	// Warn about species that are listed explicitly but not used in reactions
    	Set<LocatedSpecies> speciesNotUsed = new LinkedHashSet<>();
    	speciesNotUsed.addAll(speciesLocationsListed);
    	speciesNotUsed.removeAll(speciesLocationsInferred);
    	if (!speciesNotUsed.isEmpty()) {
    		if (speciesNotUsed.size() == 1) {
    			Logging.getLogger(LogType.WARNING).log(String.format("Species '%s' listed explicitly but not used in reactions", 
    					speciesNotUsed.iterator().next()));
    		}
    		else {
    			Logging.getLogger(LogType.WARNING).log(String.format("%d species listed explicitly but not used in reactions: '%s'", 
    					speciesNotUsed.size(), StringUtils.join(speciesNotUsed, "', '")));
    		}
    	}
    	
    	// Warn about parameters that are used in reactions but not listed explicitly
    	Set<String> parametersNotListed = new LinkedHashSet<>();
    	parametersNotListed.addAll(parameterNamesInferred);
    	parametersNotListed.removeAll(parameterValues.keySet());
    	if (!parametersNotListed.isEmpty()) {
    		modelInvalid = true;
    		if (parametersNotListed.size() == 1) {
    			Logging.getLogger(LogType.ERROR).log(String.format("Parameter %s used but not listed explicitly", 
    					parametersNotListed.iterator().next()));
    		}
    		else {
    			Logging.getLogger(LogType.ERROR).log(String.format("%d parameters used but not listed explicitly: %s", 
    					parametersNotListed.size(), StringUtils.join(parametersNotListed, ", ")));
    		}
    		// Offer fixes for missing parameters
    		if (!speciesNotListed.isEmpty()) {
    			Logging.getLogger(LogType.WARNING_FIX).log("");
    		}
    		for (String parameterName: parametersNotListed) {
    			Logging.getLogger(LogType.WARNING_FIX).log(String.format("parameter %s = 0 units", parameterName));
    		}
    	}
    	
    	Set<String> parametersNotUsed = new LinkedHashSet<>();
    	parametersNotUsed.addAll(parameterValues.keySet());
    	parametersNotUsed.removeAll(parameterNamesInferred);
    	if (!parametersNotUsed.isEmpty()) {
    		if (parametersNotUsed.size() == 1) {
    			Logging.getLogger(LogType.WARNING).log(String.format("Parameter %s listed explicitly but not used", 
    					parametersNotUsed.iterator().next()));
    		}
    		else {
    			Logging.getLogger(LogType.WARNING).log(String.format("%d parameters listed explicitly but not used: %s", 
    					parametersNotUsed.size(), StringUtils.join(parametersNotUsed, ", ")));
    		}
    	}
    	
    	// Print out info about the parsed model
	    if (Logging.isLoggable(LogType.DEBUG)) {
	    	logSet(LogType.DEBUG, "Species names listed explicitly", speciesLocationsListed);
	    	logSet(LogType.DEBUG, "Parameter names listed explicitly", parameterValues.keySet());
	    	logSet(LogType.DEBUG, "Reactions (with reversible reactions duplicated)", reactions);
	    	
	    	logSet(LogType.DEBUG, "Species names used in reactions but not listed explicitly", speciesNotListed);
	    	logSet(LogType.DEBUG, "Species names listed explicitly but not used in reactions", speciesNotUsed);
	    	logSet(LogType.DEBUG, "Parameter names used but not listed explicitly", parametersNotListed);
	    	logSet(LogType.DEBUG, "Parameter names listed explicitly but not used", parametersNotUsed);
	    	
	    	logSet(LogType.DEBUG, "Membrane names inferred from reactions", membraneNamesInferred);
	    	
	    	Logging.getLogger(LogType.DEBUG).log("Initial conditions:");
	    	if (initialConditions == null) {
	    		Logging.getLogger(LogType.DEBUG).log("  <null>");
	    	}
	    	else {
	    		Logging.getLogger(LogType.DEBUG).log(initialConditions.toString());
	    	}
	    	Logging.getLogger(LogType.DEBUG).log("");
	    }
	    
	    // Print out info about the model's parameters
	    if (Logging.isLoggable(LogType.PARAMETERS)) {
	    	
	    	Logging.getLogger(LogType.PARAMETERS).log("Parameters equations defined in the model file:");
	    	for (String parameterName: parameterEquations.keySet()) {
	    		Logging.getLogger(LogType.PARAMETERS).log(String.format("parameter %s = %s %s", 
	    				parameterName, parameterEquations.get(parameterName), parameterUnits.get(parameterName)));
	    	}
	    	Logging.getLogger(LogType.PARAMETERS).log("");
	    	Logging.getLogger(LogType.PARAMETERS).log("Parameter values computed:");
	    	for (String parameterName: parameterEquations.keySet()) {
	    		Logging.getLogger(LogType.PARAMETERS).log(String.format("parameter %s = %s %s", 
	    				parameterName, parameterValues.get(parameterName), parameterUnits.get(parameterName)));
	    	}
	    }
	    
	    // Print out info about the tags
	    if (Logging.isLoggable(LogType.TAGS)) {
	    	if (tagsToSpecies.isEmpty()) {
	    		Logging.getLogger(LogType.TAGS).log("No tags defined in the model file.");
	    	}
	    	else {
	    		Logging.getLogger(LogType.TAGS).log("--------------------------------------");
	    		Logging.getLogger(LogType.TAGS).log("- Species per tag (one line per tag) -");
	    		Logging.getLogger(LogType.TAGS).log("--------------------------------------");
	    		
	    		// Sort tags by now many species have that tag
	    		Set<String> orderedTags = new TreeSet<>(new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						int sizeCompare = Integer.compare(tagsToSpecies.get(o1).size(), tagsToSpecies.get(o2).size());
						if (sizeCompare != 0) {
							return -sizeCompare;
						}
						else {
							return o1.compareTo(o2);
						}
					}
				});
	    		orderedTags.addAll(tagsToSpecies.keySet());
	    		for (String tagName: orderedTags) {
	    			StringJoiner species = new StringJoiner(", ");
	    			for (String speciesName: tagsToSpecies.get(tagName)) {
	    				species.add(speciesName);
	    			}
	    			Logging.getLogger(LogType.TAGS).log(String.format("%s: %s", tagName, species.toString()));
	    		}
	    		
	    		Logging.getLogger(LogType.TAGS).log("");
	    		Logging.getLogger(LogType.TAGS).log("-------------------------------------------");
	    		Logging.getLogger(LogType.TAGS).log("- Tags per species (one line per species) -");
	    		Logging.getLogger(LogType.TAGS).log("-------------------------------------------");
	    		
	    		// Sort species alphabetically by name
	    		Set<String> orderedSpecies = new TreeSet<>();
	    		orderedSpecies.addAll(speciesToTags.keySet());
	    		for (String speciesName: orderedSpecies) {
	    			StringJoiner tags = new StringJoiner(", ");
	    			for (String tagName: speciesToTags.get(speciesName)) {
	    				tags.add(tagName);
	    			}
	    			Logging.getLogger(LogType.TAGS).log(String.format("%s: %s", speciesName, tags.toString()));
	    		}
	    	}
	    }
	    
	    // Throw an exception if the model is not valid
	    if (modelInvalid) {
	    	throw new InvalidModelException("Simulation cannot be run because model is not valid - check error log for details.", null);
	    }
	}
	
	/**
	 * Assumes: The model file has been parsed.
	 * @param randomSeed
	 * @return The parsed model file as a Java object.
	 * @throws InvalidModelException If the model failed semantic checks.
	 * @throws InvalidSimulationException If the simulation could not be created (probably because initial reaction rates could not be calculated).
	 */
	private Simulation constructSimulation(Integer randomSeed) throws InvalidSimulationException, InvalidModelException {
    	Set<String> allSpeciesNames = Stream.concat(speciesLocationsListed.stream(), speciesLocationsInferred.stream())
    			.map(locatedSpecies -> locatedSpecies.getSpeciesName())
    			.collect(Collectors.toCollection(LinkedHashSet::new));
    	
    	if (initialConditions == null) {
    		Logging.getLogger(LogType.WARNING).log("No initial conditions specified");
    	}
    	else {
    		// Add the initial species to each membrane (wherever it is in the tree)
    		for (Entry<LocatedSpecies> entry: initialSpecies.entrySet()) {
    			LocatedSpecies locatedSpecies = entry.getElement();
    			int howMany = entry.getCount();
    			Collection<Membrane> matchingMembranes = initialConditions.getMatchingMembranes(locatedSpecies.getMembraneName(), locatedSpecies.getLocation());
    			if (matchingMembranes.isEmpty()) {
    				Logging.getLogger(LogType.ERROR).log("Couldn't find matching membrane: " + locatedSpecies.getMembraneName());
    			}
    			for (Membrane matchingMembrane: matchingMembranes) {
					matchingMembrane.addSpecies(locatedSpecies, howMany);
    			}
    		}
    	}
    	
		MembraneModel model = new MembraneModel(membraneNamesInferred, allSpeciesNames, initialSpecies, parameterValues, reactions, speciesToTags, tagsToSpecies);
		
		// Perform semantic checks on the parsed model
	    performSemanticChecks();
		
		return new ReactionRateTableSimulation(model, initialConditions, randomSeed);
	}
	
	/**
	 * Log the contents of a set.
	 */
	private void logSet(LogType level, String title, Set<?> set) {
		Logging.getLogger(level).log(String.format("%s (%d)", title, set.size()));
		for (Object name: set) {
    		Logging.getLogger(level).log("  " + name);
    	}
    	if (set.isEmpty()) {
    		Logging.getLogger(level).log("  <none>");
    	}
    	Logging.getLogger(level).log("");
	}
}
