/*
 * This class abstracts the idea of a packet used in the HIPSTER protocol.
 * The goal is to  be able to inspect/modify an existing packet or create a
 * new one.
 * 
 * The packet can be created either from a DatagramPacket, in which case the
 * constructor is in charge of decoding the packet, or inserting the data 
 * manually, field by field.
 * 
 * To create a new packet from an existing datagram use:
 * 	HipsterPacket suchBeard = new HipsterPacket().fromDatagram(datagram);
 * 
 * To create a new packet and insert data manually:
 *	HipsterPacket moreEdgy = new HipsterPacket().
 * 	moreEdgy.setPayload(buffer); // Updates size automatically!
 * 	[...]
 * 
 * The data stored in this class can be inspected and modified at any time.
 * 
 * For info about the packet structure see Documentation/packet_structure
 */

package Utils;

import java.net.*;

public class HipsterPacket {
	/*
	 * Public constants
	 */
	public static final int DATA = 0;
	public static final int ACK  = 1;
	public static final int ETX  = 2;

	/*
	 * Private Variables
	 */
	private InetAddress destinationAddress;
	private int destinationPort;
	private int dataLength;
	private int code;
	private int sequenceNumber;
	private byte[] payload;
	
	/*
	 * Constructors
	 */
	/**
	 * Create a new packet with the default values (all set to 0)
	 */
	public HipsterPacket() {
		// java already initializes our data
	}
	/**
	 * Not a proper contstructor but a function thap parses a DatagramPacket
	 * and initializes the variables in this class accordingly.
	 * 
	 * @throws IllegalArgumentException
	 * 	If the packet is not properly formatted or cannot be parsed.
	 */
	public HipsterPacket fromDatagram(DatagramPacket packet) {
	
		return this;
	}

	/*
	 * Methods
	 */
	public int getDestinationPort() {
		return destinationPort;
	}
	/**
	 * DataLength is read-only.
	 * To avoid inconsistencies it is automatically updated when
	 * inserting/modifying the payload with the function setPayload()
	 */
	public int getDataLength() {
		return dataLength;
	}

	public boolean isRegularDatagram() {
		return (code == DATA);
	}

	public boolean isAck() {
		return (code == ACK);
	}

	public boolean isEtx() {
		return (code == ETX);
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public byte[] getPayload() {
		return payload;
	}
}
