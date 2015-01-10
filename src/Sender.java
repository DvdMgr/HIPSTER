/*
 * Sender application for the HIPSTER protocol
 *
 * for usage see the USAGE variable.
 */

import java.net.*; // Socket and datagram stuff
import java.io.*;
import java.util.*; // Vector Arrays and others
import java.util.concurrent.*; // BlockingQueue and more

public class Sender {
	private static final String USAGE = "USAGE:\n\t" +
		"sender [-c channel_IP] [-d destination_IP:Port] [-p Port] input_file" +
		"\n\nBy default all addresses are 127.*.*.* (loopback).\n" +
		"The default port this program listens on is 3000.\n" +
		"The default port for the receiver is 4000.";

	private static final int CHANNEL_PORT = 65432;
	/*********************************************************
	 * The following variables affect the sender's behaviour *
	 * "Rule of thumb" (cit.)                                *
	 *********************************************************/
	private static final int PAYLOAD_SIZE = 1000; // Byte
	/*
	 * If more than this number of packets are still not acked then enter
	 * a slow phase that sends a packet every ACK.
	 */
	private static final int WINDOW_SIZE = 128;   // Packets
	/*
	 * When the window expire the sender starts a retransmission.
	 * Before starting a retransmission give the receiver some time to breath
	 * and empty its buffes. The amount of time to wait is controlled by this
	 * variable.
	 */
	private static final int WINDOW_WAIT = 15;	// ms
	/*
	 * Number of retries for sending the ETX packet
	 */
	private static final int ETX_RETRIES = 10;

	// runtime options. See USAGE variable
	private static String fileName = "";
	private static InetAddress chAddr = InetAddress.getLoopbackAddress();
	private static InetAddress dstAddr = chAddr; // localhost
	private static int dstPort = 4000;
	private static int myPort = 3000;

	private static ListenerThread ackListener;

	public static void main(String[] args) throws Exception {
		// those variables are used for statistics
		int dataRead = 0; // total bytes read
		int dataSent = 0; // total bytes sent (including header)

		for(int i = 0; i < args.length; i++)
		{
			if ("-c".equals(args[i])) {
				// the next string is the channel address
				i++;
				chAddr = InetAddress.getByName(args[i]);
			} else if ("-d".equals(args[i])) {
				// the next string is the destination address
				i++;
				String[] sep = args[i].split(":");
				dstAddr = InetAddress.getByName(sep[0]);
				if (sep.length > 1) {
					dstPort = Integer.parseInt(sep[1]);
				}
			} else if ("-p".equals(args[i])) {
				// the next string is my port
				i++;
				myPort = Integer.parseInt(args[i]);
			} else {
				// the current string is the source filename
				fileName = args[i];
			}
		}
		// the input file must be valid
		File inFile = new File(fileName);
		if ((!inFile.isFile()) || (!inFile.canRead())) {
			System.out.println("Invalid input file: " + fileName + "\n");
			System.out.println(USAGE);
			return;
		}
		// initialize some data
		DatagramSocket UDPSock = new DatagramSocket(myPort);
		FileInputStream inFstream = new FileInputStream(inFile);
		ackListener = new ListenerThread(UDPSock);
		ackListener.start();
		Map<Integer, DatagramPacket> packets = new
			HashMap<Integer, DatagramPacket>();
		// everythig correctly initialized. Greet the user
		System.out.println("Listening on port: " + myPort);
		// All the file has to be stored in memory for this algorithm to work
		byte[] buf = new byte[PAYLOAD_SIZE];
		int read = inFstream.read(buf);
		int sn = 0;
		while (read >= 0) {
			dataRead += read;
			DatagramPacket datagram;
			datagram = craftPacket(Arrays.copyOf(buf, read), sn);
			packets.put(sn, datagram);
			++sn;
			read = inFstream.read(buf);
		}
		inFstream.close();
		System.out.println("Read: " + dataRead + " Bytes (" + (sn - 1) +
			" packets)");
		long startTime = System.currentTimeMillis(); // used for stats
		// send that data!!
		dataSent += sendAll(packets, UDPSock, sn);
		// new ACKs will be handled here as the transmission is complete
		ackListener.stopIt();
		UDPSock.setSoTimeout(2500); // time to wait for the ack of ETX
		// send an ETX packet to close the connection
		boolean closed = false;
		int retries = ETX_RETRIES;

		HipsterPacket pkt = new HipsterPacket();
		pkt.setCode(HipsterPacket.ETX);
		pkt.setDestinationAddress(dstAddr);
		pkt.setDestinationPort(dstPort);
		pkt.setSequenceNumber(sn); // last one!
		DatagramPacket etx = pkt.toDatagram();
		etx.setAddress(chAddr);
		etx.setPort(CHANNEL_PORT);

		while ((!closed) && (retries > 0)) {
			UDPSock.send(etx);
			dataSent += HipsterPacket.headerLength;
			try {
				byte[] ackBuf = new byte[HipsterPacket.headerLength];
				DatagramPacket rec = new DatagramPacket(ackBuf,
					HipsterPacket.headerLength);
				UDPSock.receive(rec);

				HipsterPacket ack = new HipsterPacket().fromDatagram(rec);
				if ((ack.isAck()) && (ack.getSequenceNumber() == sn))
					closed = true;
			} catch (SocketTimeoutException soTomeout) {
				// do nothing
			}
			--retries;
			if (retries == 0)
				System.out.println("ETX timed out.");
		}
		// print the collected stats in a human readable manner
		long elapsed = System.currentTimeMillis() - startTime;
		long speed = dataSent / elapsed;
		double overhead = 100.0 * (dataSent - dataRead) / dataRead;
		System.out.printf("Bytes sent: %s (overhead %3.2f%%)\n", dataSent,
			overhead);
		System.out.println("Elapsed time: " + elapsed + "ms (" + speed +
			"KBps)");
	}

