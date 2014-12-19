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
 *	HipsterPacket moreEdgy = new HipsterPacket();
 * 	moreEdgy.setPayload(buffer); // Updates size automatically!
 * 	[...]
 *
 * To create a new datagram use
 * DatagramPacket datagr = suchBeard.toDatagram();
 *
 * The data stored in this class can be inspected and modified at any time.
 *
 * For info about the packet structure see Documentation/packet_structure
 */

import java.net.*;
import java.util.*;

public class HipsterPacket {
	/*
	 * Public constants
	 */
	public static final int DATA = 0;
	public static final int ACK  = 1;
	public static final int ETX  = 2;
	public static final int headerLength = 12;
	public static final int byteMask = 0xFF;

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
	 * Not a proper constructor but a function that parses a DatagramPacket
	 * and initializes the variables in this class accordingly.
	 *
	 * @throws IllegalArgumentException
	 * 	If the packet is not properly formatted or cannot be parsed.
	 */
	public HipsterPacket fromDatagram(DatagramPacket packet) {
		byte[] buffer = packet.getData();
		//the first four bytes are dst IP address
		try {
			setDestinationAddress(InetAddress.getByAddress(Arrays.copyOfRange(buffer, 0, 4)));
		}
		catch(UnknownHostException e)
		{
			throw new IllegalArgumentException();
		}

		//bytes 4 and 5 are port number
		setDestinationPort(((buffer[4] << 8) & 0xFF00) + (buffer[5] & 0xFF));

		//bit the last 6 bits of byte 7 are code
		setCode(buffer[7] & 0x3F);

		//bytes from 8 to 11 are the sequence number
		setSequenceNumber(toInt(Arrays.copyOfRange(buffer, 8, 12)));

		//the other bytes are the payload
		//check that dataLength is equal to the length of the payload
		//byte 6 and the first 2 msb of byte 7 are dataLength
		//otherwise throw an exception
		if(((buffer[6] << 2) + ((buffer[7] >> 6) & 0x0003)) == (buffer.length - headerLength)) {
			setPayload(Arrays.copyOfRange(buffer, headerLength, buffer.length));
		}
		else {
			throw new IllegalArgumentException();
		}
		return this;
	}

	/*
	 * Methods
	 */
	public int getDestinationPort() {
		return destinationPort;
	}

	public InetAddress getDestinationAddress() {
		return destinationAddress;
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


	/**
	* Sets the new payload and updates packet length
	*/
	public void setPayload(byte[] buffer) {
		payload = buffer;
		dataLength = payload.length;
	}

	public void setDestinationAddress(InetAddress dst) {
		destinationAddress = dst;
	}

	public void setDestinationPort(int dstPort) {
		destinationPort = dstPort;
	}

	public void setCode(int newCode) throws IllegalArgumentException {
		if(newCode < 0 || code > 2) {
			throw new IllegalArgumentException();
		}
		code = newCode;
	}

	public void setSequenceNumber(int sn) {
		sequenceNumber = sn;
	}

	/**
	* Returns the header of an HIPSTER packet in a correct format, see
	* https://github.com/DvdMgr/HIPSTER/blob/master/Documentation/packet_structure
	*/
	public byte[] getHeader() {
		byte[] header = new byte[headerLength];
		//bytes 0-3 are the dst ip
		byte[] ip = destinationAddress.getAddress();
		System.arraycopy(ip, 0, header, 0, ip.length);
		//bytes 4-5 are dst port
		header[4] = (byte) (destinationPort >> 8); //& byteMask);
		header[5] = (byte) destinationPort;// & byteMask;
		//byte 6 is the eight most significant bits of dataLength
		header[6] = (byte) (dataLength >> 2);// & byteMask;
		//byte 7 is the 2 remaining bits of dataLength and code
		int lsLength = (dataLength << 6) & byteMask;
		header[7] = (byte) (lsLength + code);// & byteMask;
		//bytes 8-11 are sequenceNumber
		byte[] sn = toBytes(sequenceNumber);
		System.arraycopy(ip, 0, header, 8, sn.length);
		return header;
	}

	/**
	* Returns a DatagramPacket ready to be sent to (dst IP, dst port) of this HipsterPacket
	* with a correctly formatted header and the payload of this HipsterPacket
	*/
	public DatagramPacket toDatagram() {
		byte[] buffer = new byte[headerLength + dataLength];
		//First headerLength bytes are the header
		System.arraycopy(this.getHeader(), 0, buffer, 0, headerLength);
		//In bytes from headerlength to dataLength + headerLength there's the payload
		System.arraycopy(payload, 0, buffer, headerLength, dataLength);
		DatagramPacket pck = new DatagramPacket(buffer, buffer.length, destinationAddress, destinationPort);
		return pck;
	}


	/**
	* Helper method, returns a byte[] which contains int i
	*/
	private byte[] toBytes(int i)
	{
		byte[] result = new byte[4];

		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i /*>> 0*/);

		return result;
	}

	/**
	* Helper method, returns an int from a byte[]
	*/
	private int toInt(byte[] bytes) {
		int value = ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16)
		| ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
		return value;
	}
}
