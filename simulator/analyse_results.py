# To be run after the simulator has finished.
# Expects to be run from the logs directory.

import numpy
import matplotlib.pyplot as plt

def makeGraph():
    print('Creating graph...')
    
    with open('output_Species.csv') as f:
        # Get headings from first line
        headings = [ entry.strip() for entry in f.readline().split(',')[1:] ]
        print('Headings: ' + ', '.join(headings))
        # Read in time series data
        lines = numpy.zeros((10, len(headings)))
        rowNum = 0
        for line in f.readlines():
            columns = [ entry.strip() for entry in line.split(',')[1:] ]
            lines[rowNum] = columns
            rowNum += 1
            # Grow array as needed
            if (rowNum >= lines.shape[0]):
                lines.resize((rowNum*2, len(headings)))
        # Shrink array to remove unused portion
        lines.resize((rowNum, len(headings)))
        # Create the plot
        fig = plt.figure(1)
        ax = fig.add_subplot(111)
        # Time is the last column, and goes on the x axis
        xLabel = headings[-1]
        xValues = lines[:,-1]
        ax.set_xlabel(xLabel)
        # Create a different colour for each line
        hsv = plt.get_cmap('hsv')
        colours = hsv(numpy.linspace(0, 1.0, lines.shape[1]))
        # Plot a line for each species
        for columnNum in range(0, lines.shape[1]-1):
            yValues = lines[:,columnNum]
            ax.plot(xValues, yValues, label=headings[columnNum], color=colours[columnNum])
        legend = ax.legend(bbox_to_anchor=(1.05, 1), loc=2, borderaxespad=0.)
        # Save plot as a png file
        plt.savefig('output_Species.png', bbox_extra_artists=(legend,), bbox_inches='tight')
        print('Graph saved.')

if __name__ == '__main__':
    makeGraph()
