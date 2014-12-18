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
		"sender -c channel_IP -d destination_IP:Port -p Port filename\n"
		+ "Tell him something about default values";

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
				dstAddress = args[i];
			} else if ("-p".equals(args[i])) {	
				// the next string is my port
				i++;
				myPort = Integer.parseInt(args[i]);
			} else {
				// the current string is the source filename
				fileName = args[i];
			}
		}
		System.out.println("filename = " + fileName);
		System.out.println("chAddress = " + chAddress);
		System.out.println("dstAddress = " + dstAddress);
		System.out.println("dstPort = " + dstPort);
		System.out.println("myPort = " + myPort);
		// the input file must be valid
		File inFile = new File(fileName);
		if ((!inFile.isFile()) || (!inFile.canRead())) {
			System.out.println("Invalid input file: " + fileName + "\n");
			System.out.println(USAGE);
		}
		// initialize some data that will be used later
		UDPSock = new DatagramSocket(myPort);
		InetSocketAddress dst = new InetSocketAddress(dstAddress,
	                                          dstPort);
		FileInputStream inFstream = new FileInputStream(inFile);

		byte[] buf = new byte[PAYLOAD_SIZE];
		int read = inFstream.read(buf);
		while (read >= 0) {
			HipsterPacket pkt = new HipsterPacket();
			pkt.setCode(HipsterPacket.DATA);
			pkt.setPayload(Arrays.copyOf(buf, read));
			pkt.setDestinationAddress(
				InetAddress.getByName(dstAddress));	
			pkt.setDestinationPort(dstPort);
			pkt.setSequenceNumber(42);

			UDPSock.send(pkt.toDatagram());
			read = inFstream.read(buf);
		}
		inFstream.close();
  	}
}
