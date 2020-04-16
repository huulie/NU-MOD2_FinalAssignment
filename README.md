# NU-MOD2_FinalAssignment: FileTransfer server/client
Final assignment of Nedap University module 2

## Introduction
This repository contains a FileTransfer server (_to run locally on a laptop or remote on a Raspberry Pi_) and a FileTransfer client (_to run locally on laptop_). 

## Installation instruction
Take care: this program  __only works if you have Java 11 installed__. Please install Java 11 if necessary.

_An "Unable to access jarfile" error may be solved by placing the file in another folder_

### FileTranser server 
#### locally
- download the FileTransferServer-1.0.0.jar and all other files with server in their names (see _releases_)
- start the server by navigating to the folder containing the FileTransferServer-1.0.0.jar. Then type: `java -jar FileTransferServer-1.0.0.jar` and answer the questions in the terminal.

#### on a Raspberry Pi
- install the Pi according to these instructions: https://github.com/nedap/nu-module-2/blob/master/pi_setup/setup.md
- clone this repository to your local machine, connected to the same network as the Pi.
- edit the build.gradle: add the Pi's hostname or IP on the indicated line.
- now automatically deploy to the Pi with `./gradlew deploy` (on unix/linux systems)

## FileTransfer client
- download the FileTransferClient-1.0.0.jar and all other files with client in their names (see _releases_)
- start the server by navigating to the folder containing the FileTransferClient-1.0.0.jar. Then type: `java -jar FileTransferClient-1.0.0.jar` and answer the questions in the terminal.
