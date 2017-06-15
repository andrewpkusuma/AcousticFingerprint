package com.ureca.acousticfingerprint;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;

public class AudioAnalysis {

    private static final int CHUNK_SIZE = 4096;
    //Range : {320, 2560, 5120, 20000, 22050}
    private static final int[] RANGE = new int[]{30, 238, 476, 1858, 2048};
    private static final int FILTER_WINDOW_SIZE = 20;

    public static ArrayList<int[]> analyze(short[] audio) {
        Complex[][] spectrum = fft(audio);
        ArrayList<int[]> peak = findPeak(spectrum);
        return peak;
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
            Complex[] complex = hannWindow(complexTemp);
            // Perform FFT analysis on the chunk:
            FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.UNITARY);
            results[times] = fft.transform(complex, TransformType.FORWARD);
        }
        return results;

    }

    public static ArrayList<int[]> findPeak(Complex[][] spectrum) {
        double[][] peak = new double[spectrum.length][RANGE.length];
        double[][] highscores = new double[spectrum.length][RANGE.length];
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

        double totalMag[] = new double[((peak.length - 1) / FILTER_WINDOW_SIZE) + 1], meanMag[] = new double[((peak.length - 1) / FILTER_WINDOW_SIZE) + 1];
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
                double amp = spectrum[i][(int) peak[i][j]].abs();
                if (amp >= meanMag[(i - 1) / FILTER_WINDOW_SIZE] && amp >= CHUNK_SIZE * 0.5) {
                    int[] temp = {i, (int) peak[i][j], (int) amp};
                    peakFiltered.add(temp);
                }
            }
        }


        //Filtering using mean of whole record

        /*
        double totalMag = 0, meanMag = 0;
        for (int i = 0; i < peak.length; i++)
            for (int j = 0; j < peak[i].length; j++)
                totalMag += spectrum[i][(int) peak[i][j]].abs();
        meanMag = totalMag / (peak.length * peak[0].length);
        for (int i = 0; i < peak.length; i++)
            for (int j = 0; j < peak[i].length; j++) {
                double amp = spectrum[i][(int) peak[i][j]].abs();
                if (amp >= CHUNK_SIZE && amp >= meanMag) {
                    int[] temp = {i, (int) peak[i][j]};
                    peakFiltered.add(temp);
                }
            }
        */

        return peakFiltered;
    }

    public static Complex[] hannWindow(Complex[] recordedData) {

        // iterate until the last line of the data buffer
        for (int n = 0; n < recordedData.length; n++) {
            // reduce unnecessarily performed frequency part of each and every frequency
            recordedData[n] = new Complex(recordedData[n].getReal() * (0.5 - 0.5 * Math.cos((2 * Math.PI * n)
                    / (recordedData.length - 1))), 0);
        }
        // return modified buffer to the FFT function
        return recordedData;
    }

}
