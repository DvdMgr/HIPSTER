/*
 * Sender application for the HIPSTER protocol
 *
 * for usage see the USAGE variable.
 */

import java.net.*; // Socket and datagram stuff
import java.io.*;
import java.util.Arrays;

public class Sender {
	private static final String USAGE = "USAGE:\n\t" +
		"sender [-c channel_IP] [-d destination_IP:Port] [-p Port] input_file" +
		"\n\nBy default all addresses are 'localhost'.\n" +
		"The default port this program listens on is 3000.\n" +
		"The default port for the receiver is 4000.";

	private static final int CHANNEL_PORT = 65432;
	/*
	 * The following variables affect the sender's behaviour
	 * "Rule of thumb" (cit.)
	 */
	private static final int PAYLOAD_SIZE = 512; // Byte
	private static final int WINDOW_SIZE = 256;  // Packets
	private static final int ACK_TIMEOUT = 1500; // ms
	private static boolean MICHELE_MODE = true;

	// this socket is used by both threads
	private static DatagramSocket UDPSock;

	public static void main(String[] args) throws Exception {
		// those variables store the runtime options
		String fileName = "";
		String chAddress = "localhost";
		String dstAddress = "localhost";
		int dstPort = 4000;
		int myPort = 3000;
		// those variables are used for statistics
		int dataRead = 0; // total bytes read
		int dataSent = 0; // total bytes sent (including header)

		for(int i = 0; i < args.length; i++)
		{
			if ("-c".equals(args[i])) {
				// the next string is the channel address
				i++;
				chAddress = args[i];
			} else if ("-d".equals(args[i])) {
				// the next string is the destination address
				i++;
				String[] sep = args[i].split(":");
				dstAddress = sep[0];
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
		// initialize some data that will be used later
		UDPSock = new DatagramSocket(myPort);
		UDPSock.setSoTimeout(ACK_TIMEOUT);
		InetAddress channel = InetAddress.getByName(chAddress);
		FileInputStream inFstream = new FileInputStream(inFile);
		// everythig correctly initialized. Greet the user
		System.out.println("Listening on port: " + myPort);
		// take time into account (used for statistics)
		long startTime = System.currentTimeMillis();
		byte[] buf = new byte[PAYLOAD_SIZE];
		int read = inFstream.read(buf);
		int sn = 0;
		while (read >= 0) {
			dataRead += read;
			HipsterPacket pkt = new HipsterPacket();
			pkt.setCode(HipsterPacket.DATA);
			pkt.setPayload(Arrays.copyOf(buf, read));
			pkt.setDestinationAddress(InetAddress.getByName(dstAddress));
			pkt.setDestinationPort(dstPort);
			pkt.setSequenceNumber(sn);
			++sn;
			// to send an hipster packet convert it into a datagram and
			// set the destination port & address
			DatagramPacket datagram = pkt.toDatagram();
			datagram.setAddress(channel);
			datagram.setPort(CHANNEL_PORT);
			UDPSock.send(datagram);
			// TODO: take retransmission into account
			dataSent += read + HipsterPacket.headerLength;

			read = inFstream.read(buf);
			if (MICHELE_MODE == true)
				Thread.sleep(1);
			else if ((sn % WINDOW_SIZE == 0) || (read <= 0))
				processACKs();
		}
		// send an ETX packet to close the connection
		HipsterPacket pkt = new HipsterPacket();
		pkt.setCode(HipsterPacket.ETX);
		pkt.setDestinationAddress(InetAddress.getByName(dstAddress));
		pkt.setDestinationPort(dstPort);
		pkt.setSequenceNumber(sn);
		DatagramPacket etx = pkt.toDatagram();
		etx.setAddress(channel);
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

	private static void processACKs() throws IOException {
		for(int count = 0; count < WINDOW_SIZE; count++) {
			DatagramPacket aPacket = new DatagramPacket(
				new byte[HipsterPacket.headerLength],
				HipsterPacket.headerLength);
			try {
				UDPSock.receive(aPacket);
			} catch (SocketTimeoutException ste) {
				System.out.println("\nreceive() timed out");
				return; // there's no point to wait longer
			}
			HipsterPacket ack = new HipsterPacket().fromDatagram(aPacket);
			if (ack.isAck())
				System.out.print("\rACK for: " + ack.getSequenceNumber());
		}
	}
}
