###################################################
# Makefile for the HIPSTER file transfer protocol
###################################################

all: sender channel receiver

sender: src/Sender.java src/HipsterPacket.java
	@javac -d . $?
	
channel: src/Channel.java src/HipsterPacket.java
	@javac -d . $?
	
receiver: src/Receiver.java src/HipsterPacket.java
	@javac -d . $?

clean:
	@rm -f *.class