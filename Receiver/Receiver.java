import java.net.*;

public class Receiver {

  // Stub method
  public static void main(String[] args) {

    // Initialize the UDP socket, listening on port 65432
    DatagramSocket socket = new DatagramSocket(65432);

    byte[] receivedData = new byte[6];
    DatagramPacket datagram = new DatagramPacket(receivedData, receivedData.length);
    socket.receive(datagram);

    for (int i = 0; i < receivedData.length; i++ )
    System.out.print(receivedData[i] + " ");

    return;
  }

}
