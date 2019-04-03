package com.simomics.leishsim.diagramming;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.model.MembraneModel;
import com.simomics.leishsim.model.Reaction;
import com.simomics.leishsim.model.StoichiometrySpecies;

/**
 * An abstract representation of a graph created from an MDSL file.
 */
public class Graph {

	/** All the nodes in the graph */
	private final Set<Node> nodes;

	/** All the edges in the graph */
	private final Multimap<Node, Node> edges;
	
	/** For directed graphs, the inverse connections */
	private final Multimap<Node, Node> inverseEdges;
	
	/**
	 * Create an empty graph.
	 */
	public Graph() {
		// Create an empty graph
		nodes = new LinkedHashSet<>();
		edges = HashMultimap.create();
		inverseEdges = HashMultimap.create();
	}
	
	/**
	 * Create a graph representing the given model.
	 */
	public static Graph create(MembraneModel model) {
		Graph graph = new Graph();
		
		Map<String, Node> speciesNodes = new HashMap<>();
		Map<Reaction, Node> reactionNodes = new HashMap<>();
		int nextSpeciesReactionIndex = 0;
		
		// Create a node for each species
		for (String speciesName: model.getSpeciesNames()) {
			Node node = new Node(nextSpeciesReactionIndex, 1, speciesName);
			nextSpeciesReactionIndex += 1;
			speciesNodes.put(speciesName, node);
			graph.nodes.add(node);
		}
		
		// Create a node for each reaction
		Set<Reaction> reactions = model.getReactions();
		for (Reaction reaction: reactions) {
			Node node = new Node(nextSpeciesReactionIndex, 2);
			node.addData("mdsl_line", reaction.getMdslLine());
			node.addData("mdsl_line_number", reaction.getMdslLineNumber().toString());
			nextSpeciesReactionIndex += 1;
			reactionNodes.put(reaction, node);
			graph.nodes.add(node);
		}
		
		// Create an edge for each species involved in each reaction
		// TODO: Ignoring locations and stoichiometries for now
		for (Reaction reaction: model.getReactions()) {
			Node reactionNode = reactionNodes.get(reaction);
			for (StoichiometrySpecies stoichSpecies: reaction.getConsumedSpecies()) {
				Node speciesNode = speciesNodes.get(stoichSpecies.getSpeciesName());
				graph.addEdge(speciesNode, reactionNode);
			}
			for (StoichiometrySpecies stoichSpecies: reaction.getProducedSpecies()) {
				Node speciesNode = speciesNodes.get(stoichSpecies.getSpeciesName());
				graph.addEdge(reactionNode, speciesNode);
			}
		}
		return graph;
	}
	
	/**
	 * Create a graph from the given model, restricted to nodes with the given tag.
	 */
	public static Graph create(MembraneModel model, List<String> tags) {
		Graph graph = new Graph();
		
		Map<String, Node> speciesNodes = new HashMap<>();
		Map<Reaction, Node> reactionNodes = new HashMap<>();
		int nextSpeciesReactionIndex = 0;
		
		// Create a node for each species with this tag
		Set<String> speciesNames = new HashSet<>();
		for (String tag: tags) {
			speciesNames.addAll(model.getSpeciesForTag(tag));
		}
		for (String speciesName: speciesNames) {
			Node node = new Node(nextSpeciesReactionIndex, 1, speciesName);
			nextSpeciesReactionIndex += 1;
			speciesNodes.put(speciesName, node);
			graph.nodes.add(node);
		}
		
		// Create a node for each reaction involving species with this tag
		Set<Reaction> reactions = new HashSet<>();
		for (String tag: tags) {
			reactions.addAll(model.getReactionsForSpeciesTag(tag));
		}
		for (Reaction reaction: reactions) {
			Node node = new Node(nextSpeciesReactionIndex, 2);
			node.addData("mdsl_line", reaction.getMdslLine());
			node.addData("mdsl_line_number", reaction.getMdslLineNumber().toString());
			nextSpeciesReactionIndex += 1;
			reactionNodes.put(reaction, node);
			graph.nodes.add(node);
		}

		// Create edges for reactions. Create duplicate nodes for species that don't have the tag.
		// TODO: Ignoring locations and stoichiometries for now
		for (Reaction reaction: reactions) {
			Node reactionNode = reactionNodes.get(reaction);
			for (StoichiometrySpecies stoichSpecies: reaction.getConsumedSpecies()) {
				Node speciesNode = speciesNodes.get(stoichSpecies.getSpeciesName());
				if (speciesNode == null) {
					speciesNode = new Node(nextSpeciesReactionIndex, 3, stoichSpecies.getSpeciesName());
					nextSpeciesReactionIndex += 1;
					graph.nodes.add(speciesNode);
				}
				graph.addEdge(speciesNode, reactionNode);
			}
			for (StoichiometrySpecies stoichSpecies: reaction.getProducedSpecies()) {
				Node speciesNode = speciesNodes.get(stoichSpecies.getSpeciesName());
				if (speciesNode == null) {
					speciesNode = new Node(nextSpeciesReactionIndex, 3, stoichSpecies.getSpeciesName());
					nextSpeciesReactionIndex += 1;
					graph.nodes.add(speciesNode);
				}
				graph.addEdge(reactionNode, speciesNode);
			}
		}
		return graph;
	}
	
