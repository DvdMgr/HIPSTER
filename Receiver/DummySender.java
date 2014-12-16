import java.net.*;  // Sockets and Datagram utils

public class DummySender {

  static int sendingPort = 65433;

  public static void main(String[] args) throws Exception {

    // Initialize the DatagramSocket
    DatagramSocket socket = new DatagramSocket();

    String message = "Here is the message";

    byte[] data = message.getBytes();

    InetAddress localhost = InetAddress.getLocalHost();
    socket.send(new DatagramPacket(data, data.length, localhost, sendingPort));

    return;
  }

}
