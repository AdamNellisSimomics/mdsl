package com.simomics.leishsim.diagramming;

import com.simomics.leishsim.DiagramGenerator;

/**
 * A command used by the diagram generator.
 */
public interface DiagramCommand {

	/** Run the command */
	void run(DiagramGenerator diagramGenerator);
}
