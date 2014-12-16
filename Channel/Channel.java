import java.net.*;
import java.io.*;

public class Channel {

  final int buffSize = 1024;
  DatagramSocket sock;
  byte[] received;
  byte[] toSend;

  InetAddress fwAddr;
  int fwPort;

  /*
  * Constructor
  */
  Channel(DatagramSocket s)
  {
    sock = s;
  }

  /*
  * Receive packets, extraxt from HIPSTER header the fwAddr and fwPort
  */
  public void getRequest()
  {
    try{
      received = new byte[buffSize];
      DatagramPacket recPck = new DatagramPacket(received, buffSize);
      sock.receive(recPck);
      received = recPck.getData();
      fwAddr = InetAddress.getByName(received[0] + "." + received[1] + "." + received[2] + "." + received[3]);
      fwPort = twoBytesToInt(received[3], received[4]);
    }
    catch(SocketException ex){
      System.err.println("SocketException in getRequest");
    }
    catch(IOException ex){
      System.err.println("IOException in getRequest");
    }
  }


  /*
  * Send the same payload as the received one to (fwAddr, fwPort)
  */
  public void sendResponde()
  {
    try{
      toSend = received;
      DatagramPacket sendPck = new DatagramPacket(toSend, buffSize, fwAddr, fwPort);
      sock.send(sendPck);
    }
    catch(SocketException ex){
      System.err.println("SocketException in sendResponse");
    }
    catch(IOException ex){
      System.err.println("IOException in sendResponse");
    }
  }


  public static void main(String[] args) {

    return;
  }

  /*
  * Combines two bytes in a int,
  * Param:
  * b1 the MS byte
  * b2 the LS byte
  */
  private int twoBytesToInt(byte b1, byte b2) {
    return (int) ((b1 << 8) | (b2 & 0xFF));
  }

}
