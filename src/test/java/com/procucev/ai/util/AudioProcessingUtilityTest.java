package com.procucev.ai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AudioProcessingUtility Tests")
class AudioProcessingUtilityTest {

    @Test
    @DisplayName("Should validate WAV header bytes accurately")
    void testIsWavHeader() {
        byte[] validWav = AudioProcessingUtility.createWavHeader(1024, 16000, 1);
        assertTrue(AudioProcessingUtility.isWavHeader(validWav));

        byte[] invalidHeader = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        assertFalse(AudioProcessingUtility.isWavHeader(invalidHeader));
        assertFalse(AudioProcessingUtility.isWavHeader(null));
        assertFalse(AudioProcessingUtility.isWavHeader(new byte[5]));
    }

    @Test
    @DisplayName("Should compute audio duration correctly from PCM bytes")
    void testCalculatePcmDurationMs() {
        // 16000 Hz, 1 channel, 16-bit (2 bytes per sample) = 32000 bytes per second
        byte[] oneSecondAudio = new byte[32000];
        long duration = AudioProcessingUtility.calculatePcmDurationMs(oneSecondAudio, 16000, 1);
        assertEquals(1000, duration);

        byte[] halfSecondAudio = new byte[16000];
        long halfSecDuration = AudioProcessingUtility.calculatePcmDurationMs(halfSecondAudio, 16000, 1);
        assertEquals(500, halfSecDuration);

        assertEquals(0, AudioProcessingUtility.calculatePcmDurationMs(null, 16000, 1));
        assertEquals(0, AudioProcessingUtility.calculatePcmDurationMs(oneSecondAudio, 0, 1));
    }

    @Test
    @DisplayName("Should detect silence in zeroed audio buffer")
    void testSilenceDetection() {
        byte[] silentBuffer = new byte[1600]; // 100ms of zeros
        assertTrue(AudioProcessingUtility.isSilence(silentBuffer, 0.01));
        assertEquals(0.0, AudioProcessingUtility.calculateRmsLevel(silentBuffer), 0.0001);
    }

    @Test
    @DisplayName("Should calculate non-zero RMS for active signal")
    void testActiveSignalRms() {
        // Create a sine wave PCM buffer
        int sampleCount = 800;
        byte[] pcmBytes = new byte[sampleCount * 2];
        ByteBuffer buf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < sampleCount; i++) {
            short sample = (short) (Math.sin(2 * Math.PI * i / 20.0) * 16384);
            buf.putShort(sample);
        }

        double rms = AudioProcessingUtility.calculateRmsLevel(pcmBytes);
        assertTrue(rms > 0.1, "RMS level should be significant for sine wave signal");
        assertFalse(AudioProcessingUtility.isSilence(pcmBytes, 0.01));
    }

    @Test
    @DisplayName("Should generate standard 44-byte WAV header with valid parameters")
    void testCreateWavHeader() {
        long payloadLength = 32000;
        int sampleRate = 16000;
        int channels = 1;

        byte[] header = AudioProcessingUtility.createWavHeader(payloadLength, sampleRate, channels);
        assertEquals(44, header.length);
        assertEquals('R', header[0]);
        assertEquals('I', header[1]);
        assertEquals('F', header[2]);
        assertEquals('F', header[3]);
        assertEquals('W', header[8]);
        assertEquals('A', header[9]);
        assertEquals('V', header[10]);
        assertEquals('E', header[11]);
    }
}
