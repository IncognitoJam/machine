package com.pb.discord.machine.voice;

import net.dv8tion.jda.audio.AudioConnection;
import net.dv8tion.jda.audio.AudioSendHandler;
import net.dv8tion.jda.utils.SimpleLog;
import org.tritonus.dsp.ais.AmplitudeAudioInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Queue;

public class QueuedFilePlayer implements AudioSendHandler {

    private Queue<PriorityFile> queuedAudioFiles;
    private boolean playing = false;
    private boolean paused = false;

    protected AudioInputStream audioSource = null;
    protected AudioFormat audioFormat = null;
    protected AmplitudeAudioInputStream amplitudeAudioStream = null;

    protected float amplitude = 1.0F;

    public void setAudioSource(AudioInputStream inSource) {
        if (inSource == null)
            throw new IllegalArgumentException("Cannot create an audio player from a null AudioInputStream!");

        if (audioSource != null) {
            try {
                audioSource.close();
            } catch (Exception ignored) {
            }
        }

        AudioFormat baseFormat = inSource.getFormat();

        // Converts first to PCM data. If the data is already PCM data, this will not change anything.
        AudioFormat toPCM = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),// AudioConnection.OPUS_SAMPLE_RATE,
                baseFormat.getSampleSizeInBits() != -1 ? baseFormat.getSampleSizeInBits() : 16,
                baseFormat.getChannels(),
                // If we are given a frame size, use it. Otherwise, assume 16 bits (2 8bit shorts) per channel.
                baseFormat.getFrameSize() != -1 ? baseFormat.getFrameSize() : 2 * baseFormat.getChannels(),
                baseFormat.getFrameRate() != -1 ? baseFormat.getFrameRate() : baseFormat.getSampleRate(),
                baseFormat.isBigEndian());
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(toPCM, inSource);

        // Then resamples to a sample rate of 48000hz and ensures that data is Big Endian.
        audioFormat = new AudioFormat(
                toPCM.getEncoding(),
                AudioConnection.OPUS_SAMPLE_RATE,
                toPCM.getSampleSizeInBits(),
                toPCM.getChannels(),
                toPCM.getFrameSize(),
                AudioConnection.OPUS_SAMPLE_RATE,
                true);

        // Used to control volume
        amplitudeAudioStream = new AmplitudeAudioInputStream(pcmStream);
        amplitudeAudioStream.setAmplitudeLinear(amplitude);

        audioSource = AudioSystem.getAudioInputStream(audioFormat, amplitudeAudioStream);
    }

    public void setVolume(float volume) {
        this.amplitude = volume;
        if (amplitudeAudioStream != null) {
            amplitudeAudioStream.setAmplitudeLinear(amplitude);
        }
    }

    @Override
    public boolean canProvide() {
        return !isPaused();
    }

    @Override
    public byte[] provide20MsAudio() {
        if (audioSource == null || audioFormat == null)
            throw new IllegalStateException("The Audio source was never set for this player!\n" +
                    "Please provide an AudioInputStream using setAudioSource.");
        try {
            int amountRead;
            byte[] audio = new byte[AudioConnection.OPUS_FRAME_SIZE * audioFormat.getFrameSize()];
            amountRead = audioSource.read(audio, 0, audio.length);
            if (amountRead > -1) {
                return audio;
            } else {
                pause();
                audioSource.close();
                return null;
            }
        } catch (IOException e) {
            SimpleLog.getLog("JDAPlayer").log(e);
        }
        return new byte[0];
    }

    /**
     * Creates a new instance of a {@link QueuedFilePlayer}.
     */
    public QueuedFilePlayer() {
        queuedAudioFiles = new PriorityQueue<>();
    }

    /**
     * Adds the given file to the player's audio source queue if and only if it exists.
     *
     * @param file An audio file to use as audio source.
     * @throws IOException                   If the file is not available.
     * @throws UnsupportedAudioFileException If the file is not supported by the player.
     */
    public void addAudioFile(PlayerEntry playerEntry) throws IOException, UnsupportedAudioFileException {

    }

    public void play() {
        playing = true;
        paused = false;
    }

    public void pause() {
        playing = false;
        paused = true;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isPaused() {
        return paused;
    }

    protected void reset() {
        queuedAudioFiles.clear();
        playing = false;
        paused = true;
    }


}
