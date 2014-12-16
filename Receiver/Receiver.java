import java.net.*; // Sockets and Datagram utils
import Utils.PacketAnalyzer;

public class Receiver {

  static int listeningPort = 65433;
  static int totalPacketLength = 32+32+32+512; // Destination address + Port and codes + SN + Payload

  public static void main(String[] args) throws Exception { // TODO Refine Exception catching

    // Initialize the UDP socket, listening on port 65433
    DatagramSocket socket = new DatagramSocket(listeningPort);

    byte[] receivedData = new byte[512];
    DatagramPacket datagram = new DatagramPacket(receivedData, receivedData.length);
    socket.receive(datagram);

    for (int i = 0; i < receivedData.length; i++ )
      System.out.print(receivedData[i] + " ");

    System.out.println("");

    return;
  }

}
