package com.ureca.acousticfingerprint;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Andrew on 10/19/16.
 */

// In this case, the fragment displays simple text based on the page
public class ListenFragment extends Fragment {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static String[] files = {"holcim.wav", "hutch1.wav", "hutch2.wav", "janet1.wav", "janet2.wav", "keells1.wav", "keells2.wav", "keells3.wav", "keells4.wav", "keells5.wav"};
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    SharedPreferences sharedpreferences;
    private short[] audio;
    private TextView text;
    private Timer timer = null;
    private FloatingActionButton mRecordButton = null;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private int recordInterval;
    private TimerTask recordTask;
    private RecordRunnable runnable;
    private DBHelper dbHelper;

    public static ListenFragment newInstance() {
        return new ListenFragment();
    }

    private void onRecord(boolean isRecording) {
        if (!isRecording) {
            AudioMatching.reset();
            startRecording();
            mRecordButton.setImageResource(R.drawable.ic_stop_white_36px);
            mRecordButton.setSoundEffectsEnabled(true);
            text.setText("Click on the button to stop recording");
        } else {
            stopRecording();
            mRecordButton.setImageResource(R.drawable.ic_hearing_white_36px);
            mRecordButton.setSoundEffectsEnabled(false);
            text.setText("Click on the button to start recording");
        }
    }

    private void startRecording() {
        recorder.startRecording();
        isRecording = true;
        runnable = new RecordRunnable();
        recordingThread = new Thread(runnable, "AudioRecorder Thread");
        recordingThread.start();
        recordTask = new RecordTask();
        timer = new Timer();
        timer.schedule(recordTask, recordInterval * 1000);
    }

    private void stopRecording() {
        if (recorder != null && runnable != null) {
            recordTask.cancel();
            runnable.stop();
            runnable = null;
            recordingThread = null;
            recorder.stop();
            isRecording = false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page, container, false);
        text = (TextView) view.findViewById(R.id.text);
        mRecordButton = (FloatingActionButton) view.findViewById(R.id.start_record);
        mRecordButton.setImageResource(R.drawable.ic_hearing_white_36px);
        mRecordButton.setSoundEffectsEnabled(false);

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            0);
                else
                    onRecord(isRecording);
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        recordInterval = Integer.parseInt(sharedpreferences.getString("recInterval", "2"));
        audio = new short[RECORDER_SAMPLERATE * recordInterval];
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BUFFER_SIZE);
        // Renew database upon AudioAnalysis parameters change
        /*
        dbHelper = new DBHelper(getContext());
        dbHelper.refreshDatabase();
        for (int i = 0; i < files.length; i++) {
            String fileNames = files[i];
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileNames);
            byte[] imgDataBa = new byte[(int) file.length()];

            DataInputStream dataIs;
            try {
                dataIs = new DataInputStream(new FileInputStream(file));
                dataIs.readFully(imgDataBa);
            } catch (IOException e) {
                e.printStackTrace();
            }

            short[] shorts = new short[imgDataBa.length / 2];
            // to turn bytes to shorts as either big endian or little endian.
            ByteBuffer.wrap(imgDataBa).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

            ArrayList<int[]> peaks = AudioAnalysis.analyze(shorts);
            ArrayList<Fingerprint> fingerprints = AudioHashing.hash(peaks);

            for (Fingerprint f : fingerprints)
                dbHelper.insertFingerprint(f.getAnchorFrequency(), f.getPointFrequency(), f.getDelta(), f.getAbsoluteTime(), i);

        }
        */
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isRecording) {
            onRecord(true);
        }
    }

    private class RecordTask extends TimerTask {
        public void run() {
            if (recorder != null) {
                stopRecording();
                ArrayList<int[]> peaks = AudioAnalysis.analyze(audio);
                ArrayList<Fingerprint> fingerprints = AudioHashing.hash(peaks);
                audio = new short[RECORDER_SAMPLERATE * recordInterval];
                startRecording();
                int[] match = AudioMatching.match(fingerprints, new DBHelper(getContext()));
                if (match != null && isRecording) {
                    final String MATCHING_AD = files[match[0]];
                    final int COUNT = match[1];
                    text.post(new Runnable() {
                        public void run() {
                            text.setText("Match: " + MATCHING_AD + " " + COUNT);
                        }
                    });
                }
            }
        }
    }

    private class RecordRunnable implements Runnable {
        private volatile boolean isStopped = false;

        public void run() {
            if (!isStopped) {
                short sData[] = new short[BUFFER_SIZE / 2];
                int index = 0;
                while (isRecording) {
                    recorder.read(sData, 0, BUFFER_SIZE / 2);
                    if (index + sData.length <= audio.length) {
                        System.arraycopy(sData, 0, audio, index, sData.length);
                        index += sData.length;
                    }
                }
            }
        }

        void stop() {
            isStopped = true;
        }
    }
}
