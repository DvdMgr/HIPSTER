/**
* This Receiver only communicates with the channel. It needs to send datagrams that contain the Channel address in
* the UDP header and the Source address in the Hipster header in order for the channel to correctly forward these to
* the sender.
*/

import java.net.*;    // Sockets and Datagram utils
import java.util.*;   // Maps and useful stuff
import java.io.*;     // Support for writing files

// TODO Clarify addresses and ports for the channel and the source.
// TODO Decide whether to use a TreeMap or a HashMap followed by a TreeMap for sorting
// TODO Clean up this mess
public class Receiver {

  static String filename = "ReceivedFile";

  static int udpListenPort = 65433;                  // These two ports handle communication with the channel
  static int udpSendPort = 65432;

  static int hipsterListenPort = 65433;              // These two ports handle communication with the sender
  static int hipsterSendPort = 65431;

  static final int MAXIMUM_DATAGRAM_SIZE = 2048;    // This buffer will be trimmed according to the size of the datagram

  static InetAddress sourceAddress;                 // TODO Set these dynamically?
  static InetAddress channelAddress;

  private static final String USAGE = "USAGE:\n\t" +
  "Receiver [-c channel_IP] [-d destination_IP:Port] [-p Port] [-f output_file]" +
  "\n\nBy default all addresses are 'localhost'.\n" +
  "The default port this program listens on is 65433.\n" +
  "The default port for the receiver is 65431.";

  public static void main(String[] args) throws Exception { // TODO Refine Exception catching

    // By default these addresses are localhost.
    sourceAddress = InetAddress.getLocalHost();
    channelAddress = InetAddress.getLocalHost();

    // Parse command line arguments
    for(int i = 0; i < args.length; i++)
    {
      if ("-h".equals(args[i])) {
        System.out.println(USAGE);
        return;
      } else if ("-c".equals(args[i])) {
        // the next string is the channel address
        i++;
        channelAddress = InetAddress.getByName(args[i]);
      } else if ("-d".equals(args[i])) {
        // the next string is the destination address
        i++;
        String[] sep = args[i].split(":");
        sourceAddress = InetAddress.getByName(sep[0]);
        if (sep.length > 1) {
          hipsterSendPort = Integer.parseInt(sep[1]);
        }
      } else if ("-p".equals(args[i])) {
        // the next string is my port
        i++;
        udpListenPort = Integer.parseInt(args[i]);
      } else if ("-f".equals(args[i])) {
        // the next string is the filename
        i++;
        filename = args[i];
      }
    }

    // Initialize the UDP sockets
    DatagramSocket receiverSocket = new DatagramSocket(udpListenPort);
    DatagramSocket senderSocket = new DatagramSocket();

    // Prepare the map that will be used to store the file
    Map<Integer, byte[]> map = new HashMap<Integer, byte[]>();

    // This checklist is used to keep track of which packets we have received
    boolean[] checklist = new boolean[10];

    // Variables that keep track of the speed at which we receive data
    long startTime = System.currentTimeMillis();
    long dataReceived = 0;

    // This will be the last sequence number
    int etxSn = 0;

    /*
     * Start executing the algorithm
     */
    boolean waitingForData = true;

    while (waitingForData) {

      // Listen and wait for new data to arrive
      DatagramPacket datagram = listenOnSocket(receiverSocket);

      // Analyze the packet
      HipsterPacket hipsterPacket = new HipsterPacket();
      hipsterPacket = hipsterPacket.fromDatagram(datagram);
      int sn = hipsterPacket.getSequenceNumber();
      byte[] data = hipsterPacket.getPayload();
      dataReceived += datagram.getLength();       // Counter of the received bytes

      // Print sn of the received packet (debugging purposes)
      //System.out.println("Received packet of sequence number: " + sn);

      // Mark packet as received in the checklist
      if (sn < checklist.length) {
        checklist[sn] = true;
      } else {
        System.out.println("Expanding checklist...");
        boolean[] newchecklist = new boolean[2*checklist.length];
        System.arraycopy(checklist, 0, newchecklist, 0, checklist.length);
        checklist = newchecklist;
        checklist[sn] = true;
      }

      // Check whether the packet has the ETX flag
      if (hipsterPacket.isEtx()) {
        waitingForData = false;
        etxSn = sn;
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

    System.out.println("Bytes received: " + dataReceived);
    System.out.println("Elapsed time: " + elapsed + "ms (" + speed +
    "KBps)");

    // Print some debug info
    System.out.println("Etx packet had SN: " + etxSn);
    int storedMappings = map.size();
    System.out.println("Number of mappings stored: " + storedMappings);

    System.out.println(lostPackets + " packets lost in total");

    int lostPackets = 0;
    for (int i = 0; i < etxSn; i++)
      if (checklist[i] == false) {
        System.out.println("Packet " + i + " was not received");
        lostPackets++;
      }

    /*
     * Reorder and write output file
     */

    // Order elements by adding received data in a treemap
    Map<Integer, byte[]> orderedMap = new TreeMap<Integer, byte[]>();
    orderedMap.putAll(map);
    map = null;   // Free up memory space

    ByteArrayOutputStream stream = new ByteArrayOutputStream();

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
