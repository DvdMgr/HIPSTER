import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
* This class represents a Channel which receives a packet, decides if it should be dropped or
* forwarded with a random delay and forwards it to the end point of the connection, whose IP and
* port must be specified in first 6 bytes of the UDP payload.
* It listens on port 65432.
*/
public class Channel {

  //port numbers
  public static final int standardPort = 65432;
  public static final int forwardPort = 50000;

  //parameters tuned with "rule of thumb"
  private static final int NUMBER_THREADS = 200;
  private static final int BUFFSIZE = 2048; //arbitrary "big number"

  //runtime options, see USAGE variable
  private static boolean noDrop = false;
  private static boolean noDelay = false;
  private static boolean verbose = false;

  //counters
  private static int receivedCounter = 0;
  private static int droppedCounter = 0;

  private static final String USAGE = "USAGE:\n\t" +
  "Channel [-noDrop] [-noDelay] [-v]" +
  "\n\nBy default the Channel drops and delays.\n" +
  "If -noDrop flag is added the Channel doesn't drop packets.\n" +
  "If -noDelay flag is added the Channel doesn't add delay to packets. \n" +
  "If -v flag is added the Channel show some statistics on received and forwarded packets.";

  //private variables of a Channel object
  private DatagramSocket listenSock;
  private DatagramSocket forwardSock;
  private ScheduledExecutorService scheduler;

  public static void main(String[] args) {
    try {

      // Parse command line arguments
      for(int i = 0; i < args.length; i++)
      {
        if ("-h".equals(args[i])) {
          System.out.println(USAGE);
          return;
        } else if ("-noDrop".equals(args[i])) {
          // the next string is the channel address
          noDrop = true;
          System.out.println("The Channel won't drop any packet on purpose");
        } else if ("-noDelay".equals(args[i])) {
          // the next string is the destination address
          noDelay = true;
          System.out.println("The Channel won't delay any packet on purpose");
        } else if ("-v".equals(args[i])) {
          verbose = true;
          System.out.println("The Channel will show a counter for received packets");
        }
      }

      //create the listening and forwarding sockets
      DatagramSocket listenSocket = new DatagramSocket(standardPort);
      DatagramSocket forwardSocket = new DatagramSocket(forwardPort);

      //create the ThreadPool
      ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(NUMBER_THREADS);

      System.out.println("Butterhand Channel started - ready to receive and forward");

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


  /**
  * Public constructor
  */
  public Channel(DatagramSocket listen, DatagramSocket forward, ScheduledExecutorService scheduledThreadPool)
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
      receivedCounter++;

      //extract the data and compute the UDP payload length
      received = recPck.getData();
      int usefulLength = recPck.getLength() - recPck.getOffset();

      //decide if the packet is dropped or not
      if(!noDrop && isDropped(usefulLength)) {
        //do nothing
        droppedCounter++;
        if(verbose) { //print stats
          System.out.print("\rPackets received: " + receivedCounter + " dropped " + droppedCounter);
        }
      } else {
        if(verbose) { //print stats
          System.out.print("\rPackets received: " + receivedCounter + " dropped " + droppedCounter);
        }
        //forward the packet
        //extract forward address and port
        InetAddress fwAddr = InetAddress.getByAddress(Arrays.copyOfRange(received, 0, 4));
        int fwPort = twoBytesToInt(received[4], received[5]);

        //craft the to-be-forwarded packet
        byte[] toSend = Arrays.copyOfRange(received, recPck.getOffset(), recPck.getLength());
        recPck.setData(toSend);
        recPck.setPort(fwPort);
        recPck.setAddress(fwAddr);

        //compute random delay
        int delay = 0;
        if(!noDelay) {
          delay = (int) randExpDl(usefulLength); //the delay in ms is approximated to its floor
        }

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

  //Private utility methods

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
  * Param
  * length is the length in byte of the UDP packet
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
  * Param
  * length is the length in byte of the UDP packet
  */
  private boolean isDropped(int length) {
    double pLoss = 1 - Math.exp(-((double)length / 1024));
    if(Math.random() > pLoss)
      return false;
    else
      return true;
  }
}

/**
* Runnable class that takes care of forwarding packets
*/
class Forwarder implements Runnable {

  //private variables of a Forwarder object
  DatagramPacket toBeForwarded;
  DatagramSocket senderSocket;

  /**
  * Constructor which initializes a Forwarder object
  * The packet should contain the correct forwarding IP address and port number
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
    }
    catch(IOException e)
    {
      System.err.println("IOException in Forwarder.run()");
    }
  }
}
