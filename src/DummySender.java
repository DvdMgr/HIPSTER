import java.net.*;  // Sockets and Datagram utils
import java.util.*;

// TODO This class needs to be cleaned up, commented and improved in a lot of ways.
// TODO Experiment with multiple messages or files
public class DummySender {

  static int sendToPort = 65433;
  static int channelPort = 65432;
  static int listenToPort = 65431;
  static final int MAXIMUM_DATAGRAM_SIZE = 2048;    // Totally arbitrary "Big number"

  public static void main(String[] args) throws Exception {

    // XXX Make the sender send multiple messages (maybe let the user enter them until a special key is entered)

    // Initialize the DatagramSocket
    DatagramSocket socket = new DatagramSocket(listenToPort);

    String message;

    if (args.length != 0)
      message = args[0];
    else message = "This is a test";
    int code = Integer.parseInt(args[1]);
    int sn = Integer.parseInt(args[2]);

    InetAddress localhost = InetAddress.getLocalHost();

    HipsterPacket hipster = new HipsterPacket();
    hipster.setPayload(message.getBytes());
    hipster.setDestinationAddress(localhost);
    hipster.setDestinationPort(sendToPort);
    switch (code) {
      case 0:
        hipster.setCode(HipsterPacket.DATA);
        break;
      case 1:
        hipster.setCode(HipsterPacket.ACK);
        break;
      case 2:
        hipster.setCode(HipsterPacket.ETX);
        break;
    }
    hipster.setSequenceNumber(sn);

    DatagramPacket datagram = hipster.toDatagram();

    datagram.setPort(channelPort);
    datagram.setAddress(localhost);

    socket.send(datagram);

    // Create a generically big DatagramPacket
    byte[] receivedData = new byte[MAXIMUM_DATAGRAM_SIZE];
    DatagramPacket receivedDatagram = new DatagramPacket(receivedData, receivedData.length);

    // Listen to the socket
    socket.receive(receivedDatagram);

    // Trim the datagram
    byte[] trimmedData = Arrays.copyOfRange(receivedDatagram.getData(), receivedDatagram.getOffset(), receivedDatagram.getLength());
    receivedDatagram.setData(trimmedData);

    // Analyze packet
    HipsterPacket pck = new HipsterPacket();
    pck.fromDatagram(receivedDatagram);

    // Print packet info
    System.out.println("Received ACK info");
    System.out.println(pck.getDestinationAddress());
    System.out.println(pck.getDestinationPort());
    System.out.println("Is ack? " + pck.isAck());
    System.out.println(pck.getCode());
    System.out.println(pck.getSequenceNumber());
    System.out.println("Contents of the ACK (there shouldn't be any payload):" + new String(pck.getPayload(), "UTF-8"));

    return;
  }

}
