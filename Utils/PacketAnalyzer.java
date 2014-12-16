/*
  This class will analyze the datagram payload, extracting information from the protocol-defined header.

  The same instance can be reused via the analyze method, or multiple instances can be used
  at the same time. After running analyze or creating a new analyzer, the header and the data can be
  inspected using the get methods.

  Here is the reference of the packet structure:

  0             15|16    26|27   31
  |-------------------------------|
  |     DESTINATION IP (ipv4)     |     // Destination:   0 to 31
  |-------------------------------|     // Dst Port:      32 to 47
  |   DST PORT    | LENGTH | CODE |
  |-------------------------------|     // Length:        48 to 57
  |        SEQUENCE NUMBER        |     // Code:          58 to 63
  |-------------------------------|     // SN:            64 to 95
  |                               |     // Data:          96 to (96+Length)
  |             DATA              |
  |                               |
  |-------------------------------|

  LENGTH is 10 bytes long, so the maximum payload length is 1024 Bytes

  CODES:
  0 -> Regular data packet
  1 -> ACK (sequence number is the same as the packet being ACKed)
  2 -> ETX (End of Transmission)

 */

package Utils;

import java.net.*;

public class PacketAnalyzer {

  /*
    Public constructors
  */

  // The empty constructor will return 0 for all ints and false for all booleans unless analyze is used
  public PacketAnalyzer() {

  }

  public PacketAnalyzer(byte[] packet) {
    analyze(packet);
  }

  // This method is used to analyze a new packet (using the same instance of the PacketAnalyzer)
  public analyze(byte[] packet) {
    // TODO This method has to fill in the values of the fields of the packet correctly
  }

  /*
    Getter methods
  */

  // TODO implement getDestinationAddress (what type should it return?)

  public int getDestinationPort() {
    return destinationPort;
  }

  public int getDataLength() {
    return dataLength;
  }

  public boolean isRegularDatagram() {
    return isRegularDatagram;
  }

  public boolean isAck() {
    return isAck;
  }

  public boolean isEtx() {
    return isEtx;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public byte[] getData() {
    return data;
  }

  /*
    Variables
  */

  // private InetAddress destinationAddress; // TODO Decide whether we want this in InetAddress form or something else
  private int destinationPort;
  private int dataLength;
  private boolean isRegularDatagram;
  private boolean isAck;
  private boolean isEtx;
  private int sequenceNumber;
  private byte[] data;
}
