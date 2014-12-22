/**
* This Receiver only communicates with the channel. It needs to send datagrams that contain the Channel address in
* the UDP header and the Source address in the Hipster header in order for the channel to correctly forward these to
* the sender.
*/

import java.net.*;    // Sockets and Datagram utils
import java.util.*;   // Maps and useful stuff
import java.io.*;

// TODO Clarify addresses and ports for the channel and the source.
// TODO Decide whether to use a TreeMap or a HashMap followed by a TreeMap for sorting
// TODO Clean up this mess
public class Receiver {

  static String filename = "File";

  static int udpListenPort = 65433;// 65433;                 // These two ports handle communication with the channel
  static int udpSendPort = 65432;//65432;

  static int hipsterListenPort = 65433;              // These two ports handle communication with the sender
  static int hipsterSendPort = 65431;

  static final int MAXIMUM_DATAGRAM_SIZE = 2048;    // FIXME Totally arbitrary "Big number"

  static InetAddress sourceAddress;                 // TODO Set these dynamically?
  static InetAddress channelAddress;

  public static void main(String[] args) throws Exception { // TODO Refine Exception catching

    // TODO Use nice args handling to set parameters
    if (args.length == 1)
      filename = args[0];

    sourceAddress = InetAddress.getByName("82.48.46.170"); //getLocalHost();     // TODO We should use the actual address here
    channelAddress = InetAddress.getByName("79.7.45.120"); //getLocalHost();

    // Initialize the UDP sockets
    DatagramSocket receiverSocket = new DatagramSocket(udpListenPort);
    DatagramSocket senderSocket = new DatagramSocket();

    // Prepare the map that will be used to store the file
    Map<Integer, byte[]> map = new HashMap<Integer, byte[]>();

    boolean[] checklist = new boolean[3180];

    long startTime = System.currentTimeMillis();
    long dataReceived = 0;

    int lastSn = 0;

    // Start executing the algorithm
    boolean waitingForData = true;

    while (waitingForData) {

      // Listen and wait for new data to arrive
      DatagramPacket datagram = listenOnSocket(receiverSocket);

      // Analyze the packet
      HipsterPacket hipsterPacket = new HipsterPacket();
      hipsterPacket = hipsterPacket.fromDatagram(datagram);
      int sn = hipsterPacket.getSequenceNumber();
      byte[] data = hipsterPacket.getPayload();
      dataReceived += datagram.getLength();

      // Print sn of the received packet
      System.out.println("Received packet of sequence number: " + sn);
      checklist[sn] = true;

      // Check whether the packet has the ETX flag
      if (hipsterPacket.isEtx()) {
        waitingForData = false;
        lastSn = sn;
      }
      else {
        // Add the packet to the map using the SN as key
        map.put(sn, data);
        //System.out.println("Added packet of sequence number: " + sn);
      }

      // Craft the ACK
      DatagramPacket ack = craftAck(sn);
      ack.setAddress(channelAddress);
      ack.setPort(udpSendPort);

      // Send the ACK
      senderSocket.send(ack);
    }

    // Calculate data arrival rate
    long elapsed = System.currentTimeMillis() - startTime;
    long speed = dataReceived / elapsed;

    System.out.printf("Bytes received: " + dataReceived);
    System.out.println("Elapsed time: " + elapsed + "ms (" + speed +
    "KBps)");

    int storedMappings = map.size();
    System.out.println("Number of mappings stored: " + storedMappings);

    for (int i = 0; i < lastSn; i++)
      if (checklist[i] == false)
        System.out.println("Packet " + i + " was not received");

    // We use ByteArrayOutputStream for testing
    // Use FileOutputStream for real application (remember that memory is limited)

    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    // Order by adding elements in a treemap
    Map<Integer, byte[]> orderedMap = new TreeMap<Integer, byte[]>();
    orderedMap.putAll(map);

    for(Map.Entry<Integer,byte[]> entry : orderedMap.entrySet()) {
      Integer integerKey = entry.getKey();
      byte[] value = entry.getValue();

      stream.write(value, 0, value.length);
    }

    // Write the stream to a file
    try{
      // Open stream
      FileOutputStream outputStream = new FileOutputStream (filename);

      // Write to file
      try{
        stream.writeTo(outputStream);
      }
      catch(Exception e) {
        System.out.println(e.getMessage());
      }
      // Close stream
      finally{
        outputStream.close();
      }
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }

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
    byte[] trimmedData = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getLength());
    datagram.setData(trimmedData);

    return datagram;
  }

  /**
    * This method creates an Hipster Ack using the specified sequence number.
    */
  private static DatagramPacket craftAck(int sequenceNumber) {

    HipsterPacket hipster = new HipsterPacket();
    hipster.setPayload(new byte[0]);                // Empty payload
    hipster.setDestinationAddress(sourceAddress);   // Source address
    hipster.setDestinationPort(hipsterSendPort);    // Source port number
    hipster.setCode(HipsterPacket.ACK);
    hipster.setSequenceNumber(sequenceNumber);

    return hipster.toDatagram();
  }

}
