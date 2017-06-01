package com.ureca.acousticfingerprint;

/**
 * Created by Andrew on 1/27/17.
 */

public class Fingerprint {
    private short anchorFrequency;
    private short pointFrequency;
    private byte delta;
    private short absoluteTime;
    private int adID;

    public Fingerprint(short anchorFrequency, short pointFrequency, byte delta, short absoluteTime, int adID) {
        this.anchorFrequency = anchorFrequency;
        this.pointFrequency = pointFrequency;
        this.delta = delta;
        this.absoluteTime = absoluteTime;
        this.adID = adID;
    }

    public short getAnchorFrequency() {
        return anchorFrequency;
    }

    public short getPointFrequency() {
        return pointFrequency;
    }

    public byte getDelta() {
        return delta;
    }

    public short getAbsoluteTime() {
        return absoluteTime;
    }

    public int getAdID() {
        return adID;
    }
}
