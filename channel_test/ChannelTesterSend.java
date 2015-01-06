/*
 * Tester for the Channel, this class sends packets with a certain payload and
 * record the time at which they're sent. It also keeps a counter of the sent packets.
 * It is the same as a normal sender but without retransmission handling, in order to simplify the
 * computation of the delay introduced by the channel.
 * For usage see the USAGE variable.
 */

import java.net.*; // Socket and datagram stuff
import java.io.*;
import java.util.*; // Vector Arrays and others

public class ChannelTesterSend {
  private static final String USAGE = "USAGE:\n\t" +
    "sender [-d destination_Port] [-p Port] [-s payloadSize] [-dl]" +
    "\n\nBy default all addresses are 127.*.*.* (loopback).\n" +
    "The default port this program listens on is 3000.\n" +
    "The default port for the receiver is 4000. \n" +
    "The default payload size is 1000. \n" +
    "-dl is used to print the tx time of each packet";

  private static final int CHANNEL_PORT = 65432;
  /*
   * The following variables affect the sender's behaviour
   * "Rule of thumb" (cit.)
   */
  private static int PAYLOAD_SIZE = 1000; // Byte
  //watch out: the actual size of the payload of UDP at the Channel is PAYLOAD_SIZE + HIPSTER_HEADER
  private static int PAY_PACKETS = 5000;
  private static final int WINDOW_SIZE = 24;   // Packets
  private static boolean MICHELE_MODE = true;
  private static final int SENDER_PAUSE = 20;  // ms

  // runtime options. See USAGE variable
  private static String fileName = "";
  private static InetAddress chAddr = InetAddress.getLoopbackAddress();
  private static InetAddress dstAddr = chAddr; // localhost
  private static int dstPort = 4000;
  private static int myPort = 3000;
  private static boolean printDl = false;

  private static ListenerThread ackListener;

