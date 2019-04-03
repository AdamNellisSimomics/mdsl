package com.simomics.leishsim;

import java.io.PrintStream;

/**
 * Logger that prints to the screen as well as writing to a file.
 */
public class TextFileAndScreenLogger extends TextFileLogger {

	private final PrintStream screenOutput;

	/**
	 * @see TextFileLogger#TextFileLogger(String)
	 * @param logName
	 * @param screenOutput either System.out or System.err
	 */
	public TextFileAndScreenLogger(String logName, PrintStream screenOutput, boolean addTimestamp) {
		super(logName, addTimestamp);
		this.screenOutput = screenOutput;
	}

	/**
	 * @see TextFileLogger#TextFileLogger(String, String)
	 * @param logName
	 * @param fileExtension
	 * @param screenOutput either System.out or System.err
	 */
	public TextFileAndScreenLogger(String logName, String fileExtension, PrintStream screenOutput, boolean addTimestamp) {
		super(logName, fileExtension, addTimestamp);
		this.screenOutput = screenOutput;
	}

	@Override
	public void log(String message) {
		super.log(message);
		screenOutput.print(logName);
		screenOutput.print(": ");
		screenOutput.println(message);
	}

	@Override
	public void log(String message, Throwable exception) {
		super.log(message, exception);
		screenOutput.print(logName);
		screenOutput.print(": ");
		screenOutput.println(message);
		exception.printStackTrace(screenOutput);
	}

}