	/**
	 * Read a .graph file.
	 */
	public static Graph readFromGraphFile(File graphFile) {
		Graph graph = new Graph();
		
		boolean inNode = false;
		boolean inEdge = false;
		
		Integer nodeId = null;
		Integer nodeGroup = null;
		String nodeLabel = null;
		Double nodeXPos = null;
		Double nodeYPos = null;
		Map<String,String> nodeData = null;
		
		Integer edgeSourceId = null;
		Integer edgeDestId = null;
		
		boolean errors = false;
		
		Map<Integer, Node> allNodes = new HashMap<>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(graphFile))) {
		    String line;
		    int lineNumber = 0;
		    while ((line = br.readLine()) != null) {
		       line = line.trim();
		       lineNumber += 1;
		       
		       if (line.equals("node") || line.equals("edge") || line.isEmpty()) {
		    	   if (inNode) {
		    		   // Add a new node
		    		   Node newNode = new Node(nodeId, nodeGroup, nodeLabel);
		    		   if (nodeXPos != null) {
		    			   newNode.setxPos(nodeXPos);
		    		   }
		    		   if (nodeYPos != null) {
		    			   newNode.setyPos(nodeYPos);
		    		   }
		    		   for (Entry<String,String> data: nodeData.entrySet()) {
		    			   newNode.addData(data.getKey(), data.getValue());
		    		   }
		    		   allNodes.put(nodeId, newNode);
		    		   graph.nodes.add(newNode);
		    	   }
		    	   else if (inEdge) {
		    		   // Add a new edge
		    		   Node sourceNode = allNodes.get(edgeSourceId);
		    		   Node destNode = allNodes.get(edgeDestId);
		    		   if (sourceNode == null) {
		    			   Logging.getLogger(LogType.ERROR).log(String.format("Line %d: Can't find source node %d for edge", lineNumber, edgeSourceId));
		    			   errors = true;
		    		   }
		    		   if (destNode == null) {
		    			   Logging.getLogger(LogType.ERROR).log(String.format("Line %d: Can't find destination node %d for edge", lineNumber, edgeDestId));
		    			   errors = true;
		    		   }
		    		   graph.addEdge(sourceNode, destNode);
		    	   }
		       }
		       
		       if (line.equals("node")) {
		    	   // Start a new node
		    	   if (inEdge) {
		    		   Logging.getLogger(LogType.ERROR).log(String.format("Line %d: All nodes must come before any edges.", lineNumber));
		    		   Logging.getLogger(LogType.ERROR).log("Failed to parse file: " + graphFile.getAbsolutePath());
		    		   return graph;
		    	   }
		    	   nodeId = null;
		    	   nodeGroup = 1;
		    	   nodeLabel = null;
		    	   nodeXPos = null;
		    	   nodeYPos = null;
		    	   nodeData = new LinkedHashMap<>();
		    	   inNode = true;
		    	   continue;
		       }
		       else if (line.equals("edge")) {
		    	   // Start a new edge
		    	   edgeSourceId = null;
		    	   edgeDestId = null;
		    	   inNode = false;
		    	   inEdge = true;
		    	   continue;
		       }
		       else if (line.isEmpty()) {
		    	   // Skip blank lines
		    	   continue;
		       }
		       else if (inNode) {
		    	   // Accumulate a node
		    	   String[] data = line.split(":");
		    	   if (data.length != 2) {
		    		   Logging.getLogger(LogType.ERROR).log(String.format("Line %d: Data lines must be of the format: <key>:<value>", lineNumber));
		    		   errors = true;
		    		   continue;
		    	   }
		    	   
		    	   String key = data[0].trim();
		    	   String value = data[1].trim();
		    	   switch (key) {
		    	   case "id":
		    		   nodeId = Integer.parseInt(value);
		    		   break;
		    	   case "label":
		    		   nodeLabel = value;
		    		   break;
		    	   case "group":
		    		   nodeGroup = Integer.parseInt(value);
		    		   break;
		    	   case "x":
		    		   nodeXPos = Double.parseDouble(value);
		    		   break;
		    	   case "y":
		    		   nodeYPos = Double.parseDouble(value);
		    		   break;
		    	   default:
		    		   nodeData.put(key, value);
		    		   break;
		    	   }
		    	   continue;
		       }
		       else if (inEdge) {
		    	   // Accumulate an edge
		    	   String[] data = line.split(":");
		    	   if (data.length != 2) {
		    		   Logging.getLogger(LogType.ERROR).log(String.format("Line %d: Data lines must be of the format: <key>:<value>", lineNumber));
		    		   errors = true;
		    		   continue;
		    	   }
		    	   
		    	   String key = data[0].trim();
		    	   String value = data[1].trim();
		    	   switch (key) {
		    	   case "source":
		    		   edgeSourceId = Integer.parseInt(value);
		    		   break;
		    	   case "dest":
		    		   edgeDestId = Integer.parseInt(value);
		    		   break;
		    	   }
		    	   continue;
		       }
		       else {
		    	   Logging.getLogger(LogType.ERROR).log(String.format("Line %d: Could not parse line: %s", lineNumber, line));
		    	   errors = true;
		       }
		    }
		} catch (IOException e) {
			Logging.getLogger(LogType.ERROR).log("Failed to read file: " + graphFile.getAbsolutePath(), e);
		}
		if (errors) {
			Logging.getLogger(LogType.ERROR).log("Errors when parsing file: " + graphFile.getAbsolutePath());
		}
		
		return graph;
	}
	
	public Collection<Node> getNodes() {
		return nodes;
	}
	
	public Collection<Map.Entry<Node, Node>> getEdges() {
		return edges.entries();
	}
	
	public void addNodes(Collection<Node> nodes) {
		this.nodes.addAll(nodes);
	}
	
	public void addEdge(Node source, Node destination) {
		edges.put(source, destination);
		inverseEdges.put(destination, source);
	}
	
	/**
	 * Perform a layout on this graph.
	 * Lay out the nodes in a circle.
	 */
	public void layoutCircular(double layoutRadius, double xOffset, double yOffset) {
		int nodeIndex = -1;
		for (Node node: nodes) {
			nodeIndex += 1;
			double layoutAngle = 2 * Math.PI * nodeIndex / nodes.size();
			double layoutX = layoutRadius * Math.cos(layoutAngle);
			double layoutY = layoutRadius * Math.sin(layoutAngle);
			node.setxPos(layoutX + xOffset);
			node.setyPos(layoutY + yOffset);
		}
	}
	
	/**
	 * Lay out each cluster separately in a circle.
	 */
	public void layoutCircularClusters() {
		double circleRadius = 10;
		double xSeparation = 50;
		double xOffset = 0;
		Collection<Set<Node>> clusters = getClusters();
		for (Set<Node> cluster: clusters) {
			Graph subgraph = getSubgraph(cluster);
			subgraph.layoutCircular(circleRadius * subgraph.getNodes().size(), 0, 0);
			subgraph.layoutShift(xOffset, 0);
			xOffset = subgraph.getMaxX() + xSeparation;
		}
	}
	
	/**
	 * Shift the current positions of the nodes, keeping their relative positions.
	 * @param xPadding minimum x position of any node.
	 * @param yPadding minimum y position of any node.
	 */
	public void layoutShift(double xPadding, double yPadding) {
		double minX = getMinX();
		double minY = getMinY();
		for (Node node: nodes) {
			if (node.getxPos() != null) {
				node.setxPos(node.getxPos() - minX + xPadding);
			}
			if (node.getyPos() != null) {
				node.setyPos(node.getyPos() - minY + yPadding);
			}
		}
	}
	
	public double getMinX() {
		double minX = Double.POSITIVE_INFINITY;
		for (Node node: nodes) {
			if (node.getxPos() < minX) {
				minX = node.getxPos();
			}
		}
		return minX;
	}
	
	public double getMaxX() {
		double maxX = Double.NEGATIVE_INFINITY;
		for (Node node: nodes) {
			if (node.getxPos() > maxX) {
				maxX = node.getxPos();
			}
		}
		return maxX;
	}
	
	public double getMinY() {
		double minY = Double.POSITIVE_INFINITY;
		for (Node node: nodes) {
			if (node.getyPos() < minY) {
				minY = node.getyPos();
			}
		}
		return minY;
	}
	
	public double getMaxY() {
		double maxY = Double.NEGATIVE_INFINITY;
		for (Node node: nodes) {
			if (node.getyPos() > maxY) {
				maxY = node.getyPos();
			}
		}
		return maxY;
	}
	
	/**
	 * @return The subgraph of this graph containing only the given nodes.
	 * Changes made to this subgraph will write through to this graph.
	 */
	private Graph getSubgraph(Collection<Node> nodes) {
		Graph subgraph = new Graph();
		subgraph.addNodes(nodes);
		for (Node source: nodes) {
			for (Node destination: edges.get(source)) {
				subgraph.addEdge(source, destination);
			}
		}
		return subgraph;
	}
	
	/**
	 * @return Subgraphs of this graph, one for each connected cluster in this graph.
	 * @param sorted If true, then return results sorted by cluster size.
	 */
	private Collection<Set<Node>> getClusters() {
		Collection<Set<Node>> clusters = new LinkedHashSet<>();
		List<Node> remainingNodes = new ArrayList<>();
		remainingNodes.addAll(nodes);
		while (!remainingNodes.isEmpty()) {
			Node nextNode = remainingNodes.remove(0);
			Set<Node> currentCluster = new LinkedHashSet<>();
			List<Node> openSet = new ArrayList<>();
			Set<Node> closedSet = new HashSet<>();
			openSet.add(nextNode);
			while (!openSet.isEmpty()) {
				Node currentNode = openSet.remove(0);
				currentCluster.add(currentNode);
				closedSet.add(currentNode);
				Set<Node> edgeNodes = new HashSet<>();
				edgeNodes.addAll(edges.get(currentNode));
				edgeNodes.addAll(inverseEdges.get(currentNode));
				for (Node edgeNode: edgeNodes) {
					if (!closedSet.contains(edgeNode)) {
						openSet.add(openSet.size(), edgeNode);
					}
				}
			}
			clusters.add(currentCluster);
		}
		return clusters;
	}
	
	/**
	 * Write out the diagram as a JSON file, for laying out with D3.
	 */
	public void writeJsonFile(File outputFile) {
		Logging.getLogger(LogType.PROGRESS).log("Creating diagram " + outputFile.getAbsolutePath());
		
		// Write the output file
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, false), "utf-8"))) {
			writer.write("{\n");
			writer.write("  \"nodes\": [\n");
			String separator = ""; // no commas allowed after last items in json lists
			for (Node node: nodes) {
				writer.write(separator);
				writer.write(String.format("    {\"id\": \"%d\", \"group\": %d", 
						node.getId(), node.getGroup(), node.getText()));
				if (node.getxPos() != null && node.getyPos() != null) {
					writer.write(String.format(", \"x\": %f, \"y\": %f", 
							node.getxPos(), node.getyPos()));
				}
				if (node.getText() != null) {
					writer.write(String.format(", \"name\": \"%s\"", node.getText()));
				}
				for (Entry<String,String> data: node.getData()) {
					writer.write(String.format(", \"%s\": \"%s\"", data.getKey(), data.getValue()));
				}
				writer.write("}");
				separator = ",\n";
			}
			writer.write("\n  ],\n");
			writer.write("  \"links\": [\n");
			
			separator = "";
			for (Map.Entry<Node, Node> edge: edges.entries()) {
				Node source = edge.getKey();
				Node destination = edge.getValue();
				writer.write(separator);
				writer.write(String.format("    {\"source\": \"%s\", \"target\": \"%s\", \"value\": 1}", 
						source.getId(), destination.getId()));
				separator = ",\n";
			}
			writer.write("\n  ]\n");
			writer.write("}\n");
			
			Logging.getLogger(LogType.PROGRESS).log("Diagram created.");
		} catch (IOException e) {
			// Failed to write to the file
			Logging.getLogger(LogType.ERROR).log("Problem writing to output file " + outputFile, e);
		}
	}
	
	/**
	 * Write out the diagram as a GRAPHML file, for importing into yEd.
	 */
	public void writeGraphmlFile(File outputFile) {
		Logging.getLogger(LogType.PROGRESS).log("Creating diagram " + outputFile.getAbsolutePath());
		
		// Write the output file
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, false), "utf-8"))) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
//					"<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"  \r\n" + 
//					"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
//					"    xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns\r\n" + 
//					"     http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">\r\n");
					"<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" \r\n"
					+ "    xmlns:java=\"http://www.yworks.com/xml/yfiles-common/1.0/java\" \r\n"
					+ "    xmlns:sys=\"http://www.yworks.com/xml/yfiles-common/markup/primitives/2.0\" \r\n"
					+ "    xmlns:x=\"http://www.yworks.com/xml/yfiles-common/markup/2.0\" \r\n"
					+ "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
					+ "    xmlns:y=\"http://www.yworks.com/xml/graphml\" \r\n"
					+ "    xmlns:yed=\"http://www.yworks.com/xml/yed/3\" \r\n"
					+ "    xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd\">\r\n");

			writer.write("  <key for=\"node\" id=\"data\" yfiles.type=\"nodegraphics\"/>\n");
			writer.write("  <key for=\"node\" id=\"extra-data\"/>\n");
			writer.write("  <graph id=\"G\" edgedefault=\"directed\">\n");
			for (Node node: nodes) {
				writer.write(String.format("    <node id=\"%d\" group=\"%d\"", 
						node.getId(), node.getGroup()));
				writer.write(">\n");
				writer.write(
						"      <data key=\"data\">  \r\n" + 
						"        <y:ShapeNode>  \r\n");
				// Position nodes if they have layout information
				if (node.getxPos() != null && node.getyPos() != null) {
					writer.write(String.format(
							"          <y:Geometry x=\"%f\" y=\"%f\" width=\"10.0\" height=\"10.0\" />\r\n",
							node.getxPos(), node.getyPos()));
				}
				// Add node text if necessary
				if (node.getText() != null) {
					writer.write(String.format(
							"          <y:NodeLabel>%s</y:NodeLabel> \r\n", node.getText()));
				}
				// Different styling for different node groups
				switch (node.getGroup()) {
				case 1: // Species (and species with the chosen tag)
					writer.write("          <y:BorderStyle color=\"#000000\" type=\"line\" width=\"1.0\"/>\r\n");
					writer.write("          <y:Fill color=\"#CCCCFF\" transparent=\"false\"/>\r\n");
					break;
				case 2: // Reactions
					writer.write("          <y:BorderStyle color=\"#000000\" type=\"line\" width=\"0.0\"/>\r\n");
					writer.write("          <y:Fill color=\"#FFFFFF\" transparent=\"false\"/>\r\n");
					break;
				case 3: // Species without the chosen tag (not used if no tag specified)
					writer.write("          <y:BorderStyle hasColor=\"false\"/>\r\n");
					writer.write("          <y:Fill hasColor=\"false\"/>\r\n");
					break;
				default:
					Logging.getLogger(LogType.WARNING).log(String.format(
							"Node with unknown group for styling in graphml: group=\"%d\"",
							node.getGroup()));
				}
				writer.write(
						"        </y:ShapeNode>\r\n"+
						"      </data>\r\n");
				
				// Save node group to file, for extracting later
				writer.write("      <data key=\"extra-data\">\r\n");
				writer.write(String.format("        <group>%d</group>\r\n", node.getGroup()));
				if (!node.getData().isEmpty()) {
					writer.write("        <node-data>\r\n");
					for (Entry<String,String> dataItem: node.getData()) {
						String key = dataItem.getKey();
						String value = dataItem.getValue().replace(">", "&gt;").replace("<", "&lt;");
						writer.write(String.format("          <%s>%s</%s>\r\n", key, value, key));
					}
					writer.write("        </node-data>\r\n");
				}
				writer.write("      </data>\r\n");

				writer.write("    </node>\n");
			}
			for (Map.Entry<Node, Node> edge: edges.entries()) {
				Node source = edge.getKey();
				Node destination = edge.getValue();
				writer.write(String.format("    <edge source=\"%s\" target=\"%s\"/>\n", 
						source.getId(), destination.getId()));
			}
			writer.write("  </graph>\n");
			writer.write("</graphml>\n");
			
			Logging.getLogger(LogType.PROGRESS).log("Diagram created.");
		} catch (IOException e) {
			// Failed to write to the file
			Logging.getLogger(LogType.ERROR).log("Problem writing to output file " + outputFile, e);
		}
	}
}
