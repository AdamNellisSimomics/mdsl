
#
# Run Antlr first, before running this script:
# java -jar antlr-4.5.3-complete.jar -Dlanguage=Python3 -o generated-src/main/python/ src/main/antlr/MembraneModel.g4
#

import os
import sys
import antlr4
import random
import itertools
import graph_tool.all as gt
from collections import defaultdict

# Import generated code
sys.path.insert(1, 'generated-src/main/python/')
from MembraneModelLexer import MembraneModelLexer
from MembraneModelListener import MembraneModelListener
from MembraneModelParser import MembraneModelParser


class TagReadingListener(MembraneModelListener):
    """
    Pulls out the species tags from an MDSL file.
    Species tags can be accessed from self.tags after walking.
    """
    
    def __init__(self):
        self.inSpecies = False # Whether or not we are in a species definition
        self._tags = set()
        self._speciesPerTag = defaultdict(set)
    
    def enterSpeciesDefinition(self, ctx):
        self.inSpecies = True
        self.currentSpeciesName = ctx.name.text
        
    def exitSpeciesDefinition(self, ctx):
        self.inSpecies = False
    
    def exitTag(self, ctx):
        if self.inSpecies:
            tag = ctx.value.text
            if tag.startswith("'") and tag.endswith("'"):
                tag = tag[1:-1]
            self._tags.add(tag)
            self._speciesPerTag[tag].add(self.currentSpeciesName)
    
    @property
    def tags(self):
        return sorted(list(self._tags))
    
    @property
    def speciesPerTag(self):
        return self._speciesPerTag


class GraphMakingListener(MembraneModelListener):
    """
    Converts an MDSL file into a graph.
    """
    
    def __init__(self, species=None):
        """
        @param species: restrict the graph to focus on the given species. Defaults to all species.
        """
        self.focusSpecies = species
        self.inLeftHandSide = False # Whether we are on the left hand side of a reaction or the right hand side
        self.inRightHandSide = False
        self._reactions = []
    
    def enterReactionDefinition(self, ctx):
        self.inLeftHandSide = True
        self.inRightHandSide = False
        self.currentConsumed = set()
        self.currentProduced = set()
        
    def enterEquation(self, ctx):
        self.inLeftHandSide = False
        self.inRightHandSide = False
        
    def exitArrow(self, ctx):
        self.inLeftHandSide = False
        self.inRightHandSide = True
        
    def exitReactionDefinition(self, ctx):
        self.inLeftHandSide = False
        self.inRightHandSide = False
        # Only add reactions involving the species we are focussing on
        if self.focusSpecies == None or (self.focusSpecies & self.currentConsumed) or (self.focusSpecies & self.currentProduced):
            if not self.currentProduced:
                self.currentProduced.add('decay')
            self._reactions.append((self.currentConsumed, self.currentProduced))
    
    def exitLocatedSpecies(self, ctx):
        if self.inLeftHandSide:
            self.currentConsumed.add(ctx.species.text)
        elif self.inRightHandSide:
            self.currentProduced.add(ctx.species.text)
    
    @property
    def reactions(self):
        """
        @return list of reactions as pairs of (consumedSpecies, producedSpecies)
        """
        return self._reactions


