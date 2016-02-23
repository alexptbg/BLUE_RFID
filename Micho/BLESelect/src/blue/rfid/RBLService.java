package blue.rfid;

import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * @author Alex Soares
 * 18 FEB 2016
 * alex@ka-ex.net
**/

public class RBLService extends Service {
	private final static String TAG = RBLService.class.getSimpleName();
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;

	public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_GATT_RSSI = "ACTION_GATT_RSSI";
	public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "EXTRA_DATA";
	public final static String ACTION_GATT_MSG = "ACTION_GATT_MSG";
	
	public final static UUID UUID_BLE_SHIELD_TX = UUID
			.fromString(RBLGattAttributes.BLE_SHIELD_TX);
	
	public final static UUID UUID_BLE_SHIELD_RX = UUID
			.fromString(RBLGattAttributes.BLE_SHIELD_RX);
	
	public final static UUID UUID_BLE_SHIELD_SERVICE = UUID
			.fromString(RBLGattAttributes.BLE_SHIELD_SERVICE);

	public final static UUID CLIENT_UUID = UUID
			.fromString(RBLGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
	
	private BluetoothGattCharacteristic bleTx;
	private BluetoothGattCharacteristic bleRx;
	
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			String intentAction;

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				intentAction = ACTION_GATT_CONNECTED;
				broadcastUpdate(intentAction);
				Log.i(TAG, "Connected to GATT server.");
				//Attempts to discover bluettoth services after successful connection.
				Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				intentAction = ACTION_GATT_DISCONNECTED;
				Log.i(TAG, "Disconnected from GATT server.");
				broadcastUpdate(intentAction);
			}
		}

		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_RSSI, rssi);
			} else {
				Log.w(TAG, "onReadRemoteRssi received: " + status);
			}
		};

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			} 
			//get characteristics
			bleTx = gatt.getService(UUID_BLE_SHIELD_SERVICE).getCharacteristic(UUID_BLE_SHIELD_TX);
			bleRx = gatt.getService(UUID_BLE_SHIELD_SERVICE).getCharacteristic(UUID_BLE_SHIELD_RX);
			
			if(!gatt.setCharacteristicNotification(bleRx, true)) {
				Log.e("SensorInterfaceActivity", "Can't set RX characteristic notifications");
			}
			BluetoothGattDescriptor bleGattDesc = bleRx.getDescriptor(CLIENT_UUID);
			
			if(bleGattDesc != null) {
				bleGattDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				if(!mBluetoothGatt.writeDescriptor(bleGattDesc)) {
					Log.e("SensorInterfaceActivity", "Can't write RX descriptor value");
				}
			} else {
				Log.e("SensorInterfaceActivity", "Can't get RX descriptor");
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			} 
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
		    BluetoothGattCharacteristic characteristic) {
			    super.onCharacteristicChanged(gatt, characteristic);
			    Log.e("SensorInterfaceActivity", "Received data");
			    /*
				float data;
				byte[] RxBuf = characteristic.getValue();
				
				int RxBufIndex = 0;
			    */
			    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			Log.d("SensorInterfaceActivity", "Characteristic write");
			if(gatt == null) {
				Log.e("SensorInterfaceActivity", "bleGatt is null");
			}
			if(status != BluetoothGatt.GATT_SUCCESS) {
				Log.w("SensorInterfaceActivity", "Write unsuccessful");
			}
		}
	};
	
	public void sendResponse(String value) {
		// send data over Bluetooth LE //this is not implemented yet.
		if(bleTx == null) {
			Log.e("SensorInterfaceActivity", "No Tx characteristic");
			return;
		}
		bleTx.setValue(value);
		// send byte
		Log.v("SensorInterfaceActivity", "Sending the data");
		if(mBluetoothGatt.writeCharacteristic(bleTx)) {
			Log.e("TX sent", value);
		} else {
			Log.e("SensorInterfaceActivity", "Couldn't send request byte");
		}
	}
	
	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
		//Log.e("T3","Connected...");
	}

	private void broadcastUpdate(final String action, int rssi) {
		final Intent intent = new Intent(action);
		intent.putExtra(EXTRA_DATA, String.valueOf(rssi));
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action,
		final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);
		//build data string here
		intent.putExtra(ACTION_GATT_MSG,characteristic.getStringValue(0));
		intent.setAction(ACTION_GATT_MSG);
		Log.e("INFO: ",characteristic.getStringValue(0));
		sendBroadcast(intent);
	}
    
	public class LocalBinder extends Binder {
		RBLService getService() {
			return RBLService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that
		// BluetoothGatt.close() is called
		// such that resources are cleaned up properly. In this particular
		// example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device. Try to reconnect.
		if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
			Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
			if (mBluetoothGatt.connect()) {
				return true;
			} else {
				return false;
			}
		}

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			Log.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		// We want to directly connect to the device, so we are setting the
		// autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		Log.d(TAG, "Trying to create a new connection.");
		mBluetoothDeviceAddress = address;

		return true;
	}

	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
	}

	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readCharacteristic(characteristic);
	}

	public void readRssi() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readRemoteRssi();
	}

	public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.writeCharacteristic(characteristic);
	}

	public void setCharacteristicNotification(
			BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		if (UUID_BLE_SHIELD_RX.equals(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic
					.getDescriptor(UUID.fromString(RBLGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
			descriptor
					.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		} else {
			Log.e("DEBUG_69", characteristic.getStringValue(0));
		}
	}

	public BluetoothGattService getSupportedGattService() {
		if (mBluetoothGatt == null)
			return null;
		return mBluetoothGatt.getService(UUID_BLE_SHIELD_SERVICE);
	}
}
