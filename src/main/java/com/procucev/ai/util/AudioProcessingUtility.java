package com.procucev.ai.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility for processing and inspecting raw PCM and WAV audio buffers
 * for real-time speech recognition pipelines.
 */
public final class AudioProcessingUtility {

    private AudioProcessingUtility() {
        // Utility class, prevent instantiation
    }

    /**
     * Standard audio configuration constants.
     */
    public static final int DEFAULT_SAMPLE_RATE = 16000;
    public static final int DEFAULT_CHANNELS = 1;
    public static final int BYTES_PER_SAMPLE_16BIT = 2;

    /**
     * Checks if a byte buffer contains a standard RIFF/WAVE header.
     *
     * @param audioData byte array of audio file
     * @return true if header matches 'RIFF' and 'WAVE'
     */
    public static boolean isWavHeader(byte[] audioData) {
        if (audioData == null || audioData.length < 12) {
            return false;
        }
        return audioData[0] == 'R' && audioData[1] == 'I' && audioData[2] == 'F' && audioData[3] == 'F' &&
               audioData[8] == 'W' && audioData[9] == 'A' && audioData[10] == 'V' && audioData[11] == 'E';
    }

    /**
     * Estimates audio duration in milliseconds from PCM 16-bit mono audio byte array.
     *
     * @param pcmBytes raw PCM byte array
     * @param sampleRate sample rate in Hz (e.g. 16000)
     * @param channels number of audio channels (e.g. 1 for mono)
     * @return duration in milliseconds
     */
    public static long calculatePcmDurationMs(byte[] pcmBytes, int sampleRate, int channels) {
        if (pcmBytes == null || sampleRate <= 0 || channels <= 0) {
            return 0;
        }
        long bytesPerSecond = (long) sampleRate * channels * BYTES_PER_SAMPLE_16BIT;
        return (pcmBytes.length * 1000L) / bytesPerSecond;
    }

    /**
     * Calculates the Root Mean Square (RMS) volume level of 16-bit Little-Endian PCM audio.
     * Returns a normalized value between 0.0 (total silence) and 1.0 (peak level).
     *
     * @param pcm16Bytes raw 16-bit little endian PCM data
     * @return normalized RMS amplitude (0.0 to 1.0)
     */
    public static double calculateRmsLevel(byte[] pcm16Bytes) {
        if (pcm16Bytes == null || pcm16Bytes.length < 2) {
            return 0.0;
        }

        ByteBuffer buffer = ByteBuffer.wrap(pcm16Bytes).order(ByteOrder.LITTLE_ENDIAN);
        double sumSquares = 0.0;
        int sampleCount = pcm16Bytes.length / 2;

        for (int i = 0; i < sampleCount; i++) {
            short sample = buffer.getShort();
            double normalized = sample / 32768.0;
            sumSquares += normalized * normalized;
        }

        return Math.sqrt(sumSquares / sampleCount);
    }

    /**
     * Determines whether an audio buffer is silence below a specified RMS threshold.
     *
     * @param pcm16Bytes raw 16-bit little endian PCM data
     * @param silenceThreshold normalized threshold (typically 0.01 - 0.03)
     * @return true if RMS level is below threshold
     */
    public static boolean isSilence(byte[] pcm16Bytes, double silenceThreshold) {
        return calculateRmsLevel(pcm16Bytes) < silenceThreshold;
    }

    /**
     * Generates a standard 44-byte WAV header for linear 16-bit PCM audio.
     *
     * @param totalAudioLen total raw audio payload length in bytes
     * @param sampleRate sample rate (e.g. 16000)
     * @param channels channels (e.g. 1 for mono, 2 for stereo)
     * @return 44-byte WAV header
     */
    public static byte[] createWavHeader(long totalAudioLen, int sampleRate, int channels) {
        long totalDataLen = totalAudioLen + 36;
        long byteRate = (long) sampleRate * channels * 2;

        byte[] header = new byte[44];
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 16 for PCM format chunk size
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1 (PCM)
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2);  // block align
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return header;
    }
}
