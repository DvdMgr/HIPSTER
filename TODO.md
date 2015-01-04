This is the TODO list for the project.

TODO:
- Add test files
- Implement dummy Sender (sends a file through the socket without listening for ACKs)    DUE 2014-12-21
- Implement dummy Channel (forwards each packet correctly to the receiver)               DUE 2014-12-21
- Implement dummy Receiver (receives and reconstructs the file)                          DUE 2014-12-21
- Consider using a checksum


DONE:
- ~~create folders for Client, Channel and Server (Davide)~~
  Superseeded by a single src/ directory (easier compilation)
- Performance analysis (Michele) first draft done
- ~~Flow charts (Paolo)~~
- ~~Define codes for the header (Paolo)~~
- ~~Implement HipsterPacket to create/parse datagrams (Michele)~~
- ~~Makefile (Paolo)~~