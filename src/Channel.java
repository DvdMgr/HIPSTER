import java.net.*;
import java.io.*;
import java.util.*;

//TODO improve the overall quality
//TODO add p_drop and delay
public class Channel {

  final int buffSize = 1024; //FIXME arbitrary "big number"
  DatagramSocket listenSock;
  DatagramSocket forwardSock;

  byte[] received;
  byte[] toSend;

  InetAddress fwAddr;
  int fwPort;

  public static final int zanellaPort = 65432;
  public static final int forwardPort = 50000;


  /*
  * Constructor
  */
  Channel(DatagramSocket listen, DatagramSocket forward)
  {
    listenSock = listen;
    forwardSock = forward;
  }

  /*
  * Receive packets, extraxt from header the fwAddr and fwPort
  */
  public void getRequest()
  {
    try{
      received = new byte[buffSize];
      DatagramPacket recPck = new DatagramPacket(received, buffSize);
      listenSock.receive(recPck);
      System.out.println("Packet received! from" + recPck.getPort());
      received = recPck.getData();

      fwAddr = InetAddress.getByAddress(Arrays.copyOfRange(received, 0, 4));
      fwPort = twoBytesToInt(received[4], received[5]);

      toSend = Arrays.copyOfRange(received, recPck.getOffset(), recPck.getLength());
      recPck.setData(toSend);
      recPck.setPort(fwPort);
      recPck.setAddress(fwAddr);
      forwardSock.send(recPck);
      System.out.println(new String(recPck.getData(), "UTF-8"));
      System.out.println("Packet forwarded! to" + recPck.getPort());

    }
    catch(UnknownHostException e)
    {
      System.err.println("Bad formatted ip address in packet");
    }
    catch(SocketException ex){
      System.err.println("SocketException in getRequest");
    }
    catch(IOException ex){
      System.err.println("IOException in getRequest");
    }
  }



  public static void main(String[] args) {

    try {
      DatagramSocket listenSocket = new DatagramSocket(zanellaPort);
      DatagramSocket forwardSocket = new DatagramSocket(forwardPort);
      while(true) {
        Channel chan = new Channel(listenSocket, forwardSocket);
        chan.getRequest();
      }
    }
    catch(SocketException e)
    {
      System.err.println("SocketException in main");
    }


  }

  /*
  * Combines two bytes in a int,
  * Param:
  * b1 the MS byte
  * b2 the LS byte
  */
  private int twoBytesToInt(byte b1, byte b2) {
    return (int) (((b1 << 8) & 0xFF00) | (b2 & 0xFF));
  }

}
