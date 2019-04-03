#!/usr/bin/env python

import socket
import re
import os
from run_simulation import getAutoLogName

# Standard socket stuff:
host = ""
port = 8444
print host
print port

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind((host, port))
sock.listen(1) # don't queue up any requests

# Loop forever, listening for requests:
while True:
    csock, caddr = sock.accept()
    print "Connection from: " + `caddr`
    req = csock.recv(1024) # get the request, 1kB max
    print req
    match = re.match('GET', req)
    if match:
        logName = getAutoLogName()
        os.system("python run_simulation.py --log-dir %s &" % (logName, ))
        print "Started simulation."
        csock.sendall("""HTTP/1.0 200 OK
Content-Type: application/json

{ 
"status" : "Started", 
"logsDirectory" : """
+logName+
""", 
}
""")
    
    else:
        # If there was no recognised command then return a 404 (page not found)
        print "Returning 404"
        csock.sendall("HTTP/1.0 404 Not Found\r\n")
    csock.close()
