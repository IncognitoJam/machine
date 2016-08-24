package com.pb.discord.machine.voice;

import java.io.File;
import java.net.URI;

public class PriorityFile extends File implements Comparable<File> {

    private int priority = 1;

    public PriorityFile(String pathname, int priority) {
        super(pathname);
        this.priority = priority;
    }

    public PriorityFile(String pathname) {
        super(pathname);
    }

    public PriorityFile(String parent, String child) {
        super(parent, child);
    }

    public PriorityFile(File parent, String child) {
        super(parent, child);
    }

    public PriorityFile(URI uri) {
        super(uri);
    }

    public PriorityFile(File file) {
        super(file.getAbsolutePath());
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(File other) {
        int otherPriority = other instanceof PriorityFile ? ((PriorityFile) other).getPriority() : 1;
        return priority > otherPriority ? 1 : (priority < otherPriority ? -1 : 0);
    }

}
