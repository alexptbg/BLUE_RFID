package blue.rfid;

import java.util.HashMap;

/**
 * @author Alex Soares
 * 18 FEB 2016
 * alex@ka-ex.net
**/

public class RBLGattAttributes {
	private static HashMap<String, String> attributes = new HashMap<String, String>();
	//BLE ID = 00002902-0000-1000-8000-00805f9b34fb
	
	//ADAFRUIT
	
	public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	public static String BLE_SHIELD_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
	public static String BLE_SHIELD_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
	public static String BLE_SHIELD_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    
	//RN4020
	/*
	public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	public static String BLE_SHIELD_TX = "00035b03-58e6-07dd-021a-08123a000301";
	public static String BLE_SHIELD_RX = "00035b03-58e6-07dd-021a-08123a0003ff";
	public static String BLE_SHIELD_SERVICE = "00035b03-58e6-07dd-021a-08123a000300";
	*/
	
	static {
		// RBL Services.
		attributes.put("00035b03-58e6-07dd-021a-08123a000300", "BLE Shield Service");
		// RBL Characteristics.
		attributes.put(BLE_SHIELD_TX, "BLE Shield TX");
		attributes.put(BLE_SHIELD_RX, "BLE Shield RX");
	}

	public static String lookup(String uuid, String defaultName) {
		String name = attributes.get(uuid);
		return name == null ? defaultName : name;
	}
}
