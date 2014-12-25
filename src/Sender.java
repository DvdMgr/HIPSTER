/*
 * Sender application for the HIPSTER protocol
 *
 * for usage see the USAGE variable.
 */

import java.net.*; // Socket and datagram stuff
import java.io.*;
import java.util.*; // Vector Arrays and others

public class Sender {
	private static final String USAGE = "USAGE:\n\t" +
		"sender [-c channel_IP] [-d destination_IP:Port] [-p Port] input_file" +
		"\n\nBy default all addresses are 127.*.*.* (loopback).\n" +
		"The default port this program listens on is 3000.\n" +
		"The default port for the receiver is 4000.";

	private static final int CHANNEL_PORT = 65432;
	/*
	 * The following variables affect the sender's behaviour
	 * "Rule of thumb" (cit.)
	 */
	private static final int PAYLOAD_SIZE = 1000; // Byte
	private static final int WINDOW_SIZE = 24;   // Packets
	private static boolean MICHELE_MODE = false;
	private static final int SENDER_PAUSE = 1;  // ms

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
		HashMap<Integer, DatagramPacket> packets = new HashMap<Integer,
			DatagramPacket>();
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
		final int maxSN = sn; // the sender loop need to know when to stop
		System.out.println("Read: " + dataRead + " Bytes (" + (maxSN - 1) +
			" packets)");
		long startTime = System.currentTimeMillis(); // used for stats
		// send that data!!
		while (!packets.isEmpty()) {
			dataSent += sendAll(packets, maxSN, UDPSock);
			// remove acked packets from the map
			Vector<Integer> acked = ackListener.acked;
			int size = acked.size();
			for (int i = 0; i <	size; i++)
				packets.remove(acked.get(i));
		}
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
			dataSent += HipsterPacket.headerLength;
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
