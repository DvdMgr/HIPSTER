This is the TODO list for the project.

TODO:
- Makefile (Paolo)      // Add the Utils folder in the classpath, via -cp "HIPSTER/" -- Davide
- Implement PacketAnalyzer
- Implement Utils/PacketCreator (in order to correctly craft datagrams and the application layer header)
- Add test files
- Implement dummy Sender (sends a file through the socket without listening for ACKs)    DUE 2014-12-21
- Implement dummy Channel (forwards each packet correctly to the receiver)               DUE 2014-12-21
- Implement dummy Receiver (receives and reconstructs the file)                          DUE 2014-12-21
- Consider using a checksum


DONE:
- Create folders for Client, Channel and Server (Davide)
- Performance analysis (Michele) first draft done
- ~~Flow charts (Paolo)~~
- ~~Define codes for the header (Paolo)~~
