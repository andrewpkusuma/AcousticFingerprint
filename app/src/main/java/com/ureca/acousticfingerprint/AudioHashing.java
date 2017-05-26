package com.ureca.acousticfingerprint;

import java.util.ArrayList;

/**
 * Created by Andrew on 26/5/17.
 */

public class AudioHashing {

    private static final int ANCHOR_DISTANCE = 3;
    private static final int TARGET_ZONE_SIZE = 5;

    public static ArrayList<Fingerprint> hash(ArrayList<int[]> peak) {

        ArrayList<Fingerprint> fingerprints = new ArrayList<>();

        for (int i = 0; i <= peak.size() - (ANCHOR_DISTANCE + TARGET_ZONE_SIZE); i++) {
            for (int j = i + ANCHOR_DISTANCE; j < i + ANCHOR_DISTANCE + TARGET_ZONE_SIZE; j++) {
                int[] anchor = peak.get(i);
                int[] point = peak.get(j);
                fingerprints.add(new Fingerprint((short) anchor[1], (short) point[1], (byte) (point[0] - anchor[0]), (short) anchor[0], 0));
            }
        }

        return fingerprints;

    }

}
