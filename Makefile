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

report: tex/report.tex
	@pdflatex $?
clean:
	@rm -f *.class *.pdf *.log *.aux