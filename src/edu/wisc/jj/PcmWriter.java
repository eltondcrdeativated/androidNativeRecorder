package edu.wisc.jj;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.os.Environment;
import android.util.Log;

public class PcmWriter implements Runnable {
	// this class has 2 threads, modifying the isReocrding variable, use mutex
	// to achieve synchronization
	private final Object mutex = new Object();
	private volatile boolean isRecording;
	private rawData rawData;
	private File pcmFile;
	private File pcmFileExtraChannel;
	DataOutputStream dataOutputStreamInstance;
	DataOutputStream dataOutputStreamInstanceStereo;
	private List<rawData> list;
	private boolean stereoRecording;

	/**
	 * 
	 * @param stereo
	 *            whether it is using stereo recording
	 */
	public PcmWriter(boolean stereo) {
		super();
		this.stereoRecording = stereo;
		File audioDataDirectory = new File(Environment
				.getExternalStorageDirectory().getAbsolutePath()
				+ "/audioData/");
		if (!audioDataDirectory.exists()) {
			Log.d(this.toString(), "audioData folder doesn't exsit");
			audioDataDirectory.mkdirs();
		}
		pcmFile = new File(audioDataDirectory, "test" + ".pcm");
		if (this.stereoRecording) {
			pcmFileExtraChannel = new File(audioDataDirectory, "test"
					+ "_1.pcm");
		}
		list = Collections.synchronizedList(new LinkedList<rawData>());
	}

	/**
	 * 
	 * @param stereo whether is using stereo recording
	 * @param fileName
	 */
	public PcmWriter(boolean stereo, String fileName) {
		super();
		this.stereoRecording = stereo;
		File audioDataDirectory = new File(Environment
				.getExternalStorageDirectory().getAbsolutePath()
				+ "/audioData/");
		if (!audioDataDirectory.exists()) {
			Log.d(this.toString(), "audioData folder doesn't exsit");
			audioDataDirectory.mkdirs();
		}
		pcmFile = new File(audioDataDirectory, fileName + ".pcm");
		if (this.stereoRecording) {
			pcmFileExtraChannel = new File(audioDataDirectory, fileName
					+ "_1.pcm");
		}
		list = Collections.synchronizedList(new LinkedList<rawData>());
	}

	public void init() {
		BufferedOutputStream bufferedStreamInstance = null;
		if (pcmFile.exists()) {
			pcmFile.delete();
			Log.d(this.toString(), pcmFile.getName() + " exists. DELETED");
		}
		try {
			pcmFile.createNewFile();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot create file: "
					+ pcmFile.toString());
		}
		try {
			bufferedStreamInstance = new BufferedOutputStream(
					new FileOutputStream(pcmFile));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Cannot Open File", e);
		}
		dataOutputStreamInstance = new DataOutputStream(bufferedStreamInstance);
		if (this.stereoRecording) {
			BufferedOutputStream bufferedStreamInstanceStereo = null;
			if (pcmFileExtraChannel.exists()) {
				pcmFileExtraChannel.delete();
				Log.d(this.toString(), pcmFileExtraChannel.getName()
						+ " exists. DELETED");
			}
			try {
				pcmFileExtraChannel.createNewFile();
			} catch (IOException e) {
				throw new IllegalStateException("Cannot create file: "
						+ pcmFileExtraChannel.toString());
			}
			try {
				bufferedStreamInstanceStereo = new BufferedOutputStream(
						new FileOutputStream(pcmFileExtraChannel));
			} catch (FileNotFoundException e) {
				throw new IllegalStateException("Cannot Open File", e);
			}
			dataOutputStreamInstanceStereo = new DataOutputStream(
					bufferedStreamInstanceStereo);
		}
	}

	public void run() {
		Log.d(this.toString(), "pcmwriter thread runing");
		while (this.isRecording()) {
			if (list.size() > 0) {
				rawData = list.remove(0);
				try {// if just recording mono
					if (!this.stereoRecording) {
						// Log.d(this.pcmFile.getName(), "raw data size" +
						// rawData.size);
						for (int i = 0; i < rawData.size; ++i) {
							dataOutputStreamInstance.writeShort(rawData.pcm[i]);
						}
					} else {
						// if doing stereo. separate channels
						// for (int i = 0; i + 1 < rawData.size; i = i + 2) {
						// if (rawData.pcm[i]!=rawData.pcm[i+1]){
						// Log.d(this.pcmFile.getName(),"horray!!! different value. index: "+i);
						// }
						// dataOutputStreamInstance.writeShort(rawData.pcm[i]);
						// dataOutputStreamInstanceStereo.writeShort(rawData.pcm[i+1]);
						// }
						for (int i = 0; i < rawData.size; i++) {
							if (i!=0 && i%2!=0 && rawData.pcm[i] != rawData.pcm[i-1]) {
								Log.d(this.pcmFile.getName(),
										"horray!!! different value. index: "
												+ i);
							}
							dataOutputStreamInstance.writeShort(rawData.pcm[i]);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					Log.d(this.pcmFile.getName(), "IO exception");
				}
			} else {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
		stop();
	}

	public void putData(short[] buf, int size) {
		// Log.d(this.pcmFile.getName(), "put data size "+size);
		synchronized (this.list) {
			rawData data = new rawData(size);
			System.arraycopy(buf, 0, data.pcm, 0, size);
			list.add(data);
		}
	}

	public void stop() {
		try {
			dataOutputStreamInstance.flush();
			dataOutputStreamInstance.close();
			if (this.stereoRecording) {
				dataOutputStreamInstanceStereo.flush();
				dataOutputStreamInstanceStereo.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.d(this.toString(), "PCMWriter thread finished");
	}

	public void setRecording(boolean isRecording) {
		synchronized (mutex) {
			this.isRecording = isRecording;
			if (this.isRecording) {
				mutex.notify();
				Log.d(this.pcmFile.getName(), "set recording to "
						+ this.isRecording);
			}
		}
	}

	public boolean isRecording() {
		synchronized (mutex) {
			return isRecording;
		}
	}

	class rawData {
		int size;
		short[] pcm;

		public rawData(int size) {
			super();
			this.size = size;
			this.pcm = new short[size];
		}
	}
}