	/**
	* Send all the data that has to be sent. Also handles retransmissions.
	* Returns the raw number of bytes sent through the socket
	*/
	private static int sendAll(Map<Integer, DatagramPacket> packets,
	DatagramSocket sock, int maxSN) throws IOException, InterruptedException
	{
		int sent = 0;         // counter for the number of bytes sent
		int index = 0;        // index in the map of packets
		int lastMissing = 0; // the biggest missing packet
		BlockingQueue<Integer> acked = ackListener.acked;

		while (true) {
			while (!acked.isEmpty()) {
				int ackedSN = acked.take();
				packets.remove(ackedSN - 1);
				if (ackedSN >= maxSN) // we have finished!
					return sent;
				if(ackedSN > lastMissing)
					lastMissing = ackedSN;
				if(index < lastMissing)
					index = lastMissing;
			}

			if ((index - lastMissing) > WINDOW_SIZE)
			{ // slow down and retransmit
				Thread.sleep(WINDOW_WAIT); 
				index = lastMissing;
			}
			
			DatagramPacket datagram = packets.get(index);
			sock.send(datagram);
			++index;
			if (index == maxSN)
				index = lastMissing;
			sent += datagram.getLength();
		}
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
	public BlockingQueue<Integer> acked;
	private boolean run = true;

	ListenerThread() {
		throw new IllegalArgumentException("I need a socket!");
	}

	ListenerThread(DatagramSocket theSocket) throws SocketException {
		rSock = theSocket;
		rSock.setSoTimeout(20);
		acked = new LinkedBlockingQueue<Integer>();
	}

	void stopIt() {
		run = false;
		this.interrupt(); // STOP NOW!
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
					acked.put(ack.getSequenceNumber());
			} catch (SocketTimeoutException soex) {
				// this is something expected. just ignore it
			} catch (InterruptedException iex) {
				// also this is expected
			} catch (Exception ex) {
				System.out.println("WARNING: " + ex);
				System.out.println("The exception will be bravely ignored.");
			}
		}
	}
}
