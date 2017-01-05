package com.example.android.P2P_MultiGroup.router;

/**
 * The echo packet structure
 *
 */
public class Packet {

	/**
	 * Different types of echo packets
	 *
	 */
	public enum TYPE {
		HELLO, HELLO_ACK, BYE, MESSAGE, UPDATE
	};

	private byte[] data;
	private Packet.TYPE type;
	private String receiverMac;
	private String senderMac;
	private String senderIP;
	private String ResourceIP;
	private String ResourceGoMac;
	private String ResourceMAC;
	private int ttl;

	/**
	 * constructor default TTL (3)
	 * @param type
	 * @param extraData
	 * @param receiverMac
	 * @param senderMac
	 */
	public Packet(int timetolive, Packet.TYPE type, byte[] extraData, String receiverMac, String senderMac,String ResourceIP,
				  String ResourceGoMac,String ResourceMAC ) {
		this.setData(extraData);
		this.setType(type);
		this.receiverMac = receiverMac;
		this.senderMac = senderMac;
		this.ResourceIP = ResourceIP;
		this.ResourceGoMac = ResourceGoMac;
		this.ResourceMAC = ResourceMAC;
		this.setTtl(timetolive);
		if (receiverMac == null)
			this.receiverMac = "00:00:00:00:00:00";
		if (senderMac == null)
			this.senderMac = "00:00:00:00:00:00";
		if (ResourceGoMac == null)
			this.ResourceGoMac = "00:00:00:00:00:00";
		if (ResourceMAC == null)
			this.ResourceMAC = "00:00:00:00:00:00";
		if (ResourceIP == null)
			this.ResourceIP = "0.0.0.0";
	}


	/**
	 * get the data (message body)
	 * @return
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * set the data (message body)
	 * @param data
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * the type of packet
	 * @return
	 */
	public Packet.TYPE getType() {
		return type;
	}

	/**
	 * set the type of packets
	 * @param type
	 */
	public void setType(Packet.TYPE type) {
		this.type = type;
	}

	/**
	 * Helper function to get a mac address string as bytes
	 * @param maca
	 * @return
	 */
	public static byte[] getMacAsBytes(String maca) {
		String[] mac = maca.split(":");
		byte[] macAddress = new byte[6]; // mac.length == 6 bytes
		for (int i = 0; i < mac.length; i++) {
			macAddress[i] = Integer.decode("0x" + mac[i]).byteValue();
		}
		return macAddress;
	}

	/**
	 * Helper function to get a byte array of data with an
	 * offset and use the next six bytes to make a MAC address string
	 * @param data
	 * @param startOffset
	 * @return
	 */
	public static String getMacBytesAsString(byte[] data, int startOffset) {
		StringBuilder sb = new StringBuilder(18);
		for (int i = startOffset; i < startOffset + 6; i++) {
			byte b = data[i];
			if (sb.length() > 0)
				sb.append(':');
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/**
	 * Serialize a packet according to the predefined structure
	 * @return
	 */
	public byte[] serialize() {

		// 6 bytes for mac
		byte[] sourceip = this.getResourceIP().getBytes();
		int sourceipLength = sourceip.length;
		byte[] serialized = new byte[1 + this.data.length + 24 +1 +1+sourceipLength];
		serialized[0] = (byte) type.ordinal();

		serialized[1] = (byte) ttl;
		serialized[2] = (byte) sourceipLength;

		byte[] mac = getMacAsBytes(this.receiverMac);

		for (int i = 3; i <= 8; i++) {
			serialized[i] = mac[i - 3];
		}
		mac = getMacAsBytes(this.senderMac);

		for (int i = 9; i <= 14; i++) {
			serialized[i] = mac[i - 9];
		}

		mac = getMacAsBytes(this.getResourceGoMac());

		for (int i = 15; i <= 20; i++) {
			serialized[i] = mac[i - 15];
		}

		mac = getMacAsBytes(this.getResourceMAC());

		for (int i = 21; i <= 26; i++) {
			serialized[i] = mac[i - 21];
		}

		for (int i = 27; i < 27+sourceipLength; i++) {
			serialized[i] = sourceip[i-27];
		}

		for (int i = 27+sourceipLength; i <serialized.length; i++) {
			serialized[i] = this.data[i -(27+sourceipLength)];
		}
		return serialized;
	}

	/**
	 * Deserialize a packet according to a predefined structure
	 * @param inputData
	 * @return
	 */
	public static Packet deserialize(byte[] inputData) {
		Packet.TYPE type = TYPE.values()[(int) inputData[0]];
		int timelive = (int) inputData[1];
		int iplength = (int) inputData[2];
		byte[] r = new byte[iplength];
		byte[] data = new byte[inputData.length - 27-iplength];
		String receivemac = getMacBytesAsString(inputData, 3);
		String sendermac = getMacBytesAsString(inputData, 9);
		String rego =getMacBytesAsString(inputData, 15);
		String remac =getMacBytesAsString(inputData, 21);

		for (int i = 27; i < 27+ iplength; i++) {
			r[i - 27] = inputData[i];
		}

		for (int i =  27+ iplength; i < inputData.length; i++) {
			data[i -  (27+ iplength)] = inputData[i];
		}
		return new Packet(timelive, type, data, receivemac, sendermac ,new String(r),rego,remac);
	}

	/**
	 * Get the receivers mac
	 * @return
	 */
	public String getMac() {
		return receiverMac;
	}

	/**
	 * Set the receivers mac
	 * @param mac
	 */
	public void setMac(String mac) {
		this.receiverMac = mac;
	}

	/**
	 * Get the sender's MAC
	 * @return
	 */
	public String getSenderMac() {
		return this.senderMac;
	}

	/**
	 * get the sender's IP
	 * @return
	 */
	public String getSenderIP() {
		return senderIP;
	}

	/**
	 * Set the sender's IP
	 * @param senderIP
	 */
	public void setSenderIP(String senderIP) {
		this.senderIP = senderIP;
	}



	public String getResourceIP() {
		return ResourceIP;
	}

	public void setResourceIP(String ResourceIP) {
		this.ResourceIP = ResourceIP;
	}


	public void setResourceMAC(String ResourceMAC) {
		this.ResourceMAC = ResourceMAC;
	}

	public String getResourceMAC() {
		return ResourceMAC;
	}


	public String getResourceGoMac() {
		return ResourceGoMac;
	}

	public void setResourceGoMac(String ResourceGoMac) {
		this.ResourceGoMac = ResourceGoMac;
	}

	/**
	 * Stringify a packet
	 */
	@Override
	public String toString() {
		return "Type" + getType().toString() + "receiver:" + getMac() + "sender:" + getSenderMac();
	}

	/**
	 * Get the TTL
	 * @return
	 */
	public int getTtl() {
		return ttl;
	}

	/**
	 * Set the TTL
	 * @param ttl
	 */
	public void setTtl(int ttl) {
		this.ttl = ttl;
	}
}
