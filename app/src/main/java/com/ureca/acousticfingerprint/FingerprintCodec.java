package com.ureca.acousticfingerprint;

import java.nio.ByteBuffer;

/**
 * Created by Andrew on 20/4/17.
 */

public class FingerprintCodec {
    private long address;
    private long couple;
    private Fingerprint fingerprint;

    public FingerprintCodec(Fingerprint fingerprint) {
        this.fingerprint = fingerprint;
    }

    public FingerprintCodec(int address, long couple) {
        this.address = address;
        this.couple = couple;
    }

    public static byte[] toByteArray(short value) {
        byte[] bytes = new byte[2];
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.putShort(value);
        return buffer.array();
    }

    public static byte[] toByteArray(int value) {
        byte[] bytes = new byte[4];
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.putInt(value);
        return buffer.array();
    }

    public static byte[] toByteArray(long value) {
        byte[] bytes = new byte[8];
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.putLong(value);
        return buffer.array();
    }

    public void encode() {
        byte[] addressByte = new byte[8];
        System.arraycopy(toByteArray(fingerprint.getAnchorFrequency()), 0, addressByte, 0, 2);
        System.arraycopy(toByteArray(fingerprint.getPointFrequency()), 0, addressByte, 2, 2);
        addressByte[4] = fingerprint.getDelta();
        address = ByteBuffer.wrap(addressByte).getLong();

        byte[] coupleByte = new byte[8];
        System.arraycopy(toByteArray(fingerprint.getAbsoluteTime()), 0, coupleByte, 0, 4);
        System.arraycopy(toByteArray(fingerprint.getSongID()), 0, coupleByte, 4, 4);
        couple = ByteBuffer.wrap(coupleByte).getLong();
    }

    public void decode() {
        byte[] addressByte = toByteArray(address);
        byte[] anchorFrequency = new byte[2];
        byte[] pointFrequency = new byte[2];
        byte delta;
        System.arraycopy(addressByte, 3, anchorFrequency, 0, 2);
        System.arraycopy(addressByte, 5, pointFrequency, 0, 2);
        delta = addressByte[7];

        byte[] coupleByte = toByteArray(couple);
        byte[] absoluteTime = new byte[4];
        byte[] songID = new byte[4];
        System.arraycopy(coupleByte, 0, absoluteTime, 0, 4);
        System.arraycopy(coupleByte, 5, pointFrequency, 0, 4);

        fingerprint = new Fingerprint(ByteBuffer.wrap(anchorFrequency).getShort(), ByteBuffer.wrap(pointFrequency).getShort(), delta, ByteBuffer.wrap(absoluteTime).getShort(), ByteBuffer.wrap(songID).getInt());
    }
}
