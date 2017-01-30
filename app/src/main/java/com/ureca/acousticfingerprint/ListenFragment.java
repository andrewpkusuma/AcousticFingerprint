package com.ureca.acousticfingerprint;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
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
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    SharedPreferences sharedpreferences;
    private short[] audio;
    private TextView text;
    //private String randomName = UUID.randomUUID().toString().substring(0, 7);
    //private String filePath = null;
    private Timer timer = null;
    private FloatingActionButton mRecordButton = null;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private int recordInterval;
    private String[] files = {"holcim.wav", "hutch1.wav", "hutch2.wav", "janet1.wav", "janet2.wav"};
    private int[] match;
    private TimerTask recordTask;
    private RecordRunnable runnable;

    public static ListenFragment newInstance() {
        return new ListenFragment();
    }

    private void onRecord(boolean isRecording) {
        if (!isRecording) {
            match = new int[files.length];
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
        timer.scheduleAtFixedRate(recordTask, recordInterval * 1000, recordInterval * 1000);
    }

    private void stopRecording() {
        if (recorder != null) {
            timer.cancel();
            timer.purge();
            recordTask.cancel();
            runnable.stop();
            runnable = null;
            recordingThread = null;
            recorder.stop();
            isRecording = false;
            //recorder.release();
            //recorder = null;
        }
    }

    private void writeAudioDataToArray() {
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
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            0);

                } else {
                    onRecord(isRecording);
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        recordInterval = Integer.parseInt(sharedpreferences.getString("recInterval", "5"));
        audio = new short[RECORDER_SAMPLERATE * recordInterval];
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BUFFER_SIZE);
        /*
        dbHelper.refreshDatabase();
        for (int i = 0; i < files.length; i++) {
            String fileNames = files[i];
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileNames);
            byte[] imgDataBa = new byte[(int) file.length()];

            DataInputStream dataIs;
            try {
                dataIs = new DataInputStream(new FileInputStream(file));
                dataIs.readFully(imgDataBa);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            short[] shorts = new short[imgDataBa.length / 2];
            // to turn bytes to shorts as either big endian or little endian.
            ByteBuffer.wrap(imgDataBa).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

            ArrayList<Fingerprint> fingerprints = AudioAnalysis.fingerprint(shorts);

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

    class RecordTask extends TimerTask {
        public void run() {
            if (recorder != null) {
                stopRecording();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRecordButton.setVisibility(View.INVISIBLE);
                    }
                });
                ArrayList<Fingerprint> fingerprints = AudioAnalysis.fingerprint(audio);
                audio = new short[RECORDER_SAMPLERATE * recordInterval];
                startRecording();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRecordButton.setVisibility(View.VISIBLE);
                    }
                });
                DBHelper dbHelper = new DBHelper(getContext());
                HashMap<ArrayList<Integer>, ArrayList<Integer>> targetZoneMap = new HashMap<>();
                SparseIntArray[] timeCoherencyMap = new SparseIntArray[files.length];
                for (int i = 0; i < files.length; i++)
                    timeCoherencyMap[i] = new SparseIntArray();
                for (Fingerprint f : fingerprints) {
                    Cursor couples = dbHelper.getData(f.getAnchorFrequency(), f.getPointFrequency(), f.getDelta());
                    if (couples.moveToFirst()) {
                        do {
                            Integer id = couples.getInt(couples.getColumnIndex("song_id"));
                            Integer absoluteTime = couples.getInt(couples.getColumnIndex("absolute_time"));
                            Integer delta = f.getAbsoluteTime() - absoluteTime;
                            ArrayList<Integer> couple = new ArrayList<>();
                            couple.add(id);
                            couple.add(absoluteTime);
                            ArrayList<Integer> a;
                            if ((a = targetZoneMap.get(couple)) != null) {
                                a.add(delta);
                                targetZoneMap.put(couple, a);
                            } else {
                                a = new ArrayList<>();
                                a.add(delta);
                                targetZoneMap.put(couple, a);
                            }
                        } while (couples.moveToNext());
                    }
                    couples.close();
                    dbHelper.close();
                }
                for (ArrayList<Integer> i : targetZoneMap.keySet()) {
                    ArrayList<Integer> a = targetZoneMap.get(i);
                    if (a.size() >= 4)
                        for (Integer delta : a) {
                            Integer count = timeCoherencyMap[i.get(0)].get(delta);
                            timeCoherencyMap[i.get(0)].put(delta, count == 0 ? 1 : count + 1);
                        }
                }
                    /*Integer count = map[id].get(delta);
                            map[id].put(delta, count == 0 ? 1 : count + 1);*/
                for (int i = 0; i < match.length; i++) {
                    SparseIntArray s = timeCoherencyMap[i];
                    int currentMaxDeltaCount = 0;
                    for (int j = 0; j < s.size(); j++) {
                        Integer delta = s.keyAt(j);
                        if (s.get(delta) >= currentMaxDeltaCount) {
                            currentMaxDeltaCount = s.get(delta);
                        }
                    }
                    match[i] += currentMaxDeltaCount;
                }
                int maxTemp = 0, maximum = -1;
                for (int i = 0; i < match.length; i++) {
                    if (match[i] > maxTemp) {
                        maximum = i;
                        maxTemp = match[i];
                    }
                }
                if (maximum >= 0) {
                    final String MATCHING_AD = files[maximum];
                    final int COUNT = match[maximum];
                    text.post(new Runnable() {
                        public void run() {
                            text.setText("Match: " + MATCHING_AD + " " + COUNT);
                        }
                    });
                }
                /*
                Writer fileWriter;
                try {
                    fileWriter = new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + randomName + ".txt");
                    for (int i : match) {
                        fileWriter.write(i+"");
                        fileWriter.write(System.lineSeparator());
                    }
                    fileWriter.close();
                    /*
                    for (Fingerprint f : fingerprints) {
                        //dbHelper.insertFingerprint(f.getAnchorFrequency(), f.getPointFrequency(), f.getDelta(), f.getAbsoluteTime(), 0);
                        fileWriter.write(f.getAnchorFrequency() + " " + f.getPointFrequency() + " " + f.getDelta() + " " + f.getAbsoluteTime());
                        fileWriter.write(System.lineSeparator());
                    }
                    //fileWriter.write(String.valueOf(dbHelper.numberOfRows()));
                    fileWriter.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            */
            }
            //randomName = UUID.randomUUID().toString().substring(0, 7);
            //filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + randomName + ".pcm";
            /*

            */
        }
    }

    class RecordRunnable implements Runnable {
        private volatile boolean exit = false;

        public void run() {
            if (!exit)
                writeAudioDataToArray();
        }

        void stop() {
            exit = true;
        }
    }
}
