package com.simomics.leishsim;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.diagramming.CommandConvert;
import com.simomics.leishsim.diagramming.CommandCreate;
import com.simomics.leishsim.diagramming.DiagramCommand;

/**
 * Main class for the diagram generation program.
 */
public final class DiagramGenerator {

	@Parameter(names = "--output-dir", description = "Directory into which to write output and log files")
	public String outputDirectory = "output";
	
	@Parameter(names = "--output-filename", description = "Name of the file to write as output. "
			+ "File format is determined by the extension given. "
			+ "Valid extensions are: .xml (for Artoo) .json (for D3) .graphml (for yEd)")
	public String outputFileName = "diagram.xml";
	
	private DiagramGenerator() {}
	
	/**
	 * The commands that this program understands.
	 */
	private static Map<String, DiagramCommand> getCommands() {
		Map<String, DiagramCommand> commands = new HashMap<>();
		commands.put("create", new CommandCreate());
		commands.put("convert", new CommandConvert());
		return commands;
	}
	
	/**
	 * Entry point for the program. 
	 * @param args Command line arguments. See {@link #getCommands()}.
	 */
	public static void main(String... args) {

		// Parse command line arguments
		DiagramGenerator diagramGenerator = new DiagramGenerator();
		JCommander jcommander = new JCommander(diagramGenerator);
		Map<String, DiagramCommand> commands = getCommands();
		for (Entry<String, DiagramCommand> command: commands.entrySet()) {
			jcommander.addCommand(command.getKey(), command.getValue());
		}
		try {
			jcommander.parse(args);
			DiagramCommand command = commands.get(jcommander.getParsedCommand());
			if (command == null) {
				// Unknown command, or no command provided
				jcommander.usage();
			}
			else {
				command.run(diagramGenerator);
			}
		}
		catch(ParameterException e) {
			// Could not parse command line arguments
			Logging.getLogger(LogType.ERROR).log(e.getMessage());
			jcommander.usage();
			return;
		}
	}
}
