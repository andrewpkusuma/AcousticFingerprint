package com.ureca.acousticfingerprint;

import android.database.Cursor;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.HashMap;

import static com.ureca.acousticfingerprint.ListenFragment.files;

/**
 * Created by Andrew on 26/5/17.
 */

public class AudioMatching {

    private static int[] match;

    public static int[] match(ArrayList<Fingerprint> fingerprints, DBHelper dbHelper) {
        HashMap<ArrayList<Integer>, ArrayList<Integer>> targetZoneMap = new HashMap<>();
        SparseIntArray[] timeCoherencyMap = new SparseIntArray[files.length];
        for (int i = 0; i < files.length; i++)
            timeCoherencyMap[i] = new SparseIntArray();
        for (Fingerprint f : fingerprints) {
            Cursor couples = dbHelper.getFingerprintCouples(f.getAnchorFrequency(), f.getPointFrequency(), f.getDelta());
            if (couples.moveToFirst()) {
                do {
                    Integer id = couples.getInt(couples.getColumnIndex("ad_id"));
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
            if (a.size() >= 3)
                for (Integer delta : a) {
                    Integer count = timeCoherencyMap[i.get(0)].get(delta);
                    timeCoherencyMap[i.get(0)].put(delta, count == 0 ? 1 : count + 1);
                }
        }
        // Currently assume most common delta is the correct delta
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

        int[] result;
        if (maximum >= 0)
            result = new int[]{maximum, match[maximum]};
        else
            result = null;
        return result;
    }

    public static void reset() {
        match = new int[files.length];
    }

}
