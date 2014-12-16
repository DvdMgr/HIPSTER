/*
  This class will analyze datagrams, extracting information from the protocol-defined header.

  The same instance can be reused via the analyze(Datagram datagram) method, or multiple instances can be used
  at the same time. After running analyze or creating a new analyzer, the header can be inspected using the get
  methods.

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

  // Public constructor
  public PacketAnalyzer(DatagramPacket datagram) {

  }

  // Variables
  private int dataLength;
  private boolean regularDatagram;
  private boolean ack;
  private boolean etx;
  private int sequenceNumber;
  private byte[] data;
}
