package ch.ethz.soms.nervous.android;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SensorService extends Service implements SensorEventListener {

	private static final String DEBUG_TAG = "SensorService";

	private final IBinder mBinder = new SensorBinder();
	private SensorManager sensorManager = null;

	private SensorFrame sensorFrame = null;
	private SensorHeader sensorHeader = null;

	private Intent batteryStatus = null;
	private Sensor sensorAccelerometer = null;
	private Sensor sensorLight = null;
	private Sensor sensorMagnet = null;
	private Sensor sensorProximity = null;
	private Sensor sensorGyroscope = null;
	private Sensor sensorTemperature = null;
	private Sensor sensorHumidity = null;

	private boolean hasAccelerometer = false;
	private boolean hasLight = false;
	private boolean hasMagnet = false;
	private boolean hasProximity = false;
	private boolean hasGyroscope = false;
	private boolean hasTemperature = false;
	private boolean hasHumidity = false;
	
	public class SensorBinder extends Binder {
		SensorService getService() {
			return SensorService.this;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		sensorMagnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		sensorTemperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		sensorHumidity = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);

		hasAccelerometer = sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		hasLight = sensorManager.registerListener(this, sensorLight, SensorManager.SENSOR_DELAY_NORMAL);
		hasMagnet = sensorManager.registerListener(this, sensorMagnet, SensorManager.SENSOR_DELAY_NORMAL);
		hasProximity = sensorManager.registerListener(this, sensorProximity, SensorManager.SENSOR_DELAY_NORMAL);
		hasGyroscope = sensorManager.registerListener(this, sensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
		hasTemperature = sensorManager.registerListener(this, sensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
		hasHumidity = sensorManager.registerListener(this, sensorHumidity, SensorManager.SENSOR_DELAY_NORMAL);

		sensorHeader = new SensorHeader(sensorManager);

		// True means the sensor works with a listener and we don't know when it will be triggered
		if (hasAccelerometer) {
			sensorHeader.addSensor(SensorDescAccelerometer.class, true);
		}
		if (hasLight) {
			sensorHeader.addSensor(SensorDescLight.class, true);
		}
		if (hasMagnet) {
			sensorHeader.addSensor(SensorDescMagnetic.class, true);
		}
		if (hasProximity) {
			sensorHeader.addSensor(SensorDescProximity.class, true);
		}
		if (hasGyroscope) {
			sensorHeader.addSensor(SensorDescGyroscope.class, true);
		}
		if (hasTemperature) {
			sensorHeader.addSensor(SensorDescTemperature.class, true);
		}
		if (hasHumidity) {
			sensorHeader.addSensor(SensorDescHumidity.class, true);
		}

		// False means it's a parameter we can query, therefore we don't have to wait for it
		sensorHeader.addSensor(SensorDescBattery.class, false);

		sensorFrame = new SensorFrame(sensorHeader);

		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
		
		Log.d(DEBUG_TAG, "Service execution started");
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		// Do nothing

	}

	@Override
	public void onDestroy() {
		// Do nothing
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do nothing

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		long timestamp = System.currentTimeMillis();
		Sensor sensor = event.sensor;
		switch (sensor.getType()) {
		case Sensor.TYPE_LIGHT:
			sensorFrame.addSensorData(new SensorDescLight(timestamp, event.accuracy, event.values[0]));
			Log.d(DEBUG_TAG, "Light data added to frame");
			break;
		case Sensor.TYPE_PROXIMITY:
			sensorFrame.addSensorData(new SensorDescProximity(timestamp, event.accuracy, event.values[0]));
			Log.d(DEBUG_TAG, "Proximity data added to frame");
			break;
		case Sensor.TYPE_ACCELEROMETER:
			sensorFrame.addSensorData(new SensorDescAccelerometer(timestamp, event.accuracy, event.values[0], event.values[1], event.values[2]));
			Log.d(DEBUG_TAG, "Accelerometer data added to frame");
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			sensorFrame.addSensorData(new SensorDescMagnetic(timestamp, event.accuracy, event.values[0], event.values[1], event.values[2]));
			Log.d(DEBUG_TAG, "Magnetic data added to frame");
			break;
		case Sensor.TYPE_GYROSCOPE:
			sensorFrame.addSensorData(new SensorDescGyroscope(timestamp, event.accuracy, event.values[0], event.values[1], event.values[2]));
			Log.d(DEBUG_TAG, "Gyroscope data added to frame");
			break;
		case Sensor.TYPE_AMBIENT_TEMPERATURE:
			sensorFrame.addSensorData(new SensorDescTemperature(timestamp, event.accuracy, event.values[0]));
			Log.d(DEBUG_TAG, "Temperature data added to frame");
			break;
		case Sensor.TYPE_RELATIVE_HUMIDITY:
			sensorFrame.addSensorData(new SensorDescHumidity(timestamp, event.accuracy, event.values[0]));
			Log.d(DEBUG_TAG, "Humidity data added to frame");
			break;
		}

		if (sensorFrame.isComplete()) {
			// Add sensor data which can be queried
			timestamp = System.currentTimeMillis();
			
			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
			int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
			boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
			boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
			float batteryPct = level / (float) scale;

			SensorDescBattery sensorDataBattey = new SensorDescBattery(timestamp, batteryPct, isCharging, usbCharge, acCharge);
			sensorFrame.addSensorData(sensorDataBattey);
			Log.d(DEBUG_TAG, "Battery data added to frame");

			// Append frame to log
			new SensorServiceLoggerTask().execute(sensorFrame);
			

			// Stop service until it's triggered the next time
			sensorManager.unregisterListener(this);

			Log.d(DEBUG_TAG, "Service execution stopped");
			
			ServiceInfo info = new ServiceInfo(getApplicationContext());
			info.setTimeOfLastFrame();
			stopSelf();
		}
	}
	
	
	/**
	 * Asynchronous task to write to the log file
	 */
	private class SensorServiceLoggerTask extends AsyncTask<SensorFrame, Void, Void> {

		@Override
		protected Void doInBackground(SensorFrame... frames) {
			SensorFrame frame = frames[0];

			BufferedWriter bufWr = null;
			try {
				File file = new File(getApplicationContext().getFilesDir(), "SensorLog.txt");
				if (file.exists()) {
					// Write to new file
					bufWr = new BufferedWriter(new FileWriter(file, true));

				} else {
					file.createNewFile();
					Log.d(DEBUG_TAG, "New log file created");
					// Append to existing file
					bufWr = new BufferedWriter(new FileWriter(file, false));
					// Write header
					bufWr.append(sensorHeader.toString());
				}
				// Write frame
				bufWr.append(sensorFrame.toString());
				bufWr.flush();
				
				new ServiceInfo(getApplicationContext()).setFileSize(file.length());
				Log.d(DEBUG_TAG, "Added frame to log");
			} catch (IOException ex) {
				// TODO: useful error handling
			} finally {
				// Cleanup
				if (bufWr != null) {
					try {
						bufWr.close();
					} catch (IOException ex) {
						// TODO: useful error handling
					}
				}
			}
			return null;
		}
	}
	
}
