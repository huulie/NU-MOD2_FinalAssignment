# NU-MOD2_FinalAssignment: FileTransfer server/client
Final assignment of Nedap University module 2

## Introduction
This repository contains a FileTransfer server (_to run locally on a laptop or remote on a Raspberry Pi_) and a FileTransfer client (_to run locally on laptop_). 

## Installation instruction

### FileTranser server 
#### locally
- download the FileTransferServer-1.0.0.jar and all other files with server in their names (see _releases_)
- start the server by excuting server (or on windows: server.bat)

#### on a Raspbarry Pi
- install the Pi according to these instructions: https://github.com/nedap/nu-module-2/blob/master/pi_setup/setup.md
- clone this repository to your local machine, connected to the same network as the Pi.
- edit the build.gradle: add the Pi's hostname or IP on the indicated line.
- now automatically deploy to the Pi with `./gradlew deploy` (on unix/linux systems)

## FileTransfer client
- download the FileTransferClient-1.0.0.jar and all other files with client in their names (see _releases_)
- start the server by excuting client (or on windows: client.bat)
