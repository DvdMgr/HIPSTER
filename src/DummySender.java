import java.net.*;  // Sockets and Datagram utils

// TODO This class needs to be cleaned up, commented and be improved in a lot of ways.
// TODO Experiment with multiple messages or files
public class DummySender {

  static int sendToPort = 65433;
  static final int MAXIMUM_DATAGRAM_SIZE = 2048;    // Totally arbitrary "Big number"

  public static void main(String[] args) throws Exception {

    // Initialize the DatagramSocket
    DatagramSocket socket = new DatagramSocket(65432);

    String message;

    if (args.length != 0)
      message = args[0];
    else message = "This is a test";

    InetAddress localhost = InetAddress.getLocalHost();

    HipsterPacket hipster = new HipsterPacket();
    hipster.setPayload(message.getBytes());
    hipster.setDestinationAddress(localhost);
    hipster.setDestinationPort(sendToPort);
    hipster.setCode(HipsterPacket.DATA);
    hipster.setSequenceNumber(1);

    DatagramPacket datagram = hipster.toDatagram();

    socket.send(datagram);

    // Create a generically big DatagramPacket
    byte[] receivedData = new byte[MAXIMUM_DATAGRAM_SIZE];
    DatagramPacket receivedDatagram = new DatagramPacket(receivedData, receivedData.length);

    // Listen to the socket
    socket.receive(receivedDatagram);

    System.out.println(new String(receivedDatagram.getData(), "UTF-8"));

    return;
  }

}