  public static void main(String[] args) throws Exception {
    // those variables are used for statistics
    int dataRead = 0; // total bytes read
    int dataSent = 0; // total bytes sent (including header)

    for(int i = 0; i < args.length; i++)
    {
       if ("-d".equals(args[i])) {
        // the next string is the destination address
        i++;
        dstPort = Integer.parseInt(args[i]);
      } else if ("-p".equals(args[i])) {
        // the next string is my port
        i++;
        myPort = Integer.parseInt(args[i]);
      } else if ("-s".equals(args[i])) {
        // the next string is the payload size
        i++;
        PAYLOAD_SIZE = Integer.parseInt(args[i]);
      } else if ("-dl".equals(args[i])) {
        // the test measures also the time of packet reception
        printDl = true;
      } else {
        // the current string is the source filename
        fileName = args[i];
      }
    }

    // initialize some data
    DatagramSocket UDPSock = new DatagramSocket(myPort);
    ackListener = new ListenerThread(UDPSock);
    ackListener.start();
    HashMap<Integer, DatagramPacket> packets = new HashMap<Integer,
      DatagramPacket>();
    // everythig correctly initialized. Greet the user
    System.out.println("Listening on port: " + myPort);
    // All the file has to be stored in memory for this algorithm to work
    byte[] buf = new byte[PAYLOAD_SIZE];

    int sn = 0;

    for(int i = 0; i < PAY_PACKETS; i++) {
      DatagramPacket datagram = craftPacket(buf, sn);
      packets.put(sn, datagram);
      ++sn;
    }
    System.out.println(sn);

    final int maxSN = sn; // the sender loop need to know when to stop
    System.out.println("Read: " + dataRead + " Bytes (" + (maxSN - 1) +
      " packets)");
    long startTime = System.currentTimeMillis(); // used for stats
    // send that data!!

    dataSent += sendAll(packets, maxSN, UDPSock);
    // we don't remove sent packets from the map
    // as we don't care about retransmissions

    // new ACKs will be handled here as the transmission is complete
    ackListener.stopIt();
    UDPSock.setSoTimeout(2500); // time to wait for the ack of ETX
    // send an ETX packet to close the connection
    boolean closed = false;

    HipsterPacket pkt = new HipsterPacket();
    pkt.setCode(HipsterPacket.ETX);
    pkt.setDestinationAddress(dstAddr);
    pkt.setDestinationPort(dstPort);
    pkt.setSequenceNumber(maxSN); // last one!
    DatagramPacket etx = pkt.toDatagram();
    etx.setAddress(chAddr);
    etx.setPort(CHANNEL_PORT);

    while (!closed) {
      UDPSock.send(etx);
      try {
        byte[] ackBuf = new byte[HipsterPacket.headerLength];
        DatagramPacket rec = new DatagramPacket(ackBuf,
          HipsterPacket.headerLength);
        UDPSock.receive(rec);

        HipsterPacket ack = new HipsterPacket().fromDatagram(rec);
        if ((ack.isAck()) && (ack.getSequenceNumber() == maxSN))
          closed = true;
      } catch (SocketTimeoutException soTomeout) {
        // do nothing
      }
    }
    // print the collected stats in a human readable manner
    long elapsed = System.currentTimeMillis() - startTime;
    long speed = dataSent / elapsed;
    double overhead = 100.0 * (dataSent - dataRead) / dataRead;
    System.out.printf("Bytes sent: %s (overhead %3.2f%%)\n", dataSent,
      overhead);
    System.out.println("Elapsed time: " + elapsed + "ms (" + speed +
      "KBps)");
    if(PAYLOAD_SIZE == 0) {
      System.out.println("Packet sent " + PAY_PACKETS);
    } else {
      System.out.println("Packet sent " + dataSent/(PAYLOAD_SIZE+HipsterPacket.headerLength));
    }
  }
  // returns the bytes sent
  private static int sendAll(Map<Integer, DatagramPacket> map, int max,
    DatagramSocket sock) throws IOException, InterruptedException
  {
    int sent = 0;
    int sentPkts = ackListener.acked.size(); //assume that all acked packets
    // up to now are sent

    for (int sn = 0; sn < max; sn++) {
      DatagramPacket datagram;
      datagram = map.get(sn);
      if (datagram == null)
        continue; // get another packet

      sock.send(datagram);
      if(printDl == true) {
      System.out.println("sn " + sn + " time " + System.currentTimeMillis());
      }
      sent += datagram.getLength();
      sentPkts++;

      if (MICHELE_MODE == true)
        Thread.sleep(SENDER_PAUSE);
      else if ((sentPkts - ackListener.acked.size()) > WINDOW_SIZE) {
        // busy-wait for the missing acks
        while ((ackListener.acked.size() - sentPkts) > 0)
        {/* do nothing */}
        //break; // start retransmission phase
      }
    }
    return sent;
  }

  private static DatagramPacket craftPacket(byte[] payload, int seqNum) {
    HipsterPacket pkt = new HipsterPacket();

    pkt.setCode(HipsterPacket.DATA);
    pkt.setPayload(payload);
    pkt.setDestinationAddress(dstAddr);
    pkt.setDestinationPort(dstPort);
    pkt.setSequenceNumber(seqNum);
    // to send an hipster packet convert it into a datagram and
    // set the destination port & address of the channel
    DatagramPacket ret = pkt.toDatagram();
    ret.setAddress(chAddr);
    ret.setPort(CHANNEL_PORT);

    return ret;
  }

}

/*
 * This thread is responsible for receiving processing acks
 */
class ListenerThread extends Thread {

  private DatagramSocket rSock;
  public Vector<Integer> acked;
  private boolean run = true;

  ListenerThread() {
    throw new IllegalArgumentException("I need a socket!");
  }

  ListenerThread(DatagramSocket theSocket) throws SocketException {
    rSock = theSocket;
    rSock.setSoTimeout(10);
    acked = new Vector<Integer>();
  }

  void stopIt() {
    run = false;
  }

  public void run() {
    while (run) { // can't stop this ta-ta-tata
      DatagramPacket received = new DatagramPacket(
        new byte[HipsterPacket.headerLength],
        HipsterPacket.headerLength);
      try {
        rSock.receive(received);
        HipsterPacket ack = new HipsterPacket().fromDatagram(received);
        if (ack.isAck())
          acked.add(ack.getSequenceNumber());
      } catch (SocketTimeoutException soex) {
        // this is something expected. just ignore it
      } catch (Exception ex) {
        System.out.println("WARNING: " + ex);
        System.out.println("The exception will be bravely ignored.");
      }
    }
  }
}