class MdslFile():
    """
    A parsed MDSL file, from which we can generate graphs.
    """
    
    def __init__(self, mdslFilePath):
        print('Parsing MDSL file: ' + mdslFilePath)
        input = antlr4.FileStream(mdslFilePath, encoding='utf-8')
        lexer = MembraneModelLexer(input)
        stream = antlr4.CommonTokenStream(lexer)
        self.parser = MembraneModelParser(stream)
        self.tree = self.parser.model()
    
        # Parse tags 
        tagReader = TagReadingListener()
        walker = antlr4.ParseTreeWalker()
        walker.walk(tagReader, self.tree)
        self.tags = tagReader.tags
        self.speciesPerTag = tagReader.speciesPerTag
    
    def writeJsonGraph(self, outputFilePath, tags=None):
        """
        Write out the graph represented by this MDSL file.
        @param tags (optional) Restrict to the given tags. Defaults to all tags.
        """
        print('Creating graph: ' + outputFilePath)
        # Work out which species to focus on
        if tags == None:
            species = None
        else:
            species = set()
            for tag in tags:
                species.update(self.speciesPerTag[tag])
        # Create the graph
        graphMaker = GraphMakingListener(species)
        walker = antlr4.ParseTreeWalker()
        walker.walk(graphMaker, self.tree)
        # Create focus nodes
        if species == None:
            species = set()
            for (speciesConsumed, speciesProduced) in graphMaker.reactions:
                species.update(speciesConsumed)
                species.update(speciesProduced)
        currentNodeId = -1
        focusNodes = []
        focusNodeIds = {}
        for speciesName in species:
            currentNodeId += 1
            focusNodeIds[speciesName] = currentNodeId
            focusNodes.append({'id': str(currentNodeId), 'name': speciesName, 'group': 1})
        # Create reaction nodes
        reactionNodes = []
        for reaction in graphMaker.reactions:
            currentNodeId += 1
            reactionNodes.append({'id': str(currentNodeId), 'group': 2})
        # Create edges and create additional species nodes as necessary
        edges = []
        additionalSpeciesNodes = []
        for (speciesConsumed, speciesProduced), reactionNode in itertools.zip_longest(graphMaker.reactions, reactionNodes):
            for speciesName, consumedOrProduced in itertools.chain(
                    ((speciesName, 'c') for speciesName in speciesConsumed),
                    ((speciesName, 'p') for speciesName in speciesProduced)):
                if speciesName in species:
                    # This is a focus species, so re-use its node
                    speciesNodeId = focusNodeIds[speciesName]
                else:
                    # This is an additional species, so create a new node
                    currentNodeId += 1
                    speciesNodeId = currentNodeId
                    additionalSpeciesNodes.append({'id': str(currentNodeId), 'name': speciesName, 'group': 3})
                if consumedOrProduced == 'c':
                    # This is a consumed species, so edge goes from species to reaction
                    edges.append({'source': str(speciesNodeId), 'target': reactionNode['id']})
                else:
                    # This is a produced species, so edge goes from reaction to species
                    edges.append({'source': reactionNode['id'], 'target': str(speciesNodeId)})
        # Lay out the graph
        allNodes = focusNodes + reactionNodes + additionalSpeciesNodes
        g = gt.Graph()
        g.add_vertex(len(allNodes))
        for edge in edges:
            g.add_edge(g.vertex(edge['source']), g.vertex(edge['target']))
        #positions = gt.fruchterman_reingold_layout(g)
        try:
            # If the graph is planar, then lay out with no edge crossings to start with
            positions = gt.planar_layout(g)
        except ValueError:
            positions = None
        positions = gt.sfdp_layout(g, 
            K = 150, # optimal edge length
            pos = positions,
        )
        # Get the node positions out of the layout algorithm's data structure
        [x_coords, y_coords] = positions.get_2d_array([0,1])
        for node, x_coord, y_coord in itertools.zip_longest(allNodes, x_coords, y_coords):
            node['x'] = x_coord
            node['y'] = y_coord
        # Write out the json file
        with open(outputFilePath, 'w') as outFile:
            outFile.write("{\n")
            outFile.write("  \"nodes\": [\n")
            separator = ''
            for node in itertools.chain(focusNodes, reactionNodes, additionalSpeciesNodes):
                outFile.write(separator)
                separator = ",\n"
                outFile.write("    {")
                separator2 = ''
                for property in ('id', 'group', 'x', 'y', 'name', 'mdsl_line_number', 'mdsl_line'):
                    if property in node:
                        outFile.write(separator2)
                        separator2 = ', '
                        if isNumber(node[property]):
                            # Don't quote numbers
                            outFile.write('"{property}": {value}'.format(property=property, value=node[property]))
                        else:
                            # Quote strings
                            outFile.write('"{property}": \"{value}\"'.format(property=property, value=node[property]))
                outFile.write("}")
            outFile.write("\n")
            outFile.write("  ],\n")
            outFile.write("  \"links\": [\n")
            separator = ''
            for edge in edges:
                outFile.write(separator)
                separator = ",\n"
                outFile.write("    {{\"source\": \"{source}\", \"target\": \"{target}\"}}".format_map(edge))
            outFile.write("\n")
            outFile.write("  ]\n")
            outFile.write("}\n")


def isNumber(x):
    try:
        x + 1
        return True
    except TypeError:
        return False

def main(mdslFilePath, outputDir):
    # Create output dir if necessary
    if not os.path.exists(outputDir):
        os.mkdir(outputDir)
    
    # Parse MDSL file
    mdslFile = MdslFile(mdslFilePath)
    
    # Create graph for all tags
    mdslFile.writeJsonGraph(os.path.join(outputDir, "all tags.json"))
    
    # Create a separate graph for each tag
    for tag in mdslFile.tags:
        mdslFile.writeJsonGraph(os.path.join(outputDir, tag + ".json"), [tag])
    

def test(mdslFilePath, outputDir, tag):
    mdslFile = MdslFile(mdslFilePath)
    mdslFile.writeJsonGraph(os.path.join(outputDir, tag + ".json"), [tag])

if __name__ == '__main__':
    main(mdslFilePath = os.path.abspath(os.path.join('src', 'test', 'resources', 'model_files', 'spleenV2.mdsl')),
         outputDir = 'out')
    #test(mdslFilePath = os.path.abspath(os.path.join('src', 'test', 'resources', 'model_files', 'spleenV2.mdsl')),
    #     outputDir = 'out',
    #     tag='parasite')
