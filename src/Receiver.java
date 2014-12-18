import java.net.*;    // Sockets and Datagram utils
import java.util.*;   // Maps and useful stuff

// TODO Clarify addresses and ports for the channel and the source.
public class Receiver {

  static int listeningPort = 65433;                 // FIXME Magic numbers
  static int sendingPort = 65432;
  static final int MAXIMUM_DATAGRAM_SIZE = 2048;    // FIXME Totally arbitrary "Big number"
  static InetAddress sourceAddress;                 // TODO Set these dynamically?
  static InetAddress channelAddress;

  public static void main(String[] args) throws Exception { // TODO Refine Exception catching

    sourceAddress = InetAddress.getLocalHost(); // TODO We should use the actual address here
    channelAddress = InetAddress.getLocalHost();

    // Initialize the UDP sockets
    DatagramSocket receiverSocket = new DatagramSocket(listeningPort);
    DatagramSocket senderSocket = new DatagramSocket();

    // Prepare the map that will be used to store the file
    Map<Integer, byte[]> map = new TreeMap<Integer, byte[]>();

    // Start executing the algorithm
    boolean waitingForData = true;

    while (waitingForData) {

      // Start listening and wait for new data to arrive
      DatagramPacket datagram = listenOnSocket(receiverSocket);

      // Analyze the packet
      HipsterPacket hipsterPacket = new HipsterPacket();
      hipsterPacket = hipsterPacket.fromDatagram(datagram);
      int sn = hipsterPacket.getSequenceNumber();
      byte[] data = hipsterPacket.getPayload();

      // Check whether the packet has the ETX flag
      if (hipsterPacket.isEtx()) {
        waitingForData = true;
      }
      else {
        // Add the packet to the map using the SN as key
        map.put(sn, data);
      }

      // Craft the ACK
      DatagramPacket ack = craftAck(sn);
      ack.setAddress(channelAddress);
      ack.setPort(sendingPort);

      // Send the ACK
      senderSocket.send(ack);

      System.out.println(new String(hipsterPacket.getPayload(), "UTF-8"));

    }

    // TODO Reassemble the file

    return;
  }

  /**
    * Listen on the selected socket and return a properly trimmed DatagramPacket.
    *
    * Datagrams are received via a generic DatagramPacket that is then filled by the DatagramSocket. Hence, it is
    * necessary to create this packet with a big data buffer in order to ensure all of the data is received and that
    * there is no truncation.
    *
    * The datagram packet that is returned by this method has the data array of length equal to the nominal length
    * of its packet.
    */
  private static DatagramPacket listenOnSocket(DatagramSocket socket) throws Exception {  // TODO Handle exception

    // Create a generically big DatagramPacket
    byte[] receivedData = new byte[MAXIMUM_DATAGRAM_SIZE];
    DatagramPacket datagram = new DatagramPacket(receivedData, receivedData.length);

    // Listen to the socket
    socket.receive(datagram);

    // Trim the data array
    byte[] trimmedData = new String(datagram.getData(), datagram.getOffset(), datagram.getLength()).getBytes();
    datagram.setData(trimmedData);

    return datagram;
  }

  /**
    * This method creates an Hipster Ack using the specified sequence number.
    */
  private static DatagramPacket craftAck(int sequenceNumber) {

    HipsterPacket hipster = new HipsterPacket();
    hipster.setPayload(new byte[0]);                // Empty payload
    hipster.setDestinationAddress(sourceAddress);
    hipster.setDestinationPort(sendingPort);
    hipster.setCode(HipsterPacket.ACK);
    hipster.setSequenceNumber(sequenceNumber);

    return hipster.toDatagram();
  }

}
