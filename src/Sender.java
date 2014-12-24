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
	private static final int PAYLOAD_SIZE = 512; // Byte
	private static final int WINDOW_SIZE = 64;   // Packets
	private static final int ACK_TIMEOUT = 1500; // ms
	private static boolean MICHELE_MODE = true;
	private static final int SENDER_PAUSE = 20;  // ms

	// runtime options. See USAGE variable
	private static String fileName = "";
	private static InetAddress chAddr = InetAddress.getLoopbackAddress();
	private static InetAddress dstAddr = chAddr; // localhost
	private static int dstPort = 4000;
	private static int myPort = 3000;
	
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
		ListenerThread ackListener = new ListenerThread(UDPSock);
		ackListener.start();
		// everythig correctly initialized. Greet the user
		System.out.println("Listening on port: " + myPort);
		// need to load the file in memory

		// take time into account (used for statistics)
		long startTime = System.currentTimeMillis();
		byte[] buf = new byte[PAYLOAD_SIZE];
		int read = inFstream.read(buf);
		int sn = 0;
		while (read >= 0) {
			dataRead += read;
			DatagramPacket datagram;
			datagram = craftPacket(Arrays.copyOf(buf, read), sn);
			UDPSock.send(datagram);
			++sn;
			// TODO: take retransmission into account
			dataSent += read + HipsterPacket.headerLength;

			read = inFstream.read(buf);
			if (MICHELE_MODE == true)
				Thread.sleep(SENDER_PAUSE);
			//else if ((sn % WINDOW_SIZE == 0) || (read <= 0))
		}
		// send an ETX packet to close the connection
		HipsterPacket pkt = new HipsterPacket();
		pkt.setCode(HipsterPacket.ETX);
		pkt.setDestinationAddress(dstAddr);
		pkt.setDestinationPort(dstPort);
		pkt.setSequenceNumber(sn);
		DatagramPacket etx = pkt.toDatagram();
		etx.setAddress(chAddr);
		etx.setPort(CHANNEL_PORT);
		UDPSock.send(etx);
		dataSent += HipsterPacket.headerLength;
		// print the collected stats in a human readable manner
		long elapsed = System.currentTimeMillis() - startTime;
		long speed = dataSent / elapsed;
		double overhead = 100.0 * (dataSent - dataRead) / dataRead;
		System.out.println("Bytes read: " + dataRead);
		System.out.printf("Bytes sent: %s (overhead %3.2f%%)\n", dataSent,
			overhead);
		System.out.println("Elapsed time: " + elapsed + "ms (" + speed +
			"KBps)");

		// cleanup
		inFstream.close();
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

	ListenerThread() {
		throw new IllegalArgumentException("I need a socket!");
	}
	
	ListenerThread(DatagramSocket theSocket) {
		rSock = theSocket;
		acked = new Vector<Integer>();
	}

	public void run() {
		while (true) { // forever
			DatagramPacket received = new DatagramPacket(
				new byte[HipsterPacket.headerLength],
				HipsterPacket.headerLength);
			try {
				rSock.receive(received);
				HipsterPacket ack = new HipsterPacket().fromDatagram(received);
				if (ack.isAck()) {
					System.out.print("\rACK for: " +
						ack.getSequenceNumber());
					acked.add(ack.getSequenceNumber());
				}
			} catch (Exception ex) {
				System.out.println("WARNING: " + ex);
				System.out.println("The exception will be bravely ignored.");
			}
		}
	}
}
