package com.ureca.acousticfingerprint;

import android.Manifest;
import android.content.Context;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
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
    // public static String[] files = {"holcim.wav", "hutch1.wav", "hutch2.wav", "janet1.wav", "janet2.wav", "keells1.wav", "keells2.wav", "keells3.wav", "keells4.wav", "keells5.wav"};
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    private static int recordInterval;
    private static int cycleLimit;
    SharedPreferences sharedpreferences;
    private int cycleCount;
    private short[] audio;
    private TextView text;
    private LinearLayout placeholder;
    private FloatingActionButton mRecordButton = null;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private TimerTask recordTask;
    private RecordRunnable recordRunnable;
    private MyInterface listener;

    public static ListenFragment newInstance() {
        return new ListenFragment();
    }

    private void onRecord(boolean isRecording) {
        if (!isRecording) {
            AudioMatching.reset(new DBHelper(getContext()));
            cycleCount = 0;
            startRecording();
            mRecordButton.setImageResource(R.drawable.ic_stop_white_36px);
            mRecordButton.setSoundEffectsEnabled(true);
            placeholder.removeAllViews();
            text.setText("Click on the button to stop recording");
            placeholder.addView(text);
        } else {
            stopRecording();
            mRecordButton.setImageResource(R.drawable.ic_hearing_white_36px);
            mRecordButton.setSoundEffectsEnabled(false);
            placeholder.removeAllViews();
            text.setText("Click on the button to start recording");
            placeholder.addView(text);
        }
    }

    private void startRecording() {
        recorder.startRecording();
        isRecording = true;
        recordRunnable = new RecordRunnable();
        recordingThread = new Thread(recordRunnable, "AudioRecorder Thread");
        recordingThread.start();
        recordTask = new RecordTask();
        Timer timer = new Timer();
        timer.schedule(recordTask, recordInterval * 1000);
    }

    private void stopRecording() {
        if (recorder != null && recordRunnable != null) {
            recordTask.cancel();
            recordRunnable.stop();
            recordRunnable = null;
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
        View view = inflater.inflate(R.layout.fragment_listen, container, false);

        placeholder = (LinearLayout) view.findViewById(R.id.placeholder);
        placeholder.setOrientation(LinearLayout.VERTICAL);
        text = new TextView(getContext());
        text.setGravity(Gravity.CENTER);
        text.setText("Click on the button to start recording");
        placeholder.addView(text);

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
        cycleLimit = Integer.parseInt(sharedpreferences.getString("cycleLimit", "10"));
        audio = new short[RECORDER_SAMPLERATE * recordInterval];
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BUFFER_SIZE);
        // Renew database upon AudioAnalysis parameters change
        /*
        dbHelper = new DBHelper(getContext());
        dbHelper.refreshDatabase();

        dbHelper.insertAd("Holcim", "Contact Details\n" +
                "\n" +
                "Address    -  Hagenholzstrasse 85\n" +
                "              CH - 8050 Zurich\n" +
                "              Switzerland.\n" +
                "\n" +
                "Phone      -  +41 58 858 5858\n" +
                "Fax        -  +41 58 858 5859", "http://www.holcim.com", R.drawable.test, 0);
        dbHelper.insertAd("Hutch", "Contact Details\n" +
                "\n" +
                "Address             -  Hutchison Telecommunications\n" +
                "\t\t       Lanka (Pvt) Ltd,\n" +
                "                       234, Galle Road, Colombo 04, \n" +
                "                       Sri Lanka.\n" +
                "\n" +
                "Telephone Number    -  1788\n" +
                "Send SMS            -  5555 \n" +
                "Email               -  cs@hutchison.lk", "https://www.hutch.lk", R.drawable.test, 1);
        dbHelper.insertAd("Hutch", "Contact Details\n" +
                "\n" +
                "Address             -  Hutchison Telecommunications\n" +
                "\t\t       Lanka (Pvt) Ltd,\n" +
                "                       234, Galle Road, Colombo 04, \n" +
                "                       Sri Lanka.\n" +
                "\n" +
                "Telephone Number    -  1788\n" +
                "Send SMS            -  5555 \n" +
                "Email               -  cs@hutchison.lk", "https://www.hutch.lk", R.drawable.test, 2);
        dbHelper.insertAd("Janet", "Contact Details\n" +
                "\n" +
                "Address -   The Janet Group.\n" +
                "            Level #1\n" +
                "            No 269, Galle Road,\n" +
                "            Mount Lavinia.\n" +
                "\n" +
                "Sri Lanka Tel - +94 114 200022\n" +
                "Fax           - +94 114 200024\n" +
                "E-mail        - info@janet-ayurveda.com\n" +
                "\n" +
                "Janet Salons\n" +
                "\n" +
                "1. No. 3, Castle Avenue, Colombo 8.\n" +
                "2. No. 15, Sinsapa Road, Colombo 6.\n" +
                "3. Keells Super Building, No. 126 B , Highlevel Road, Nugegoda.", "http://janet-ayurveda.com/", R.drawable.test, 3);
        dbHelper.insertAd("Janet", "Contact Details\n" +
                "\n" +
                "Address -   The Janet Group.\n" +
                "            Level #1\n" +
                "            No 269, Galle Road,\n" +
                "            Mount Lavinia.\n" +
                "\n" +
                "Sri Lanka Tel - +94 114 200022\n" +
                "Fax           - +94 114 200024\n" +
                "E-mail        - info@janet-ayurveda.com\n" +
                "\n" +
                "Janet Salons\n" +
                "\n" +
                "1. No. 3, Castle Avenue, Colombo 8.\n" +
                "2. No. 15, Sinsapa Road, Colombo 6.\n" +
                "3. Keells Super Building, No. 126 B , Highlevel Road, Nugegoda.", "http://janet-ayurveda.com/", R.drawable.test, 4);
        dbHelper.insertAd("Keells Super", "Contact Details \n" +
                "\n" +
                "\"John Keells Holdings PLC\"\n" +
                "\n" +
                "Jaykay Marketing Services Pvt Ltd.\n" +
                "No:148, Vauxhall Street,\n" +
                "Colombo 2, Sri Lanka.\n" +
                "\n" +
                "Telephone No.     - +94 11 2303500 \n" +
                "Text (SMS) 'Operations'  - +94 77 3762524 \n" +
                "Text (SMS) 'Technical'   - +94 77 3647586 Fax: +94 11 2303555\n" +
                "\n" +
                "\n" +
                "Email\n" +
                "Delivery Operations      - ksoperations.jms@keells.com\n" +
                "Technical                - web.jms@keells.com", "https://www.keellssuper.com", R.drawable.test, 5);
        dbHelper.insertAd("Keells Super", "Contact Details \n" +
                "\n" +
                "\"John Keells Holdings PLC\"\n" +
                "\n" +
                "Jaykay Marketing Services Pvt Ltd.\n" +
                "No:148, Vauxhall Street,\n" +
                "Colombo 2, Sri Lanka.\n" +
                "\n" +
                "Telephone No.     - +94 11 2303500 \n" +
                "Text (SMS) 'Operations'  - +94 77 3762524 \n" +
                "Text (SMS) 'Technical'   - +94 77 3647586 Fax: +94 11 2303555\n" +
                "\n" +
                "\n" +
                "Email\n" +
                "Delivery Operations      - ksoperations.jms@keells.com\n" +
                "Technical                - web.jms@keells.com", "https://www.keellssuper.com", R.drawable.test, 6);
        dbHelper.insertAd("Keells Super", "Contact Details \n" +
                "\n" +
                "\"John Keells Holdings PLC\"\n" +
                "\n" +
                "Jaykay Marketing Services Pvt Ltd.\n" +
                "No:148, Vauxhall Street,\n" +
                "Colombo 2, Sri Lanka.\n" +
                "\n" +
                "Telephone No.     - +94 11 2303500 \n" +
                "Text (SMS) 'Operations'  - +94 77 3762524 \n" +
                "Text (SMS) 'Technical'   - +94 77 3647586 Fax: +94 11 2303555\n" +
                "\n" +
                "\n" +
                "Email\n" +
                "Delivery Operations      - ksoperations.jms@keells.com\n" +
                "Technical                - web.jms@keells.com", "https://www.keellssuper.com", R.drawable.test, 7);
        dbHelper.insertAd("Keells Super", "Contact Details \n" +
                "\n" +
                "\"John Keells Holdings PLC\"\n" +
                "\n" +
                "Jaykay Marketing Services Pvt Ltd.\n" +
                "No:148, Vauxhall Street,\n" +
                "Colombo 2, Sri Lanka.\n" +
                "\n" +
                "Telephone No.     - +94 11 2303500 \n" +
                "Text (SMS) 'Operations'  - +94 77 3762524 \n" +
                "Text (SMS) 'Technical'   - +94 77 3647586 Fax: +94 11 2303555\n" +
                "\n" +
                "\n" +
                "Email\n" +
                "Delivery Operations      - ksoperations.jms@keells.com\n" +
                "Technical                - web.jms@keells.com", "https://www.keellssuper.com", R.drawable.test, 8);
        dbHelper.insertAd("Keells Super", "Contact Details \n" +
                "\n" +
                "\"John Keells Holdings PLC\"\n" +
                "\n" +
                "Jaykay Marketing Services Pvt Ltd.\n" +
                "No:148, Vauxhall Street,\n" +
                "Colombo 2, Sri Lanka.\n" +
                "\n" +
                "Telephone No.     - +94 11 2303500 \n" +
                "Text (SMS) 'Operations'  - +94 77 3762524 \n" +
                "Text (SMS) 'Technical'   - +94 77 3647586 Fax: +94 11 2303555\n" +
                "\n" +
                "\n" +
                "Email\n" +
                "Delivery Operations      - ksoperations.jms@keells.com\n" +
                "Technical                - web.jms@keells.com", "https://www.keellssuper.com", R.drawable.test, 9);


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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MyInterface) {
            listener = (MyInterface) context;
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    public interface MyInterface {
        void storeAd(Advertisement advertisement);
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
                    if (match[1] > 10) {
                        DisplayAndStoreAdRunnable displayAndStoreAdRunnable = new DisplayAndStoreAdRunnable();
                        displayAndStoreAdRunnable.setMatch(match[0]);
                        displayAndStoreAdRunnable.run();
                    }
                }
                if (cycleCount > cycleLimit) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onRecord(true);
                            placeholder.removeAllViews();
                            text.setText("No match found!" + "\n\n" +
                                    "Click on the button to start again");
                            placeholder.addView(text);
                        }
                    });
                }
                cycleCount++;
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

    private class DisplayAndStoreAdRunnable implements Runnable {
        int match = -1;
        String name;
        String details;
        String link;
        int imageID;

        @Override
        public void run() {
            DBHelper dbHelper = new DBHelper(getContext());
            final Cursor adDetails = dbHelper.getAdDetails(match);
            if (adDetails.moveToFirst()) {
                name = adDetails.getString(adDetails.getColumnIndex("name"));
                details = adDetails.getString(adDetails.getColumnIndex("details"));
                link = adDetails.getString(adDetails.getColumnIndex("link"));
                imageID = adDetails.getInt(adDetails.getColumnIndex("image_id"));
            }
            adDetails.close();
            final Advertisement advertisement = new Advertisement(name, details, link, imageID, match);
            listener.storeAd(advertisement);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRecord(true);
                    placeholder.removeAllViews();
                    TextView header = new TextView(getContext());
                    header.setGravity(Gravity.CENTER);
                    header.setText("Match found!");
                    placeholder.addView(header);
                    List<Advertisement> matchingAd = new ArrayList<>();
                    matchingAd.add(advertisement);
                    RecyclerView matchingAdDisplay = new RecyclerView(getContext());
                    matchingAdDisplay.setLayoutManager(new LinearLayoutManager(getContext()));
                    matchingAdDisplay.setHasFixedSize(true);
                    AdvertisementRecycleViewAdapter matchingAdDisplayAdapter = new AdvertisementRecycleViewAdapter(matchingAd, getContext());
                    matchingAdDisplay.setAdapter(matchingAdDisplayAdapter);
                    placeholder.addView(matchingAdDisplay);
                    TextView footer = new TextView(getContext());
                    footer.setGravity(Gravity.CENTER);
                    footer.setText("Click on the button to start recording");
                    placeholder.addView(footer);
                }
            });
        }

        public void setMatch(int match) {
            this.match = match;
        }
    }
}
