package edu.wisc.jj;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import edu.wisc.jj.PcmRecorder;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MyRecorder extends Activity implements OnClickListener,
		SensorEventListener {

	public final int STOPPED = 0;
	public final int RECORDING = 1;
	private final int CHARACTER_NUM = 26;
	public final int SENSOR_GYRO = 0;
	public final int SENSOR_ACCEL = 1;
	private final int SENSOR_LIACCEL = 2;
	// number of sensors
	private final int SENSOR_REGISTERED_NUM = 3;

	private Thread th;
	PendingIntent pi;
	BroadcastReceiver br;
	final private long ONE_SECOND = 1000;
	private final long RECORD_TIME = ONE_SECOND * 40;

	AlarmManager am;
	PcmRecorder recorderInstance = null;
	Button startButton = null;
	Button stopButton = null;
	Button exitButon = null;
	TextView textView = null;
	int status = STOPPED;
	int[] charIndex = new int[CHARACTER_NUM];
	String curChar = "";
	private int curAccuracy = -1;

	List<List<float[]>> sensorData;

	public void onClick(View v) {
		if (v == stopButton) {
			this.setTitle("stop recording");
			if (recorderInstance != null) {
				recorderInstance.setRecording(false);
			}
			textView.setText("stop recording");
			Log.d(this.toString(), "clicked stop Button");
			sensorReg(false);
		} else if (v == exitButon) {
			if (recorderInstance != null) {
				recorderInstance.setRecording(false);
			}
			sensorReg(false);
			finish();
			System.exit(0);
		} else { // start recording button or the char button
			if (v != this.startButton) {
				curChar = String.valueOf((char) (v.getId() + 'a'));
				this.charIndex[v.getId()]++;
			} else {
				curChar = "test";
			}
			if (th != null && th.isAlive()) {
				Toast.makeText(
						getApplicationContext(),
						"the recording thread is alive. Cannot start a new one",
						Toast.LENGTH_SHORT).show();
				Log.e(this.toString(),
						"another recording thread is currently running");
			} else {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
				NativeRecorder nRecorder = new NativeRecorder(curChar
						+ String.valueOf(this.charIndex[v.getId()]),
						getRecordCommands(),this.getApplicationContext());
				th = new Thread(nRecorder);
				th.start();
				textView.setText("start recording");
//				sensorReg(true);
//				// schedule ending event
//				am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//						SystemClock.elapsedRealtime() + this.RECORD_TIME, pi);
				Log.d(this.toString(), "char: " + curChar + " index: "
						+ this.charIndex[v.getId()]);
//				sendNativeCmd(curChar
//						+ String.valueOf(this.charIndex[v.getId()]));
				// recorderInstance.setRecording(true);
			}
		}
	}

	/**
	 * get terminal commands for recording from the script.jpg file in the asset
	 * folder use .jpg because to avoid compression of aapt
	 */
	private List<String> getRecordCommands() {
		InputStream is = null;
		try {
			is = getAssets().open("script.jpg");
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		DataInputStream dataIO = null;
		List<String> script = new ArrayList<String>();
		BufferedReader br = null;
		try {
			String sCurrentLine;
			dataIO = new DataInputStream(is);
			br = new BufferedReader(new InputStreamReader(is));
			while ((sCurrentLine = br.readLine()) != null) {
				script.add(sCurrentLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} finally {
			try {
				if (dataIO != null)
					dataIO.close();
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return script;
	}

	/**
	 * send native cmd using alsa_amixer and alsa_aplay to capture the audio
	 * 
	 * @param fileName
	 * 
	 * @throws IOException
	 */
	private void sendNativeCmd(String fileName) {
		InputStream is = null;
		try {
			is = getAssets().open("script.jpg");
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		DataInputStream dataIO = null;
		List<String> script = new ArrayList<String>();
		BufferedReader br = null;
		try {
			String sCurrentLine;
			dataIO = new DataInputStream(is);
			br = new BufferedReader(new InputStreamReader(is));
			while ((sCurrentLine = br.readLine()) != null) {
				Log.d(this.toString(), "read in script line: " + sCurrentLine);
				script.add(sCurrentLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} finally {
			try {
				if (dataIO != null)
					dataIO.close();
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		try {
			Process p = null;
			p = Runtime.getRuntime().exec("/system/xbin/su");
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			Thread.sleep(3000);
			for (String mLine : script) {
				if (mLine.contains("alsa_aplay")) {
					mLine += " /sdcard/nativeRecorder/" + fileName + ".wav\n";
					os.writeBytes(mLine);
					os.flush();
					break;
				}
				mLine = mLine + "\n";
				os.writeBytes(mLine);
				os.flush();
				Log.d(this.toString(), "executing cmd: " + mLine);
				// kept reading the output of the commands
				char[] buffer = new char[500];
				String output = "";
				int read;
				if ((read = reader.read(buffer)) > 0) {
					output = new String(buffer);
				}
				Log.d(this.toString(), "cmd output: " + output);
			}
			// wait until the command is finished
			String cmd = "echo -n 0\n";
			os.write(cmd.getBytes("ASCII"));
			os.flush();
			reader.read();
			Log.d(this.toString(), "finished recording. ");
			String exitCmd = "exit\n";
			os.writeBytes(exitCmd);
			os.flush();
			os.close();
			reader.close();
			// p.waitFor();
			// p.destroy();
			Toast.makeText(this, "audio finished", Toast.LENGTH_SHORT).show();
			Log.d(this.toString(), "finished");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// init variable
		sensorData = new ArrayList<List<float[]>>();
		for (int sensorIndex = 0; sensorIndex < SENSOR_REGISTERED_NUM; sensorIndex++) {
			sensorData.add(new LinkedList<float[]>());
		}
		File sensorDataDirectory = new File(Environment
				.getExternalStorageDirectory().getAbsolutePath()
				+ "/sensorData/");
		if (!sensorDataDirectory.exists()) {
			Log.d(this.toString(), "sensorData folder doesn't exsit...CREATED");
			sensorDataDirectory.mkdirs();
		}
		// set up clock. things to do when finish collecting data
		br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// recorderInstance.setRecording(false);
				sensorReg(false);
				textView.setText("stop recording");
				WritingToFileAsyncTask mAsyncTask = new WritingToFileAsyncTask();
				mAsyncTask.execute();
				Log.d(this.toString(),
						"record time elapsed. stopped recording. unregisted sensor");
				Toast.makeText(context, "sensor finished collecting",
						Toast.LENGTH_SHORT).show();
			}
		};
		registerReceiver(br, new IntentFilter("edu.wisc.jj.wakeUpCall"));
		pi = PendingIntent.getBroadcast(this, 0, new Intent(
				"edu.wisc.jj.wakeUpCall"), PendingIntent.FLAG_UPDATE_CURRENT);
		am = (AlarmManager) (this.getSystemService(Context.ALARM_SERVICE));
		// generate view
		startButton = new Button(this);
		stopButton = new Button(this);
		exitButon = new Button(this);
		textView = new TextView(this);
		startButton.setText("Start-only recording audio");
		stopButton.setText("Stop-stop recording audio. unreg sensors");
		exitButon.setText("exit");
		textView.setText("waiting to start");
		startButton.setOnClickListener(this);
		stopButton.setOnClickListener(this);
		exitButon.setOnClickListener(this);
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(textView);
		layout.addView(startButton);
		layout.addView(stopButton);
		layout.addView(exitButon);
		TableLayout tl = new TableLayout(this);
		for (int index = 0; index < CHARACTER_NUM; index = index + 4) {
			TableRow tr1 = new TableRow(this);
			// generate button on each row
			for (int colIndex = index; colIndex <= index + 3
					&& colIndex < CHARACTER_NUM; colIndex++) {
				Button myButton1 = new Button(this);
				myButton1.setText(String.valueOf((char) (colIndex + 'a')));
				myButton1.setId(colIndex);
				myButton1.setOnClickListener(this);
				// important for the layout
				myButton1
						.setLayoutParams(new TableRow.LayoutParams(
								0,
								android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
								2.5f));
				tr1.addView(myButton1);
			}
			TableLayout.LayoutParams tableRowParams = new TableLayout.LayoutParams(
					TableLayout.LayoutParams.MATCH_PARENT,
					TableLayout.LayoutParams.WRAP_CONTENT);
			tableRowParams.setMargins(0, 8, 0, 0);
			tr1.setLayoutParams(tableRowParams);
			tl.addView(tr1);
		}
		layout.addView(tl);
		this.setContentView(layout);
	}

	/**
	 * register or unregister needed sensor listeners before using it set the
	 * sampling as fast as possible accel frequency on galaxy nexus:
	 * fastest:122Hz, game:60Hz,UI:15Hz,Normal:15Hz gyroscope is relatively 10Hz
	 * less in each category
	 * 
	 * @param regOrUnreg
	 *            : register(true) or unregister(false)
	 * @return number of sensor registered
	 */
	private void sensorReg(boolean regOrUnreg) {
		SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		if (regOrUnreg) {
			Log.i("sensorReg", "reged sensors");
			mSensorManager.registerListener(this,
					mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					SensorManager.SENSOR_DELAY_FASTEST);
			mSensorManager.registerListener(this, mSensorManager
					.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
					SensorManager.SENSOR_DELAY_FASTEST);
			mSensorManager.registerListener(this,
					mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_FASTEST);
		} else {
			Log.i("sensorReg", "unreged sensors");
			mSensorManager.unregisterListener(this);
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// need to copy instead of add a value reference to it
		float[] values = new float[event.values.length];
		System.arraycopy(event.values, 0, values, 0, values.length);
		int sensorID = -1;
		// get sensorID in this class
		switch (event.sensor.getType()) {
		case Sensor.TYPE_GYROSCOPE:
			sensorID = this.SENSOR_GYRO;
			break;
		case Sensor.TYPE_ACCELEROMETER:
			sensorID = this.SENSOR_ACCEL;
			break;
		case Sensor.TYPE_LINEAR_ACCELERATION:
			sensorID = this.SENSOR_LIACCEL;
			break;
		}
		this.sensorData.get(sensorID).add(values);
		if (overThreshold(values, sensorID)) {
			textView.setText("key stroke!!!!!!!!!!!!!");
		} else {
			textView.setText("nothing");
		}
	}

	/**
	 * detect whether the sensor reading is over the threshold or not
	 * 
	 * @param values
	 * @param curSensor
	 * @return
	 */
	private boolean overThreshold(float[] values, int sensorID) {
		boolean over = false;
		switch (sensorID) {
		case SENSOR_GYRO:
			for (int index = 0; index < values.length; index++) {
				// if (!curSensor.prevKeyStrokeSampleData.isEmpty() &&
				// Math.abs(values[index]
				// - curSensor.prevKeyStrokeSampleData
				// .get(curSensor.prevKeyStrokeSampleData.size() - 1)[index]) >
				// THRESHOLD_VALUE)
				// over = true;
				// else if (Math.abs(values[index]) > THRESHOLD_VALUE){
				// over = true;
				// }

				if (values[index] > 0.040 || values[index] < -0.040)
					over = true;
			}
			break;
		case SENSOR_LIACCEL:
			for (int index = 0; index < values.length; index++) {
				if (values[index] > 0.8 || values[index] < -0.8)
					over = true;
			}
			break;
		}
		return over;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if (accuracy != this.curAccuracy) {
			textView.setText("accuracy changed (level 0--3, from lowest to highes) :"
					+ accuracy);
			Toast.makeText(getApplicationContext(),
					"accuracy changed " + accuracy, Toast.LENGTH_SHORT).show();
			this.curAccuracy = accuracy;
		}
		Log.d("sensor accuracy", sensor.getName() + "changed accuracy "
				+ accuracy);
	}

	@Override
	public void onStop() {
		super.onStop();
		// unregister sensor listeners to prevent the activity from draining the
		// device's battery.
		sensorReg(false);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// unregister sensor listeners to prevent the activity from draining the
		// device's battery.
		sensorReg(false);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onDestroy() {
		am.cancel(pi);
		unregisterReceiver(br);
		super.onDestroy();
	}

	/**
	 * an async task to write sensor data samples to file
	 * 
	 * @author JJ
	 * 
	 */
	private class WritingToFileAsyncTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected void onProgressUpdate(Integer... params) {
			switch (params[0]) {
			case 0:
				Toast.makeText(
						getApplicationContext(),
						"succesful get key press training data. input another training entry. Currently:"
								+ params[1], Toast.LENGTH_SHORT).show();
				break;
			case 1:
				textView.setText("finished writing to file. Char: "
						+ (char) params[1].intValue() + " index: " + params[2]);
				break;
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			Log.d("WritingToFileAsyncTastk",
					"Async task starts writing to file");
			synchronized (sensorData) {
				int curFileIndex = charIndex[(int) (curChar.charAt(0) - 'a')];
				String sensorTypeString;
				for (int sensorIndex = 0; sensorIndex < sensorData.size(); sensorIndex++) {
					sensorTypeString = "";
					switch (sensorIndex) {
					case SENSOR_GYRO:
						sensorTypeString = "gyro";
						break;
					case SENSOR_ACCEL:
						sensorTypeString = "accel";
						break;
					case SENSOR_LIACCEL:
						sensorTypeString = "liaccel";
						break;
					}
					if (curChar.length() > 1) {
						Log.e("async task: writing to file",
								"curChar is not a char");
					}
					// write raw data to file
					writeSamplesToFile(Environment
							.getExternalStorageDirectory().getAbsolutePath()
							+ "/sensorData/"
							+ sensorTypeString
							+ "_"
							+ curChar
							+ curFileIndex + ".log",
							sensorData.get(sensorIndex));
					Log.d(this.toString(), "finish writing to file. "
							+ sensorTypeString + " char: " + curChar + " No. "
							+ curFileIndex);
				}
				publishProgress(1, (int) curChar.charAt(0), curFileIndex);
				// clear the sensorData list after writing to file
				for (List<float[]> mList : sensorData) {
					mList.clear();
				}
			}
			return null;
		}
	}

	/**
	 * write collected sampling to file
	 * 
	 * @param fileName
	 * @return
	 */
	private boolean writeSamplesToFile(String fileName, List<float[]> data) {
		File mFile = new File(fileName);
		FileOutputStream outputStream = null;
		try {
			// choose to overwrite instead of append
			outputStream = new FileOutputStream(mFile, false);
			OutputStreamWriter osw = new OutputStreamWriter(outputStream);
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();
			DecimalFormat df = new DecimalFormat("##0.0000;##0.0000");
			String header = "raw data with key press event: \n"
					+ "file created time: " + dateFormat.format(date) + "\n"
					+ "x-axis               y-axis             z-axis\n";
			osw.write(header);
			for (int entryIndex = 0; entryIndex < data.size(); entryIndex++) {
				for (int component = 0; component < data.get(entryIndex).length; component++) {
					osw.write(df.format(data.get(entryIndex)[component])
							+ "            ");
				}
				osw.write("\n");
			}
			osw.flush();
			osw.close();
			osw = null;
		} catch (FileNotFoundException e) {
			// System.out.println("the directory doesn't exist!");
			return false;
		} catch (IOException e) {
			// System.out.println("IOException occurs");
			return false;
		}
		return true;
	}
}
