package com.ureca.acousticfingerprint;

/**
 * Created by Andrew on 31/7/17.
 */

public class Score {
    private int adID;
    private int score;

    public Score(int adID, int score) {
        this.adID = adID;
        this.score = score;
    }

    public int getAdID() {
        return adID;
    }

    public int getScore() {
        return score;
    }
}
