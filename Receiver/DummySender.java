import java.net.*;  // Sockets and Datagram utils

public class DummySender {

  public static void main(String[] args) throws Exception {

    // Initialize the DatagramSocket
    DatagramSocket socket = new DatagramSocket();

    byte[] data = {1, 2, 3, 4, 5, 6};
    InetAddress localhost = InetAddress.getLocalHost();
    socket.send(new DatagramPacket(data, data.length, localhost, 65432));

    return;
  }
  
}
