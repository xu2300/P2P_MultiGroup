package com.example.android.P2P_MultiGroup.router;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.example.android.P2P_MultiGroup.config.Configuration;
import com.example.android.P2P_MultiGroup.router.tcp.TcpSender;

/**
 * Responsible for sending all packets that appear in the queue
 *
 */
public class Sender implements Runnable {

	/**
	 * Queue for packets to send
	 */
	private static ConcurrentLinkedQueue<Packet> ccl;
	/**
	 * Constructor
	 */
	public Sender() {
		if (ccl == null)
			ccl = new ConcurrentLinkedQueue<Packet>();
	}

	/**
	 * Enqueue a packet to send
	 * @param p
	 * @return
	 */
	public static boolean queuePacket(Packet p) {
		if (ccl == null)
			ccl = new ConcurrentLinkedQueue<Packet>();
		return ccl.add(p);
	}

	@Override
	public void run() {
		TcpSender packetSender = new TcpSender();

		while (true) {
			//Sleep to give up CPU cycles
			while (ccl.isEmpty()) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			Packet p = ccl.remove();
			String ip = MeshNetworkManager.getIPForClient(p.getMac());
			packetSender.sendPacket(ip, Configuration.RECEIVE_PORT, p);

		}
	}

}
