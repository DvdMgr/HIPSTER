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
	@pdflatex $?
clean:
	@rm -f *.class *.pdf *.log *.aux *.zip

assessment: report
	@zip -j assessment.zip src/Sender.java src/Receiver.java \
		src/Channel.java src/HipsterPacket.java
	@rm -f *.class *.log *.aux
	@echo -e '\033[01;31mWell done! The best homework so far!\033[0m'