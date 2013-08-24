package edu.wisc.jj;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class NativeRecorder extends Activity implements Runnable {
	private String fileName;
	private List<String> script;
	private Handler toastHandler;
	private Runnable toastRunnable;
	private Context mContext;

	public NativeRecorder(String mName, List<String> cmd, Context context){
		this.fileName=mName;
		this.script=cmd;
		this.mContext=context;
		Log.d("nativeRecorder", "constructed a native recorder. scripts: "+cmd.toString());
		toastHandler = new Handler();
		toastRunnable = new Runnable() {
			public void run() {
				Toast.makeText(mContext,"audio finished recording",Toast.LENGTH_SHORT).show();
			}
		};
	}

	@Override
	public void run() {
		try {
			Log.d("nativeRecorder", "start running");
			Process p = null;
			p = Runtime.getRuntime().exec("/system/xbin/su");
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			Thread.sleep(3000);
			for (String mLine : this.script) {
				if (mLine.contains("alsa_aplay")) {
					mLine += " /sdcard/nativeRecorder/" + fileName + ".wav\n";
					Log.d(this.toString(), "executing cmd: " + mLine);
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
			Log.d(this.toString(), "audio finished recording. ");
			String exitCmd = "exit\n";
			os.writeBytes(exitCmd);
			os.flush();
			os.close();
			reader.close();
			// p.waitFor();
			// p.destroy();
			// Toast.makeText(this, "audio finished",
			// Toast.LENGTH_SHORT).show();
			Log.d(this.toString(), "audio thread finished");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		} finally {
			toastHandler.post(toastRunnable);
		}
	}
}
