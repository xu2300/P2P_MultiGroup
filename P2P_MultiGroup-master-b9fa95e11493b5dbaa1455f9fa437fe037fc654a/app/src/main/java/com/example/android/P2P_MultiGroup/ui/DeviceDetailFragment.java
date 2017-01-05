package com.example.android.P2P_MultiGroup.ui;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.P2P_MultiGroup.WiFiDirectActivity;
import com.example.android.P2P_MultiGroup.router.AllEncompasingP2PClient;
import com.example.android.P2P_MultiGroup.router.MeshNetworkManager;
import com.example.android.P2P_MultiGroup.router.Packet;
import com.example.android.P2P_MultiGroup.router.Sender;
import com.example.android.P2P_MultiGroup.ui.DeviceListFragment.DeviceActionListener;
import com.example.android.P2P_MultiGroup.wifi.WiFiDirectBroadcastReceiver;
import com.example.android.P2P_MultiGroup.R;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

	private static View mContentView = null;
	private WifiP2pDevice device;
	ProgressDialog progressDialog = null;

	/**
	 * Update who is in the chat from the routing table
	 */
	public static void updateGroupChatMembersMessage() {
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		if (view != null) {
			String s = "Currently in the network chatting: \n";
			for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values()) {
				s += c.getMac() + "\n";
			}
			view.setText(s);
		}
	}

	/**
	 * Once the activity is created make sure to call the super constructor
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	/**
	 * Handle the view setup and callbacks
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mContentView = inflater.inflate(R.layout.device_detail, null);
		mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				WifiP2pConfig config = new WifiP2pConfig();
				config.deviceAddress = device.deviceAddress;
				config.wps.setup = WpsInfo.PBC;
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "Connecting to :"
						+ device.deviceAddress, true, true);
				((DeviceActionListener) getActivity()).connect(config);

			}
		});

		mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				((DeviceActionListener) getActivity()).disconnect();
			}
		});

		return mContentView;
	}

	/**
	 * This is mostly for debugging
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		// User has picked an image. Transfer it to group owner i.e peer using
		// FileTransferService.
		Uri uri = data.getData();
		TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
		statusText.setText("Sending: " + uri);
		Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
	}

	/**
	 * If you aren't the group owner and a connection has been established, send a hello packet to set up the connection
	 */
	@Override
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.getView().setVisibility(View.VISIBLE);

		if (!info.isGroupOwner) {
			Sender.queuePacket(new Packet(3,Packet.TYPE.HELLO, new byte[0], null, WiFiDirectBroadcastReceiver.MAC,
					null, null,null));
			try {
				MeshNetworkManager.getSelf().setIp(getDottedDecimalIP(getLocalIPAddress()));
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		// hide the connect button
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
	}

	/**
	 * Updates the UI with device data
	 *
	 * @param device
	 *            the device to be displayed
	 */
	public void showDetails(WifiP2pDevice device) {
		this.device = device;
		this.getView().setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		String s = "Currently in the network chatting: \n";
		for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values()) {
			s += c.getMac() + "\n";
		}
		view.setText(s);
	}

	/**
	 * Clears the UI fields after a disconnect or direct mode disable operation.
	 */
	public void resetViews() {
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.status_text);
		view.setText(R.string.empty);
		this.getView().setVisibility(View.GONE);
	}

	private byte[] getLocalIPAddress() throws SocketException {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
							return inetAddress.getAddress();
						}
						//return inetAddress.getHostAddress().toString(); // Galaxy Nexus returns IPv6
					}
				}
			}
		} catch (SocketException ex) {
			//Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
		} catch (NullPointerException ex) {
			//Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
		}
		return null;
	}

	private String getDottedDecimalIP(byte[] ipAddr) {
		//convert to dotted decimal notation:
		String ipAddrStr = "";
		for (int i=0; i<ipAddr.length; i++) {
			if (i > 0) {
				ipAddrStr += ".";
			}
			ipAddrStr += ipAddr[i]&0xFF;
		}
		return ipAddrStr;
	}

}
