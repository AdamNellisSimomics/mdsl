
# Runs the parse_mdsl.py script in Docker, and
#  copies the graphs to the host.
# Run this script in the Docker terminal on Windows.

docker build -t diagrams -f Dockerfile-diagrams .
docker run diagrams
rm -f ./out/*
rmdir ./out
docker cp $(docker ps -alq):/diagrams/out/ ./out/
