package com.pb.discord.machine.voice;

import java.io.File;

public class PlayerEntry {

    private PriorityFile file;
//    private long

    private PlayerEntry(PriorityFile file) {
        this.file = file;
//        this.user = user;
    }

    public static PlayerEntry createPlayerEntry(File file) {
        if (file == null)
            throw new IllegalArgumentException("A null File was provided to the QueuedFilePlayer! Cannot play a null file!");
        if (!file.exists())
            throw new IllegalArgumentException("A non-existent file was provided to the QueuedFilePlayer! Cannot play a file that doesn't exist!");

        PriorityFile priorityFile;
        if (file instanceof PriorityFile) {
            priorityFile = (PriorityFile) file;
        } else {
            priorityFile = new PriorityFile(file);
        }

        return null;
    }

}
