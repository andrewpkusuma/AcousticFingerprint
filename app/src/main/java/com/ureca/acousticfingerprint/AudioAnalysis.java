package com.ureca.acousticfingerprint;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;

public class AudioAnalysis {

    private static final int CHUNK_SIZE = 4096;
    //Range : {80, 320, 2560, 5120, 20000, 22050}
    private static final int[] RANGE = new int[]{30, 238, 476, 1858, 2048};
    private static final int ANCHOR_DISTANCE = 3;
    private static final int TARGET_ZONE_SIZE = 5;
    //private static final int FILTER_WINDOW_SIZE = 20;

    public static ArrayList<Fingerprint> fingerprint(short[] audio) {
        Complex[][] spectrum = fft(audio);
        ArrayList<int[]> peak = findPeak(spectrum);
        ArrayList<Fingerprint> fingerprint = hash(peak);
        return fingerprint;
    }

    public static Complex[][] fft(short[] audio) {

        final int totalSize = audio.length;

        int amountPossible = totalSize / CHUNK_SIZE;

        // When turning into frequency domain we'll need complex numbers:
        Complex[][] results = new Complex[amountPossible][];

        // For all the chunks:
        for (int times = 0; times < amountPossible; times++) {
            Complex[] complexTemp = new Complex[CHUNK_SIZE];
            for (int i = 0; i < CHUNK_SIZE; i++) {
                // Put the time domain data into a complex number with imaginary part as 0:
                complexTemp[i] = new Complex((double) audio[(times * CHUNK_SIZE) + i], 0);
            }
            Complex[] complex = hammingWindow(complexTemp);
            // Perform FFT analysis on the chunk:
            FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
            results[times] = fft.transform(complex, TransformType.FORWARD);
        }
        return results;

    }

    public static ArrayList<int[]> findPeak(Complex[][] spectrum) {
        double[][] peak = new double[spectrum.length][RANGE.length];
        double[][] highscores = new double[spectrum.length][RANGE.length];
        //double totalMag[] = new double[((peak.length - 1) / FILTER_WINDOW_SIZE) + 1], meanMag[] = new double[((peak.length - 1) / FILTER_WINDOW_SIZE) + 1];
        ArrayList<int[]> peakFiltered = new ArrayList<>();
        //For every line of data:
        for (int i = 0; i < spectrum.length; i++) {
            for (int freq = 1; freq <= CHUNK_SIZE / 2; freq++) {
                //Get the magnitude:
                double mag = spectrum[i][freq].abs();

                //Find out which range we are in:
                int index = 0;
                while (RANGE[index] < freq)
                    index++;

                //Save the highest magnitude and corresponding frequency:
                if (mag > highscores[i][index]) {
                    highscores[i][index] = mag;
                    peak[i][index] = freq;
                }
            }
        }

        //Filtering using sliding windows
        /*
        int index = 0, restCount = 0;
        while ((index + 1) * FILTER_WINDOW_SIZE <= peak.length) {
            for (int j = index * FILTER_WINDOW_SIZE; j < index * FILTER_WINDOW_SIZE + FILTER_WINDOW_SIZE; j++)
                for (int k = 0; k < peak[j].length; k++)
                    totalMag[index] += spectrum[j][(int) peak[j][k]].abs();
            index++;
        }
        for (int i = index * FILTER_WINDOW_SIZE; i < peak.length; i++)
            for (int j = 0; j < peak[i].length; j++) {
                totalMag[index] += spectrum[i][(int) peak[i][j]].abs();
                restCount++;
            }
        for (int i = 0; i < meanMag.length - 1; i++)
            meanMag[i] = totalMag[i] / (FILTER_WINDOW_SIZE * peak[0].length);
        meanMag[meanMag.length - 1] = totalMag[totalMag.length - 1] / restCount;
        for (int i = 0; i < peak.length; i++) {
            for (int j = 0; j < peak[i].length; j++) {
                if (spectrum[i][(int) peak[i][j]].abs() >= meanMag[(i - 1) / FILTER_WINDOW_SIZE] && peak[i][j] != 0) {
                    int[] temp = {i, (int) peak[i][j]};
                    peakFiltered.add(temp);
                }
            }
        }
        */

        //Filtering using mean of whole record

        double totalMag = 0, meanMag = 0;
        for (int i = 0; i < peak.length; i++)
            for (int j = 0; j < peak[i].length; j++)
                totalMag += spectrum[i][(int) peak[i][j]].abs();
        meanMag = totalMag / (peak.length * peak[0].length);
        for (int i = 0; i < peak.length; i++)
            for (int j = 0; j < peak[i].length; j++) {
                double amp = spectrum[i][(int) peak[i][j]].abs();
                if (amp >= 275000 && amp >= meanMag) {
                    int[] temp = {i, (int) peak[i][j]};
                    peakFiltered.add(temp);
                }
            }

        return peakFiltered;
    }

    public static Complex[] hammingWindow(Complex[] recordedData) {

        // iterate until the last line of the data buffer
        for (int n = 0; n < recordedData.length; n++) {
            // reduce unnecessarily performed frequency part of each and every frequency
            recordedData[n] = new Complex(recordedData[n].getReal() * (0.54 - 0.46 * Math.cos((2 * Math.PI * n)
                    / (recordedData.length - 1))), 0);
        }
        // return modified buffer to the FFT function
        return recordedData;
    }

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
