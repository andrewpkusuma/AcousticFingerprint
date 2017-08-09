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
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private SparseIntArray match;

    public static ListenFragment newInstance() {
        return new ListenFragment();
    }

    private void onRecord(boolean isRecording) {
        if (!isRecording) {
            AudioMatching.reset(new DBHelper(getContext()));
            cycleCount = 0;
            match = new SparseIntArray();
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
                audio = new short[RECORDER_SAMPLERATE * recordInterval];
                startRecording();
                ArrayList<Fingerprint> fingerprints = AudioHashing.hash(peaks);

                final RequestQueue queue = Volley.newRequestQueue(getContext());
                final Gson gson = new Gson();

                JSONArray fingerprintJsonArray = null;
                try {
                    fingerprintJsonArray = new JSONArray(gson.toJson(fingerprints));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonArrayRequest scoreRequest = new JsonArrayRequest(Request.Method.POST, "http://192.168.0.14/fingerprint_score.php", fingerprintJsonArray, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Score[] update = gson.fromJson(response.toString(), new TypeToken<Score[]>() {
                        }.getType());
                        for (Score s : update)
                            match.put(s.getAdID(), match.get(s.getAdID()) + s.getScore());

                        int maxTemp = 0;
                        int matchID = -1;

                        for (int i = 0; i < match.size(); i++) {
                            int currentKey = match.keyAt(i);
                            int currentValue = match.get(currentKey);
                            if (currentValue > maxTemp) {
                                matchID = currentKey;
                                maxTemp = currentValue;
                            }
                        }

                        if (maxTemp >= 10 && matchID >= 0) {
                            JSONObject jsonObject = null;
                            try {
                                jsonObject = new JSONObject().put("adID", matchID);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            JsonObjectRequest adRequest = new JsonObjectRequest(Request.Method.POST, "http://192.168.0.14/ad_search.php", jsonObject, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    final Advertisement advertisement = gson.fromJson(response.toString(), new TypeToken<Advertisement>() {
                                    }.getType());
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
                                            matchingAdDisplay.setNestedScrollingEnabled(false);
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
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    //
                                }
                            });
                            queue.add(adRequest);
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //
                    }
                });
                queue.add(scoreRequest);

                /*
                int[] match = AudioMatching.match(fingerprints, getContext());
                if (match != null && isRecording) {
                    if (match[1] > 10) {
                        DisplayAndStoreAdRunnable displayAndStoreAdRunnable = new DisplayAndStoreAdRunnable();
                        displayAndStoreAdRunnable.setMatch(match[0]);
                        displayAndStoreAdRunnable.run();
                    }
                }
                */

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
                short sData[];
                int index = 0;
                while (isRecording) {
                    sData = new short[BUFFER_SIZE / 2];
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
            dbHelper.close();
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
                    matchingAdDisplay.setNestedScrollingEnabled(false);
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
