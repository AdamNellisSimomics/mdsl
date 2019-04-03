import os, os.path, sys, subprocess

def getAutoLogName():
    try: 
        maxNum = max(int(x[4:]) for x in os.listdir('.') if x.startswith('logs') and len(x) > 4)
        # Got some "logsX" directories
        logsName = 'logs' + str(maxNum + 1)
    except ValueError:
        if os.path.isdir('logs'):
            # Got first "logs" folder
            logsName = 'logs1'
        else:
            # No "logs" folders
            logsName = 'logs'
    return logsName

if __name__ == '__main__':
    
    # Read input file
    with open('inputs.txt', 'r') as inputFile:
        inputs = []
        for line in inputFile.readlines():
            if not line.strip().startswith('#'):
                inputs += line.strip().split(' ')
    
    # Auto-increase log directory number
    if '--auto-log-number' in sys.argv:
        logsName = getAutoLogName()
        sys.argv.remove('--auto-log-number')
        sys.argv.append('--log-dir')
        sys.argv.append(logsName)
    
    # Run simulator
    command = ['java', '-jar', 'simulator.jar'] + inputs + sys.argv[1:]
    process = subprocess.Popen(command)
    process.communicate()
