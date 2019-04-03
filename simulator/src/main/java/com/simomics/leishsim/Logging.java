package com.simomics.leishsim;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Static methods to manage logging throughout the program. 
 */
public final class Logging {
	
	/**
	 * Different types of log, that will be written to different files.
	 */
	public static enum LogType {
		/** Errors that prevent the simulation from continuing */
		ERROR, 
		
		/** Warning messages about potential user errors */
		WARNING, 
		
		/** Suggested MDSL lines to fix the warning messages */
		WARNING_FIX,
		
		/** Progress messages about what the simulator is doing */
		PROGRESS,
		
		/** Information about the parameters used in the simulation */
		PARAMETERS,
		
		/** Information about the tags used in the model */
		TAGS,
		
		/** Full state of the simulation when the simulation ends */
		FULL_STATE_AT_END,
		
		/** Simulator output aggregated over membranes */
		PRINTED_SPECIES, 
		
		/** Simulator output per membrane */
		PRINTED_SPECIES_PER_MEMBRANE,
		
		/** The reactions in order with their numbers, for linking to propensities file */
		REACTION_NUMBERS,
		
		/** Output of reaction propensities over time */
		PRINTED_PROPENSITIES,
		
		/** Details about what the simulator is doing */
		DETAIL,
		
		/** Low-level debugging information to assist in fixing problems */
		DEBUG, 
		
		/** Full trace of very low level steps - can only be taken in very small doses */
		FULL;
	}
	
	/**
	 * A logger that does nothing, for returning as a placeholder when a logger is not enabled
	 *  (clients can safely write to the null logger all they want, for log levels that are not enabled)
	 */
	private static Logger nullLogger = new Logger() {
		@Override
		public void log(String message) {
			// do nothing
		}
		@Override
		public void log(String message, Throwable exception) {
			// do nothing
		}
	};
	
	/**
	 * The types of message that will be logged. Any massages of different types will be ignored. 
	 */
	private static Map<String, Logger> enabledLogs = new HashMap<>();
	
	// Default to basic logging
	static {
		setLogLevel(LogType.PRINTED_SPECIES);
	}
	
	/** 
	 * Static class - not allowed to instantiate.
	 */
	private Logging() {}
	
	/**
	 * Set the level at which we want to see log messages.
	 * @param newLvl
	 */
	public static void setLogLevel(LogType newLvl) {
		// Add loggers for the given level and all levels above the given level
		enabledLogs.clear();
		switch (newLvl) {
		case FULL:
			enabledLogs.put(LogType.FULL.name(), new TextFileLogger("Full", true));
		case DEBUG:
			enabledLogs.put(LogType.DEBUG.name(), new TextFileAndScreenLogger("Debug", System.out, true));
		case DETAIL:
			enabledLogs.put(LogType.DETAIL.name(), new TextFileAndScreenLogger("Detail", System.out, true));
		case PRINTED_PROPENSITIES:
			enabledLogs.put(LogType.PRINTED_PROPENSITIES.name(), new TextFileLogger("Propensities", "csv", false));
		case REACTION_NUMBERS:
			enabledLogs.put(LogType.REACTION_NUMBERS.name(), new TextFileLogger("Reaction Numbers", "csv", false));
		case PRINTED_SPECIES_PER_MEMBRANE:
			enabledLogs.put(LogType.PRINTED_SPECIES_PER_MEMBRANE.name(), new TextFileLogger("SpeciesPerMembrane", "csv", false));
		case PRINTED_SPECIES:
			enabledLogs.put(LogType.PRINTED_SPECIES.name(), new TextFileAndScreenLogger("Species", "csv", System.out, true) {
				@Override
				protected String getTimestamp() {
					return super.getTimestamp() + ",";
				}
			});
		case FULL_STATE_AT_END:
			enabledLogs.put(LogType.FULL_STATE_AT_END.name(), new TextFileAndScreenLogger("FullState", "mdsl", System.out, false));
		case TAGS:
			enabledLogs.put(LogType.TAGS.name(), new TextFileAndScreenLogger("Tags", System.out, false));
		case PARAMETERS:
			enabledLogs.put(LogType.PARAMETERS.name(), new TextFileAndScreenLogger("Parameters", System.out, false));
		case PROGRESS:
			enabledLogs.put(LogType.PROGRESS.name(), new TextFileAndScreenLogger("Progress", System.out, true));
		case WARNING_FIX:
			enabledLogs.put(LogType.WARNING_FIX.name(), new TextFileLogger("FixWarnings", "mdsl", false));
		case WARNING:
			enabledLogs.put(LogType.WARNING.name(), new TextFileAndScreenLogger("Warnings", System.err, true) {
				@Override
				public void log(String message) {
					super.log("WARNING: " + message);
				}
			});
		case ERROR:
			enabledLogs.put(LogType.ERROR.name(), new TextFileAndScreenLogger("Errors", System.err, true) {
				@Override
				public void log(String message) {
					super.log("ERROR: " + message);
				}
			});
			break;
		default:
			getLogger(LogType.ERROR).log("Unknown log level: " + newLvl);
		}
	}
	
	/**
	 * @param loggerName
	 * @return The logger enabled with the given name, or the null logger if this name is not enabled.
	 * So it is safe to do: getLogger(name).log(...) even if "name" might not be an enabled logger. 
	 */
	public static Logger getLogger(String loggerName) {
		return enabledLogs.getOrDefault(loggerName, nullLogger);
	}
	
	/**
	 * @param logType
	 * @return The logger registered for the given log type, or the null logger if this type is not enabled.
	 */
	public static Logger getLogger(LogType logType) {
		return getLogger(logType.name());
	}
	
	/**
	 * Reset the logs.
	 * Clears all output files from the directory, then creates blank files for each enabled log level.
	 * @param newOutputDir
	 * @param level
	 */
	public static void resetLogging(File newOutputDir, LogType level) {
		// Set new logging level
		setLogLevel(level);
		// Create output directory if it doesn't exist
		if (!newOutputDir.isDirectory()) {
			boolean created = newOutputDir.mkdir();
			if (!created) {
				getLogger(LogType.ERROR).log("Could not create logging output directory: " + newOutputDir);
			}
		}
		// Delete any previous output files in this directory
		for(File file: newOutputDir.listFiles()) {
			if (file.getName().startsWith(Logger.FILE_PREFIX)) {
				file.delete();
			}
		}
		// Create blank files for each enabled logging level
		for (Logger logger: enabledLogs.values()) {
			if (logger instanceof TextFileLogger) {
				((TextFileLogger) logger).setOutputDir(newOutputDir);
			}
		}
	}
	
	/**
	 * @param level
	 * @return True if a message at level will be printed.
	 */
	public static boolean isLoggable(LogType level) {
		return enabledLogs.containsKey(level.name());
	}
}
