package com.example.android.P2P_MultiGroup.router;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.example.android.P2P_MultiGroup.MessageActivity;
import com.example.android.P2P_MultiGroup.WiFiDirectActivity;
import com.example.android.P2P_MultiGroup.config.Configuration;
import com.example.android.P2P_MultiGroup.router.tcp.TcpReciever;
import com.example.android.P2P_MultiGroup.ui.DeviceDetailFragment;

import android.widget.Toast;

/**
 * The main receiver class
 */
public class Receiver implements Runnable {

	/**
	 * Flag if the receiver has been running to prevent overzealous thread spawning
	 */
	public static boolean running = false;

	/**
	 * A ref to the activity
	 */
	static WiFiDirectActivity activity;

	/**
	 * Constructor with activity
	 * @param a
	 */
	public Receiver(WiFiDirectActivity a) {
		Receiver.activity = a;
		running = true;
	}

	/**
	 * Main thread runner
	 */
	public void run() {
		/*
		 * A queue for received packets
		 */
		ConcurrentLinkedQueue<Packet> packetQueue = new ConcurrentLinkedQueue<Packet>();

		/*
		 * Receiver thread
		 */
		new Thread(new TcpReciever(Configuration.RECEIVE_PORT, packetQueue)).start();

		Packet p;

		/*
		 * Keep going through packets
		 */
		while (true) {
			/*
			 * If the queue is empty, sleep to give up CPU cycles
			 */
			while (packetQueue.isEmpty()) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			/*
			 * Pop a packet off the queue
			 */
			p = packetQueue.remove();


			/*
			 * If it's a hello, this is special and need to go through the connection mechanism for any node receiving this
			 */
			if (p.getType().equals(Packet.TYPE.HELLO)) {
				// Put it in your routing table
				for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values()) {
					if (c.getMac().equals(MeshNetworkManager.getSelf().getMac()) || c.getMac().equals(p.getSenderMac()))
						continue;
					Packet update = new Packet(3,Packet.TYPE.UPDATE, new byte[0], c.getMac(),
							MeshNetworkManager.getSelf().getMac(),p.getSenderIP(),MeshNetworkManager.getSelf().getMac(),
							p.getSenderMac());
					Sender.queuePacket(update);
				}

				MeshNetworkManager.routingTable.put(p.getSenderMac(),
						new AllEncompasingP2PClient(p.getSenderMac(), p.getSenderIP(), p.getSenderMac(),
								MeshNetworkManager.getSelf().getMac()));

				// Send routing table back as HELLO_ACK
				byte[] rtable = MeshNetworkManager.serializeRoutingTable();

				Packet ack = new Packet(3,Packet.TYPE.HELLO_ACK, rtable, p.getSenderMac(), MeshNetworkManager.getSelf()
						.getMac(),p.getSenderIP(),MeshNetworkManager.getSelf().getMac(), p.getSenderMac());
				Sender.queuePacket(ack);
				somebodyJoined(p.getSenderMac());
				updatePeerList();
			} else {
				// If you're the intended target for a non hello message
				if (p.getMac().equals(MeshNetworkManager.getSelf().getMac())) {
					//if we get a hello ack populate the table
					if (p.getType().equals(Packet.TYPE.HELLO_ACK)) {
						MeshNetworkManager.deserializeRoutingTableAndAdd(p.getData());
						MeshNetworkManager.getSelf().setGroupOwnerMac(p.getSenderMac());
						somebodyJoined(p.getSenderMac());
						updatePeerList();
					} else if (p.getType().equals(Packet.TYPE.UPDATE)) {
						//if it's an update, add to the table
						MeshNetworkManager.routingTable.put(p.getResourceMAC(),
								new AllEncompasingP2PClient(p.getResourceMAC(), p.getResourceIP(), p.getResourceMAC(),p.getResourceGoMac()));

						final String message = p.getResourceMAC() + " joined the conversation";
						final String name = p.getSenderMac();
						activity.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if (activity.isVisible) {
									Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
								} else {
									MessageActivity.addMessage(name, message);
								}
							}
						});
						updatePeerList();

					} else if (p.getType().equals(Packet.TYPE.MESSAGE)) {
						//If it's a message display the message and update the table if they're not there
						// for whatever reason
						final String message = p.getResourceMAC() + " says:\n" + new String(p.getData());
						final String msg = new String(p.getData());
						final String name = p.getResourceMAC();

						if (!MeshNetworkManager.routingTable.contains(p.getResourceMAC())) {
							/*
							 * Update your routing table if for some reason this
							 * guy isn't in it
							 */
							MeshNetworkManager.routingTable.put(p.getResourceMAC(),
									new AllEncompasingP2PClient(p.getResourceMAC(), p.getResourceIP(),p.getResourceMAC(),
											p.getResourceGoMac()));
						}

						activity.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if (activity.isVisible) {
									Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
								} else {
									MessageActivity.addMessage(name, msg);
								}
							}
						});
						updatePeerList();
					}
				} else {
					// otherwise forward it if you're not the recipient
					int ttl = p.getTtl();
					// Have a ttl so that they don't bounce around forever
					ttl--;
					if (ttl > 0) {
						Sender.queuePacket(p);
						p.setTtl(ttl);
					}
				}
			}

		}
	}

	/**
	 * GUI thread to send somebody joined notification
	 * @param smac
	 */
	public static void somebodyJoined(String smac) {

		final String message;
		final String msg;
		message = msg = smac + " has joined.";
		final String name = smac;
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (activity.isVisible) {
					Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
				} else {
					MessageActivity.addMessage(name, msg);
				}
			}
		});
	}

	/**
	 * Somebody left notification on the UI thread
	 * @param smac
	 */
	public static void somebodyLeft(String smac) {

		final String message;
		final String msg;
		message = msg = smac + " has left.";
		final String name = smac;
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (activity.isVisible) {
					Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
				} else {
					MessageActivity.addMessage(name, msg);
				}
			}
		});
	}

	/**
	 * Update the list of peers on the front page
	 */
	public static void updatePeerList() {
		if (activity == null)
			return;
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				DeviceDetailFragment.updateGroupChatMembersMessage();
			}

		});
	}

}