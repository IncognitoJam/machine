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

    public static final String IBM_USERNAME = "";
    public static final String IBM_PASSWORD = "";

}
