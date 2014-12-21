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
	private static final int PAYLOAD_SIZE = 512;

	// this socket is used by both threads
	private static DatagramSocket UDPSock;

	public static void main(String[] args) throws Exception {
		String fileName = "";
		String chAddress = "localhost";
		String dstAddress = "localhost";
		int dstPort = 4000;
		int myPort = 3000;

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
		System.out.println("Listening on port: " + myPort);
		InetAddress channel = InetAddress.getByName(chAddress);

		FileInputStream inFstream = new FileInputStream(inFile);

		byte[] buf = new byte[PAYLOAD_SIZE];
		int read = inFstream.read(buf);
		int sn = 0;
		while (read >= 0) {
			HipsterPacket pkt = new HipsterPacket();
			pkt.setCode(HipsterPacket.DATA);
			pkt.setPayload(Arrays.copyOf(buf, read));
			pkt.setDestinationAddress(InetAddress.getByName(dstAddress));
			pkt.setDestinationPort(dstPort);
			pkt.setSequenceNumber(sn);
			++sn;

			DatagramPacket datagram = pkt.toDatagram();
			datagram.setAddress(channel);
			datagram.setPort(CHANNEL_PORT);
			UDPSock.send(datagram);

			read = inFstream.read(buf);
			Thread.sleep(1);
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
		// cleanup
		inFstream.close();
  	}
}
