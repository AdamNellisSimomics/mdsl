package com.simomics.leishsim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.simomics.leishsim.Logging.LogType;

/**
 * Writes log messages to a text file.
 */
public class TextFileLogger implements Logger {
	
	/** The directory in which this log file will be written */
	private File outputDir;

	/** The name of the file into which this log will be written */
	protected String logName;
	
	/** The file extension of this log file */
	private String fileExtension;

	/** Whether to add a timestamp to log messages */
	private boolean addTimestamp;
	
	/**
	 * @return A timestamp formatted as a string, for including with log messages.
	 */
	protected String getTimestamp() {
		return new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss]").format(new Date());
	}
	
	/**
	 * Create a log that will be written to a file with the given name and a default extension of .txt
	 * @see #TextFileLogger(String, String)
	 * @param logName
	 */
	public TextFileLogger(String logName, boolean addTimestamp) {
		this(logName, "txt", addTimestamp);
	}
	
	/**
	 * Create a log that will be written to a file with the given name and extension.
	 * @param logName
	 * @param fileExtension
	 */
	public TextFileLogger(String logName, String fileExtension, boolean addTimestamp) {
		this.logName = logName;
		this.fileExtension = fileExtension;
		this.addTimestamp = addTimestamp;
	}
	
	/**
	 * Set the directory into which this log file will be written.
	 * @param newOutputDir
	 */
	public void setOutputDir(File newOutputDir) {
		outputDir = newOutputDir;
		File logFile = getLogFile();
		try {
			logFile.createNewFile();
		} catch (IOException e) {
			Logging.getLogger(LogType.ERROR).log(String.format("Could not create output file %s. Maybe the directory is not writeable?", logFile.getAbsolutePath()));
		}
	}

	@Override
	public void log(String message) {
		// Write message to its log file
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getLogFile(), true), "utf-8"))) {
			if (addTimestamp) {
				writer.write(getTimestamp());
				writer.write(' ');
			}
			writer.write(message);
			writer.write('\n');
		} catch (IOException e) {
			// Failed to write the log message
			e.printStackTrace();
		}
	}
	
	@Override
	public void log(String message, Throwable exception) {
		// Log exception
		log(message);
		
		// Write stack trace to the log file
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getLogFile(), true), "utf-8"))) {
			writer.write(ExceptionUtils.getStackTrace(exception));
			writer.write('\n');
		} catch (IOException e) {
			// Failed to write to the file
			e.printStackTrace();
		}
	}
	
	public File getLogFile() {
		if (outputDir != null) {
			return new File(outputDir, getLogFileName());
		}
		else {
			return new File(getLogFileName());
		}
	}
	
	private String getLogFileName() {
		return FILE_PREFIX + logName + '.' + fileExtension;
	}
}
