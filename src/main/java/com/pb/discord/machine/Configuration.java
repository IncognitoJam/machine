package com.pb.discord.machine;

import java.io.File;

public class Configuration {

    public enum Bot {
        MACHINE(""),
        SAMARITAN(""),
        HAZECRAFT("");

        private String token;

        Bot(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public File getAvatar() {
            return FileUtils.getResourceAsFile(this.name().toLowerCase() + ".png");
        }
    }

    public static final Bot TARGET_BOT = Bot.MACHINE;

    public static final String IBM_USERNAME = "a4ddbd69-8305-4faa-b6d3-9b90a4b65773";
    public static final String IBM_PASSWORD = "cYsfXOAB6yY6";

}
