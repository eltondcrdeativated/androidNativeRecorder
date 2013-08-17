package edu.wisc.jj;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class PcmRecorder implements Runnable {

	private volatile boolean isRecording;
	private final Object mutex = new Object();
	private static final int frequency = 44100;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private String mChar;
	private int index;
	private boolean twoMicroPhone;
	private int micSource;

	public PcmRecorder() {
		super();
	}

	/**
	 * the microphonesource specified is only valid when use2Mic is false
	 * @param mChar
	 * @param index
	 * @param use2Mic
	 * @param microPhoneSource
	 */
	public PcmRecorder(String mChar, int index, boolean use2Mic,
			int microPhoneSource) {
		super();
		this.mChar = mChar;
		this.index = index;
		this.twoMicroPhone = use2Mic;
		this.micSource = microPhoneSource;
	}

	public void run() {
		PcmWriter pcmWriter1 = new PcmWriter(false, mChar
				+ String.valueOf(index));
		pcmWriter1.init();
		Thread writer1Thread = new Thread(pcmWriter1);
		pcmWriter1.setRecording(true);
		writer1Thread.start();

		PcmWriter pcmWriter2=null;
		if (this.twoMicroPhone) {
			pcmWriter2 = new PcmWriter(false, mChar
					+ String.valueOf(index)+"_2");
			pcmWriter2.init();
			Thread writer2Thread = new Thread(pcmWriter2);
			pcmWriter2.setRecording(true);
			writer2Thread.start();
		}

		synchronized (mutex) {
			while (!this.isRecording) {
				try {
					// used with notify() in setRecording() method
					mutex.wait();
				} catch (InterruptedException e) {
					throw new IllegalStateException("Wait() interrupted!", e);
				}
			}
		}
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		if (this.twoMicroPhone) {
			int bufferRead1 = 0;
			int bufferRead2 = 0;
			int bufferSize1 = AudioRecord.getMinBufferSize(frequency,
					AudioFormat.CHANNEL_IN_MONO, audioEncoding);
			int bufferSize2 = AudioRecord.getMinBufferSize(frequency,
					AudioFormat.CHANNEL_IN_MONO, audioEncoding);
			// if doesn't support that sampling frequency
			if (bufferSize1 == AudioRecord.ERROR_BAD_VALUE
					|| bufferSize1 == AudioRecord.ERROR) {
				Log.i(this.toString(), "doesn't support sampling rate of "
						+ frequency);
				throw new IllegalArgumentException(
						"entered unsupported audio sampling rate");
			}
			// grabbing 16-bit pcm audio
			short[] tempBuffer1 = new short[bufferSize1];
			short[] tempBuffer2 = new short[bufferSize2];
			AudioRecord recordInstance1 = new AudioRecord(
					MediaRecorder.AudioSource.MIC, frequency,
					AudioFormat.CHANNEL_IN_MONO, audioEncoding, bufferSize1);
			AudioRecord recordInstance2 = new AudioRecord(
					MediaRecorder.AudioSource.CAMCORDER, frequency,
					AudioFormat.CHANNEL_IN_MONO, audioEncoding, bufferSize2);
			Log.d("PcmRecorder", "created 2 audioRecorder instance");
			recordInstance1.startRecording();
			recordInstance2.startRecording();
			Log.d("PcmRecorder", "both started recording");
			while (this.isRecording) {
				bufferRead1 = recordInstance1.read(tempBuffer1, 0, bufferSize1);
				bufferRead2 = recordInstance2.read(tempBuffer2, 0, bufferSize2);
				if (bufferRead1 == AudioRecord.ERROR_INVALID_OPERATION
						|| bufferRead2 == AudioRecord.ERROR_INVALID_OPERATION) {
					throw new IllegalStateException(
							"read() returned AudioRecord.ERROR_INVALID_OPERATION");
				} else if (bufferRead1 == AudioRecord.ERROR_BAD_VALUE
						|| bufferRead2 == AudioRecord.ERROR_BAD_VALUE) {
					throw new IllegalStateException(
							"read() returned AudioRecord.ERROR_BAD_VALUE");
				} else if (bufferRead1 == AudioRecord.ERROR_INVALID_OPERATION
						|| bufferRead2 == AudioRecord.ERROR_INVALID_OPERATION) {
					throw new IllegalStateException(
							"read() returned AudioRecord.ERROR_INVALID_OPERATION");
				}
				pcmWriter1.putData(tempBuffer1, bufferRead1);
				Log.d(this.toString(),"1 put data done!");
				pcmWriter2.putData(tempBuffer2, bufferRead2);
				Log.d(this.toString(),"2 put data done");
			}
			recordInstance1.stop();
			recordInstance1.release();
			pcmWriter1.setRecording(false);
			Log.d(this.toString(),"1 stopped");
			recordInstance2.stop();
			recordInstance2.release();
			pcmWriter2.setRecording(false);			
			Log.d(this.toString(),"2 stopped");
		} else {
			Log.d("PcmRecorder", "using single microphone. Source index: "+this.micSource);
			int bufferRead = 0;
			int bufferSize = AudioRecord.getMinBufferSize(frequency,
					AudioFormat.CHANNEL_IN_MONO, audioEncoding);
			// if doesn't support that sampling frequency
			if (bufferSize == AudioRecord.ERROR_BAD_VALUE
					|| bufferSize == AudioRecord.ERROR) {
				Log.i(this.toString(), "doesn't support sampling rate of "
						+ frequency);
				throw new IllegalArgumentException(
						"entered unsupported audio sampling rate");
			}
			// grabbing 16-bit pcm audio
			short[] tempBuffer = new short[bufferSize];
			AudioRecord recordInstance = new AudioRecord(this.micSource,
					frequency, AudioFormat.CHANNEL_IN_MONO, audioEncoding,
					bufferSize);
			recordInstance.startRecording();
			while (this.isRecording) {
				bufferRead = recordInstance.read(tempBuffer, 0, bufferSize);
				if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
					throw new IllegalStateException(
							"read() returned AudioRecord.ERROR_INVALID_OPERATION");
				} else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
					throw new IllegalStateException(
							"read() returned AudioRecord.ERROR_BAD_VALUE");
				} else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
					throw new IllegalStateException(
							"read() returned AudioRecord.ERROR_INVALID_OPERATION");
				}
				pcmWriter1.putData(tempBuffer, bufferRead);
				// Log.d(this.toString(),"put data done!");
			}
			recordInstance.stop();
			recordInstance.release();
			pcmWriter1.setRecording(false);
		}
		Log.d(this.toString(), "PCMRecorder thread finished");
	}

	public void setRecording(boolean isRecording) {
		synchronized (mutex) {
			this.isRecording = isRecording;
			if (this.isRecording) {
				mutex.notify();
			}
		}
	}

	public boolean isRecording() {
		synchronized (mutex) {
			return isRecording;
		}
	}
}
