package blue.rfid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import ble.rfid.R;

/**
 * @author Alex Soares
 * 18 FEB 2016
 * alex@ka-ex.net
**/

public class BLESelect extends Activity {
	private final static String TAG = BLESelect.class.getSimpleName();
	private RBLService mBluetoothLeService;
	private BluetoothAdapter mBluetoothAdapter;
	public static List<BluetoothDevice> mDevice = new ArrayList<BluetoothDevice>();

	Button lastDeviceBtn = null;
	Button scanAllBtn = null;
	Button bTX = null;
	TextView uuidRSSI = null;
	TextView lastUuid = null;
	TextView uuidCard = null;
	TextView uuidPin = null;
	TextView uuidName = null;
	
	private static final int REQUEST_ENABLE_BT = 1;
	private static final long SCAN_PERIOD = 3000;
	public static final int REQUEST_CODE = 30;
	private String mDeviceAddress;
	private String mDeviceName;
	private String RESPONSE = "BLUERFID";
	private String CRLF = "\r\n";
	private boolean flag = true;
	private boolean connState = false;

	String path = Environment.getExternalStorageDirectory().getAbsolutePath();
	String fname = "flash.txt";
	//alex
    //String URL = "http://eesystems.net/sql/rfid_card.php?Name=1&card=";
    //String IMG_URL = "http://eesystems.net/sql/rfid_card.php?Img=1&card=";
    //Micho
	String URL = "http://eesystems.net/db2/mihailp/Progress_Portable/coupons/rfid_card.php?Name=1&card=";
	String IMG_URL = "http://eesystems.net/db2/mihailp/Progress_Portable/coupons/rfid_card.php?Img=1&card=";
    Bitmap bmp = null;
    
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((RBLService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (RBLService.ACTION_GATT_CONNECTED.equals(action)) {
				flag = true;
				connState = true;
				Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
				writeToFile(mDeviceName + " ( " + mDeviceAddress + " )");
				lastUuid.setText(mDeviceName + " ( " + mDeviceAddress + " )");
				lastDeviceBtn.setVisibility(View.GONE);
				bTX.setVisibility(View.VISIBLE);
				scanAllBtn.setText("Disconnect");
				startReadRssi();
			} else if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
				flag = false;
				connState = false;
				Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
				scanAllBtn.setText("Scan All");
				uuidRSSI.setText("");
				uuidCard.setText("");
				uuidPin.setText("");
				uuidName.setText("");
				ImageView imgView =(ImageView)findViewById(R.id.photo);
				imgView.setVisibility(View.GONE);
				bTX.setVisibility(View.GONE);
				lastDeviceBtn.setVisibility(View.VISIBLE);
			} else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
				displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
			} else if (RBLService.ACTION_GATT_MSG.equals(action)) {
				displayMsg(intent.getStringExtra(RBLService.ACTION_GATT_MSG));
			}
			//Log.e("A: ",action);
		}
	};

	private void writeToFile(String flash) {
		File sdfile = new File(path, fname);
		try {
			FileOutputStream out = new FileOutputStream(sdfile);
			out.write(flash.getBytes());
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("resource")
	private String readConnDevice() {
		String filepath = path + "/" + fname;
		String line = null;

		File file = new File(filepath);
		try {
			FileInputStream f = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(f, "GB2312");
			BufferedReader dr = new BufferedReader(isr);
			line = dr.readLine();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line;
	}

	private void displayData(String data) {
		if (data != null) {
			//bluetooth signal read
			uuidRSSI.setText("SIGNAL:\t" + data);
		}
	}

	public void displayMsg(String data) {
		if (data != null) {
			uuidCard.setText(data);
        	String url = URL+data;
        	String img_url = IMG_URL+data;
        	String resultServer = getHttpGet(url);
        	Log.e("Server:", resultServer);
        	String[] separated = resultServer.split(":");
        	if (separated[0].equals("ERROR")) {
        		alertNOOK();
        	} else {
        		alertOK();
        	}
        	uuidPin.setText(separated[0]);
        	uuidName.setText(separated[1]);
        	//get photo
        	HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(img_url);
            HttpResponse getResponse = null;
            try {
                getResponse = client.execute(get);
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //String entityContents="";
            HttpEntity responseEntity = getResponse.getEntity();
            BufferedHttpEntity httpEntity = null;
            try {
                httpEntity = new BufferedHttpEntity(responseEntity);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            InputStream imageStream = null;
            try {
                imageStream = httpEntity.getContent();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            bmp = BitmapFactory.decodeStream(imageStream);

            ImageView imgView =(ImageView)findViewById(R.id.photo);
            imgView.setImageBitmap(bmp);
            imgView.setVisibility(View.VISIBLE);
		}
	}
	
	public void alertOK() {
		try {
			MediaPlayer mp = MediaPlayer.create(this, R.raw.ping);  
			mp.start();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
	
	public void alertNOOK() {
		try {
			MediaPlayer mp = MediaPlayer.create(this, R.raw.error);  
			mp.start();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
	
	public String getHttpGet(String url) {
		StringBuilder str = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		//try to get response from eesystems
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) { // Status OK
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					str.append(line);
				}
			} else {
				Log.e("Log", "Failed to download result..");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str.toString();
	}
    
	private void startReadRssi() {
		new Thread() {
			public void run() {
				while (flag) {
					mBluetoothLeService.readRssi();
					try {
						sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);
		
	    StrictMode.ThreadPolicy ourPolicy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(ourPolicy);
        //echo
		uuidRSSI = (TextView) findViewById(R.id.uuidRSSI);
		uuidCard = (TextView) findViewById(R.id.uuCard);
		uuidPin = (TextView) findViewById(R.id.uuPin);
		uuidName = (TextView) findViewById(R.id.uuName);
		lastUuid = (TextView) findViewById(R.id.lastDevice);
		
		String connDeviceInfo = readConnDevice();
		if (connDeviceInfo == null) {
			lastUuid.setText("");
		} else {
			mDeviceName = connDeviceInfo.split("\\( ")[0].trim();
			String str = connDeviceInfo.split("\\( ")[1];
			mDeviceAddress = str.substring(0, str.length() - 2);
			lastUuid.setText(connDeviceInfo);
		}
		//check network
        if (isNetworkConnected()) {
        	//todo later
        } else {
        	//todo later
        }
        //clear text
		uuidCard.setText("");
		uuidPin.setText("");
		uuidName.setText("");
		
		lastDeviceBtn = (Button) findViewById(R.id.ConnLastDevice);
		
		lastDeviceBtn.setOnClickListener(new OnClickListener() {

		    @Override
		    public void onClick(View v) {
			    mDevice.clear();
			    String connDeviceInfo = readConnDevice();
			    if (connDeviceInfo == null) {
				    Toast toast = Toast.makeText(BLESelect.this, "No Last connect device!", Toast.LENGTH_SHORT);
				    toast.setGravity(Gravity.CENTER, 0, 0);
				    toast.show();

				    return;
			    }

			    String str = connDeviceInfo.split("\\( ")[1];
			    final String mDeviceAddress = str.substring(0, str.length() - 2);

			    scanLeDevice();

			    Timer mNewTimer = new Timer();
			    mNewTimer.schedule(new TimerTask() {

			        @Override
			        public void run() {
					    for (BluetoothDevice device : mDevice)
						    if ((device.getAddress().equals(mDeviceAddress))) {
							    mBluetoothLeService.connect(mDeviceAddress);

							    return;
						    }
					        runOnUiThread(new Runnable() {
						    
					            @Override
						        public void run() {
							        Toast toast = Toast.makeText(BLESelect.this, "No Last connect device!",
								        Toast.LENGTH_SHORT);
							        toast.setGravity(Gravity.CENTER, 0, 0);
							        toast.show();
						        }
					        });
				        }
			        }, SCAN_PERIOD);
		        }
		    });

		    scanAllBtn = (Button) findViewById(R.id.ScanAll);
		    
		    scanAllBtn.setOnClickListener(new OnClickListener() {
		    
		    @Override
		    public void onClick(View v) {
			    if (connState == false) {
				    scanLeDevice();
				    try {
					    Thread.sleep(SCAN_PERIOD);
				    } catch (InterruptedException e) {
					    // TODO Auto-generated catch block
					    e.printStackTrace();
				    }
				    Intent intent = new Intent(getApplicationContext(), Device.class);
				    startActivityForResult(intent, REQUEST_CODE);
			    } else {
				    mBluetoothLeService.disconnect();
				    mBluetoothLeService.close();
			        scanAllBtn.setText("Scan All");
				    uuidRSSI.setText("");
				    uuidCard.setText("");
				    uuidPin.setText("");
				    uuidName.setText("");
				    ImageView imgView =(ImageView)findViewById(R.id.photo);
				    imgView.setVisibility(View.GONE);
				    bTX.setVisibility(View.GONE);
				    lastDeviceBtn.setVisibility(View.VISIBLE);
			    }
		    }
	    });

		//TX teste
		bTX = (Button) findViewById(R.id.tx);
		bTX.setVisibility(View.GONE);
		bTX.setOnClickListener(new OnClickListener() {

			@Override
		    public void onClick(View v) {
		        mBluetoothLeService.sendResponse(RESPONSE+CRLF);
				Toast toast = Toast.makeText(BLESelect.this, "TX clicked!", Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
		    }

		});
		
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT).show();
			finish();
		}

	    final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
	    mBluetoothAdapter = mBluetoothManager.getAdapter();
	    if (mBluetoothAdapter == null) {
		    Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT).show();
		    finish();

		    return;
	    }

	    Intent gattServiceIntent = new Intent(BLESelect.this, RBLService.class);
	    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

	@Override
	protected void onResume() {
		super.onResume();
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_RSSI);
		intentFilter.addAction(RBLService.ACTION_GATT_MSG);
		return intentFilter;
	}

	private void scanLeDevice() {
		new Thread() {

			@Override
			public void run() {
				mBluetoothAdapter.startLeScan(mLeScanCallback);
				try {
					Thread.sleep(SCAN_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
			}
		}.start();
	}
	
	// Check network connection
	private boolean isNetworkConnected(){
	    ConnectivityManager connectivityManager 
	            = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();    
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (device != null) {
						if (mDevice.indexOf(device) == -1)
							mDevice.add(device);
					}
				}
			});
		}
	};

	@Override
	protected void onStop() {
		super.onStop();
        //update flag
		flag = false;
        //unregister service
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mServiceConnection != null)
			unbindService(mServiceConnection);
            //close app
		    System.exit(0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		} else if (requestCode == REQUEST_CODE && resultCode == Device.RESULT_CODE) {
			mDeviceAddress = data.getStringExtra(Device.EXTRA_DEVICE_ADDRESS);
			mDeviceName = data.getStringExtra(Device.EXTRA_DEVICE_NAME);
			mBluetoothLeService.connect(mDeviceAddress);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
}
