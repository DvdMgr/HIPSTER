import java.net.*; // Sockets and Datagram utils
import Utils.*;

public class Receiver {

  static int listeningPort = 65433;
  static final int MAXIMUM_DATAGRAM_SIZE = 2048;    // Totally arbitrary "Big number"

  public static void main(String[] args) throws Exception { // TODO Refine Exception catching

    // TODO put this whole method in a cycle to implement the algorithm

    // Initialize the UDP socket, listening on the selected port
    DatagramSocket socket = new DatagramSocket(listeningPort);

    // Start listening for new data to arrive
    DatagramPacket datagram = listenOnSocket(socket);

    // TODO analyze the datagram via HipsterPacket

    System.out.println(new String(datagram.getData(), "UTF-8"));

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

}
