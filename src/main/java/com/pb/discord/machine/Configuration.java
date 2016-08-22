package com.pb.discord.machine;

import java.io.File;

public class Configuration {

    public enum Bot {
        MACHINE("MTcwMTk4NTE1NjU2NTU2NTQ0.Cmzx0A.EHRWesuaZ48NJ9G7VP_EZsR0FQI"),
        SAMARITAN("MTgzNzE1MDY2NzY2NTU3MTg0.Cpy71A.YHtmP2sVnhcUBq8RtOdljhiGdPY"),
        HAZECRAFT("MjA2ODUxMDk0MTYzMjI2NjI4.Cpy73g.gPSBqr6kCWUUcbFRUp_kyHL6uLQ");

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

    public static final Bot TARGET_BOT = Bot.HAZECRAFT;

    public static final String IBM_USERNAME = "a4ddbd69-8305-4faa-b6d3-9b90a4b65773";
    public static final String IBM_PASSWORD = "cYsfXOAB6yY6";

}
