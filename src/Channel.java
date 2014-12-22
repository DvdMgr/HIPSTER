import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

//TODO test if the number of threads in the pool is enough. This is the number of threads that are
//TODO always in the pool, even if inactive, but if the number of added tasks become larger than the
//TODO number of threads the new tasks are queued and new threads aren't created. Thus this number //TODO should be large enough to host the delayed forwarding tasks
public class Channel {

  public static final int zanellaPort = 65432;
  public static final int forwardPort = 50000;
  public static final int NUMBER_THREADS = 200;

  public static final int BUFFSIZE = 2048; //FIXME arbitrary "big number"
  DatagramSocket listenSock;
  DatagramSocket forwardSock;
  ScheduledExecutorService scheduler;

  /**
  * Constructor
  */
  Channel(DatagramSocket listen, DatagramSocket forward, ScheduledExecutorService scheduledThreadPool)
  {
    listenSock = listen;
    forwardSock = forward;
    scheduler = scheduledThreadPool;
  }

  /**
  * Receive packets, extract from header the fwAddr and fwPort. Then pass a correctly formatted
  * packet to an instance of the Forwarder class which will take care of forwarding
  */
  public void getRequest()
  {
    try{
      //create a receiving buffer and wait for the packet
      byte[] received = new byte[BUFFSIZE];
      DatagramPacket recPck = new DatagramPacket(received, BUFFSIZE);
      listenSock.receive(recPck);
      System.out.println("Packet received from " + recPck.getPort());

      //extract the data and compute the UDP payload length
      received = recPck.getData();
      int usefulLength = recPck.getLength() - recPck.getOffset();

      //decide if the packet is dropped or not
      if(false) { //isDropped(usefulLength)) { //As acks are not taken into account by the sender
        //do nothing                           // ploss is set to 0
        System.out.println("Packet dropped!");
      }
      else { //forward the packet
        //extract forward address and port
        InetAddress fwAddr = InetAddress.getByAddress(Arrays.copyOfRange(received, 0, 4));
        int fwPort = twoBytesToInt(received[4], received[5]);

        //craft the to-be-forwarded packet
        byte[] toSend = Arrays.copyOfRange(received, recPck.getOffset(), recPck.getLength());
        recPck.setData(toSend);
        recPck.setPort(fwPort);
        recPck.setAddress(fwAddr);

        //compute random delay
        int delay = (int) randExpDl(usefulLength); //the delay is approximated to its floor

        //schedule the forwarding of a packet
        Forwarder forwarder = new Forwarder(recPck, forwardSock);
        scheduler.schedule(forwarder, delay, TimeUnit.MILLISECONDS);
      }
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
      //create the listening and forwarding sockets
      System.out.println("Butterhand Channel started - ready to receive and forward");
      DatagramSocket listenSocket = new DatagramSocket(zanellaPort);
      DatagramSocket forwardSocket = new DatagramSocket(forwardPort);

      //create the ThreadPool
      ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(NUMBER_THREADS);

      //receive Datagrams and perform the required tasks
      while(true) {
        Channel chan = new Channel(listenSocket, forwardSocket, scheduledThreadPool);
        chan.getRequest();
      }
    }
    catch(SocketException e)
    {
      System.err.println("SocketException in main");
    }


  }


  //Private methods

  /**
  * Combines two bytes in a int,
  * Param:
  * b1 the MS byte
  * b2 the LS byte
  */
  private int twoBytesToInt(byte b1, byte b2) {
    return (((b1 << 8) & 0xFF00) | (b2 & 0xFF));
  }

  /**
  * Computes the random waiting time for each packet to be forwarded,
  * as specified here
  * https://github.com/DvdMgr/HIPSTER/blob/master/Documentation/HW3-Protocol-design.pdf
  */
  private double randExpDl(int length) {
    double mean_rand_dl = 1024/Math.log(length);
    double rand_dl = - mean_rand_dl * Math.log(Math.random());
    return rand_dl;
  }

  /**
  * Computes the probability of dropping a packet, as specified here
  * https://github.com/DvdMgr/HIPSTER/blob/master/Documentation/HW3-Protocol-design.pdf
  * Returns true if the packet is dropped
  */
  private boolean isDropped(int length) {
    double pLoss = 1 - Math.exp(-((double)length / 1024));
    if(Math.random() > pLoss)
      return false;
    else
      return true;
  }
}

//Runnable class that takes care of forwarding packets
class Forwarder implements Runnable {

  DatagramPacket toBeForwarded;
  DatagramSocket senderSocket;


  /**
  * Constructor which initializes a Forwarder object
  */
  Forwarder(DatagramPacket pck, DatagramSocket sock) {
    toBeForwarded = pck;
    senderSocket = sock;
  }

  /**
  * Send the Datagram
  */
  public void run() {
    try {

      senderSocket.send(toBeForwarded);
      System.out.println("Packet forwarded after delay! to " + toBeForwarded.getPort());
    }
    catch(IOException e)
    {
      System.err.println("IOException in Forwarder.run()");
    }
  }


}
