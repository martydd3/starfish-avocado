package edu.upenn.seas.seniordesign.starfish;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

public class BluetoothSetupActivity extends FragmentActivity {
	private ArrayAdapter<BluetoothDeviceWrapper> mPairedArrayAdapter;
	private ArrayAdapter<BluetoothDeviceWrapper> mDiscoveredArrayAdapter;
	private ListView pairedView;
	private ListView discoveredView;
	private BluetoothAdapter mBluetoothAdapter;
	private OnItemClickListener pairedListener;
	private OnItemClickListener discoveredListener;
	private static final int REQUEST_ENABLE_BT = 0xFF;

	/***************************************************************************
	 * lifecycle methods
	 **************************************************************************/

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_setup);
		// Show the Up button in the action bar.
		setupActionBar();

		// Set result CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!this.getAdapter()) {
			return;
		}

		// Sets ArrayAdapters for list views
		viewSetup();

		// Register the BroadcastReciever
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);

		// Turn on BT adapter if not already on
		turnOnStartDiscovery();
	}

	@Override
	protected void onStop() {
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();
			}
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();
			}
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();
			}
		}
	}

	/***************************************************************************
	 * helpers for lifecycle methods
	 **************************************************************************/

	/**
	 * sets mBluetoothAdapter to the default adapter
	 * 
	 * @return false if Bluetooth not supported by this device
	 */
	private boolean getAdapter() {
		// Create bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Check for bluetooth support
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth, create dialog that explains to
			// user
			DialogFragment newFragment = new NoBluetoothDialogFragment();
			newFragment.show(getSupportFragmentManager(), "no_bluetooth");
			return false;
		}
		return true;
	}

	/**
	 * sets up listViews for populated and discovered devices
	 */
	private void viewSetup() {
		// Create array adapters-- One for paired, one for discovery
		mPairedArrayAdapter = new ArrayAdapter<BluetoothDeviceWrapper>(this,
				R.layout.list_item);
		mDiscoveredArrayAdapter = new ArrayAdapter<BluetoothDeviceWrapper>(
				this, R.layout.list_item);

		// Find and set up the ListView for already paired devices
		pairedView = (ListView) findViewById(R.id.paired_devices);
		pairedListener = new DeviceListItemClickListener();
		pairedView.setOnItemClickListener(pairedListener);

		// Find and set up the ListView for newly discovered devices
		discoveredView = (ListView) findViewById(R.id.discovered_devices);
		discoveredView.setAdapter(mDiscoveredArrayAdapter);
		discoveredListener = new DeviceListItemClickListener();
		discoveredView.setOnItemClickListener(discoveredListener);
	}

	private void populatePairedList() {
		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				mPairedArrayAdapter.add(new BluetoothDeviceWrapper(device));
			}
			pairedView.setAdapter(mPairedArrayAdapter);
		} else {
			String noDevices = getResources().getText(R.string.none_paired)
					.toString();
			ArrayAdapter<String> noDevicesAdapter = new ArrayAdapter<String>(
					this, R.layout.list_item);
			noDevicesAdapter.add(noDevices);
			pairedView.setAdapter(noDevicesAdapter);
		}
		// refresh the ListView so the devices are displayed
		mPairedArrayAdapter.notifyDataSetChanged();
	}

	private void bluetoothDiscovery() {
		if (mBluetoothAdapter == null) {
			throw new NullPointerException();
		}
		if (!mBluetoothAdapter.isEnabled()) {
			return;
		}
		if (mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}

		mBluetoothAdapter.startDiscovery();

	}

	private void turnOnStartDiscovery() {
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			populatePairedList();
			bluetoothDiscovery();
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				// Bluetooth was enabled
				populatePairedList();
				bluetoothDiscovery();
			} else {
				// Tell user there was an erro connecting to Bluetooth, close

			}
		}
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Setup of broadcast receiver
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// Add the name and address to an array adapter to show in
				// ListView
				mDiscoveredArrayAdapter.add(new BluetoothDeviceWrapper(device));
				// refresh the ListView so the devices are displayed
				mDiscoveredArrayAdapter.notifyDataSetChanged();
			}
		}
	};

	/***************************************************************************
	 * private classes
	 **************************************************************************/

	private class DeviceListItemClickListener implements
			ListView.OnItemClickListener {

		@SuppressWarnings("rawtypes")
		@Override
		public void onItemClick(AdapterView parent, View view, int position,
				long id) {
			Adapter mAdapter = parent.getAdapter();
			if (mAdapter.getClass().equals(ArrayAdapter.class)) {
				Object item = mAdapter.getItem(position);
				if (item.getClass().equals(BluetoothDeviceWrapper.class)) {
					BluetoothDeviceWrapper deviceWrapper = (BluetoothDeviceWrapper) item;
					BluetoothDevice device = deviceWrapper.getDevice();
					BTConnectingThread mThread = new BTConnectingThread(device);
					mThread.run();
				} else {
					throw new IllegalArgumentException();
				}
			} else {
				throw new IllegalArgumentException();
			}
		}

	}

	private class BluetoothDeviceWrapper {
		private BluetoothDevice device;

		public BluetoothDeviceWrapper(BluetoothDevice device) {
			if (device != null) {
				this.device = device;
			} else {
				throw new NullPointerException();
			}
		}

		public BluetoothDevice getDevice() {
			return device;
		}

		public String toString() {
			return device.getName() + "\n" + device.getAddress();
		}
	}

	private class BTConnectingThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		// SPP UUID suggested by manufacturer
		private final UUID MY_UUID = UUID
				.fromString("00001101-0000-1000-8000-00805F9B34FB");

		public BTConnectingThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {}
			mmSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
				} catch (IOException closeException) {}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			// manageConnectedSocket(mmSocket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {}
		}
	}
}