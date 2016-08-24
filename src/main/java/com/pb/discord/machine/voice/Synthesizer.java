package com.pb.discord.machine.voice;

import com.gtranslate.Audio;
import com.gtranslate.context.TranslateEnvironment;
import com.pb.discord.machine.FileUtils;

import java.io.File;

public class Synthesizer {

    private Audio audio;

    public Synthesizer() {
        TranslateEnvironment.init();
        audio = Audio.getInstance();
    }

    public File synthesize(String text) throws Exception {
        return FileUtils.audioToWav(audio.getAudio(text, "en"));
    }

}
