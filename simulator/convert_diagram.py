#!/usr/bin/env python3

import xml.etree.ElementTree as ElTree
import sys

def tidyId(nodeId):
    """
    yEd adds a prefix of 'n' to node IDs.
    This function removes this prefix.
    """
    if nodeId.startswith('n'):
        nodeId = nodeId[1:]
    return nodeId

def convert(inputFileName, outputFileName):
    """
    Convert a yEd graphml file into our intermediate .graph representation.
    """
    it = ElTree.iterparse(inputFileName)
    for _, el in it:
        if '}' in el.tag:
            el.tag = el.tag.split('}', 1)[1]  # strip all namespaces
    root = it.root
    graph = root.find('graph')
    with open(outputFileName, 'w') as outputFile:
        # Find nodes
        for node in graph.findall('node'):
            outputFile.write("node\n")
            outputFile.write("  id: {0}\n".format(tidyId(node.attrib['id'])))
            for data in node.findall('data'):
                # Find node position
                shape = data.find('ShapeNode')
                if shape is not None:
                    geometry = shape.find('Geometry')
                    outputFile.write("  x: {0}\n".format(geometry.attrib['x']))
                    outputFile.write("  y: {0}\n".format(geometry.attrib['y']))
                    label = shape.find('NodeLabel')
                    if label is not None and label.text is not None:
                        outputFile.write("  label: {0}\n".format(label.text))
                # Find node group
                group = data.find('group')
                if group is not None:
                    outputFile.write("  group: {0}\n".format(group.text))
                # Find extra node data
                extras = data.find('node-data')
                if extras is not None:
                    for data in extras:
                        outputFile.write("  {0}: {1}\n".format(data.tag, data.text))
                # TODO
        # Find edges
        for edge in graph.findall('edge'):
            outputFile.write("edge\n")
            outputFile.write("  source: {0}\n".format(tidyId(edge.attrib['source'])))
            outputFile.write("  dest: {0}\n".format(tidyId(edge.attrib['target'])))
        # Write a final newline for the java parser
        outputFile.write("\n")
    
if __name__ == '__main__':
    inputFileName = sys.argv[1]
    outputFileName = sys.argv[2]
    convert(inputFileName, outputFileName)
