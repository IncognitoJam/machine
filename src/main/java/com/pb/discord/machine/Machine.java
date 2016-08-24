package com.pb.discord.machine;

import com.pb.discord.machine.voice.Synthesizer;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDA.Status;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.audio.AudioReceiveHandler;
import net.dv8tion.jda.audio.CombinedAudio;
import net.dv8tion.jda.audio.UserAudio;
import net.dv8tion.jda.audio.player.FilePlayer;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.StatusChangeEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.managers.AccountManager;
import net.dv8tion.jda.utils.AvatarUtil;
import net.dv8tion.jda.utils.SimpleLog;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class Machine extends ListenerAdapter implements AudioReceiveHandler {

    private static final SimpleLog LOG = SimpleLog.getLog("Machine");

    private HashMap<String, String> activeGuildVoiceChannel = new HashMap<>();
    private HashMap<String, FilePlayer> voiceChannelFilePlayers = new HashMap<>();

    private ConnectFourGame connectFourGame;

    private Synthesizer synthesizer;
    private DataLine.Info dataLineInfo;
    private SourceDataLine dataLine;

    public Machine(String[] args) {
        try {
            synthesizer = new Synthesizer();

            dataLineInfo = new DataLine.Info(SourceDataLine.class, new AudioFormat(48000, 16, 2, true, true));
            dataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            dataLine.open(new AudioFormat(48000, 16, 2, true, true), 4096);

            connectFourGame = new ConnectFourGame();
            new JDABuilder()
                    .setBotToken(Configuration.TARGET_BOT.getToken())
                    .addListener(this)
                    .addListener(connectFourGame)
                    .buildBlocking();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        LOG.info("System Ready!");
    }

    @Override
    public void onStatusChange(StatusChangeEvent event) {
        if (!event.getStatus().equals(Status.CONNECTED)) return;

        try {
            AccountManager accountManager = event.getJDA().getAccountManager();
            accountManager.setAvatar(AvatarUtil.getAvatar(Configuration.TARGET_BOT.getAvatar()));
            accountManager.update();
        } catch (UnsupportedEncodingException e) {
            LOG.warn("Error updating avatar.\n" + e.toString());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isPrivate()) {
            LOG.info(String.format("[PM] %s: %s", event.getAuthor().getUsername(), event.getMessage().getContent()));
            return;
        } else {
            LOG.info(String.format("[%s] [%s] %s: %s", event.getGuild().getName(), event.getTextChannel().getName(),
                    event.getAuthor().getUsername(), event.getMessage().getContent()));
        }

        if (event.getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) return;

        Guild guild = event.getGuild();
        User user = event.getAuthor();
        TextChannel textChannel = event.getTextChannel();
        String content = event.getMessage().getContent();

        if (content.equalsIgnoreCase("!join")) {
            String guildId = guild.getId();

            VoiceChannel targetVoiceChannel = findUserVoiceChannel(guild, user);
            if (activeGuildVoiceChannel.containsKey(guildId)) {
                String activeVoiceChannelId = activeGuildVoiceChannel.get(guildId);

                if (!activeVoiceChannelId.equals(targetVoiceChannel.getId())) {
                    guild.getAudioManager().moveAudioConnection(targetVoiceChannel);
                    activeGuildVoiceChannel.put(guildId, targetVoiceChannel.getId());
                } else {
                    textChannel.sendMessage(user.getAsMention() + ", it looks like I'm already in this voice channel!");
                    return;
                }
            } else {
                guild.getAudioManager().openAudioConnection(targetVoiceChannel);
                activeGuildVoiceChannel.put(guildId, targetVoiceChannel.getId());
            }
            textChannel.sendMessage("Sure, " + user.getAsMention() + "!");

        } else if (content.equalsIgnoreCase("!leave")) {
            String guildId = guild.getId();

            if (activeGuildVoiceChannel.containsKey(guildId)) {
                guild.getAudioManager().closeAudioConnection();
                activeGuildVoiceChannel.remove(guildId);
                textChannel.sendMessage("Bye, " + user.getAsMention() + ".");
            } else {
                textChannel.sendMessage("I'm not in a voice channel, " + user.getAsMention() + "!");
            }

        } else if (content.startsWith("!repeat")) {
            textChannel.sendMessage(content.replace("!repeat", "").trim());
        } else if (content.equalsIgnoreCase("!clearchat")) {

        } else if (content.startsWith("!tts")) {
            textChannel.sendMessage("Sorry, " + user.getAsMention() + ", but that command isn't implemented yet!");
        }
    }

    private void sendMessage(String message, Message originMessage) {
        sendMessage(message, originMessage, true);
    }

    private void sendMessage(String message, Message origin, boolean speech) {
        if (origin.isPrivate()) speech = false;
        JDA jda = origin.getJDA();
        MessageChannel messageChannel = origin.getChannel();
        User sourceUser = origin.getAuthor();

        if (speech) {
            TextChannel textChannel = (TextChannel) messageChannel;
            Guild guild = textChannel.getGuild();

            VoiceChannel voiceChannel = guild.getAudioManager().getConnectedChannel();
            if (voiceChannel == null) voiceChannel = findUserVoiceChannel(guild, sourceUser);
            if (voiceChannel == null) voiceChannel = findActiveVoiceChannel(guild);
            if (voiceChannel == null) speech = false;

            if (speech) {
                FilePlayer filePlayer;
                String voiceChannelId = voiceChannel.getId();
                if (voiceChannelFilePlayers.containsKey(voiceChannelId)) {
                    filePlayer = voiceChannelFilePlayers.get(voiceChannelId);
                } else {
                    filePlayer = new FilePlayer();
                    voiceChannelFilePlayers.put(voiceChannelId, filePlayer);
                }

//                if (filePlayer.isPlaying()) {
//                    filePlayer.
//                }
            }
        }
    }

//    private VoiceChannel calculatePreferredVoiceChannel(Message message) {
//        if (message.isPrivate()) return null;
//        JDA jda = message.getJDA();
//        TextChannel textChannel = (TextChannel) message.getChannel();
//        User sourceUser = message.getAuthor();
//        Guild guild = textChannel.getGuild();
//        String guildId = guild.getId();
//
//        VoiceChannel connectedChannel = guild.getAudioManager().getConnectedChannel();
//        if (activeGuildVoiceChannel.containsKey(guildId)) {
//            // Check if we should join an already active voice channel
//            VoiceChannel preferredVoiceChannel = jda.getVoiceChannelById(activeGuildVoiceChannel.get(guildId));
//            if (preferredVoiceChannel != null) {
//                if (connectedChannel != null) {
//                    if (preferredVoiceChannel != connectedChannel) {
//                        guild.getAudioManager().moveAudioConnection(preferredVoiceChannel);
//                    } else {
//                        // Already in preferred channel! :)
//                    }
//                } else {
//                    // Not connected to a voice channel, so join the preferred one
//                    guild.getAudioManager().openAudioConnection(preferredVoiceChannel);
//                }
//            } else {
//                // There was no preferred voice channel so try to find the user that we should
//                // speak to, and join their channel.
//                preferredVoiceChannel = findUserVoiceChannel(guild, sourceUser);
//                if (preferredVoiceChannel != null) {
//                    if (connectedChannel != null) {
//                        // Already in a voice channel, so move to the new channel!
//                        guild.getAudioManager().moveAudioConnection(preferredVoiceChannel);
//                    } else {
//                        guild.getAudioManager().openAudioConnection(preferredVoiceChannel);
//                    }
//                } else {
//                    if (connectedChannel != null) {
//
//                    }
//                }
//            }
//        } else {
//
//        }
//    }

    private VoiceChannel findUserVoiceChannel(Guild guild, User user) {
        for (VoiceChannel voiceChannel : guild.getVoiceChannels()) {
            if (voiceChannel.getUsers().contains(user))
                return voiceChannel;
        }
        return guild.getVoiceChannels().get(0);
    }

    private VoiceChannel findActiveVoiceChannel(Guild guild) {
        int max = 0;
        String maxVoice = guild.getVoiceChannels().get(0).getId();
        for (VoiceChannel voiceChannel : guild.getVoiceChannels()) {
            if (voiceChannel.getUsers().size() > max) {
                maxVoice = voiceChannel.getId();
                max = voiceChannel.getUsers().size();
            }
        }
        return guild.getJDA().getVoiceChannelById(maxVoice);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public boolean canReceiveCombined() {
        return false;
    }

    @Override
    public boolean canReceiveUser() {
        return true;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio ignore) {
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {
        byte[] audioData = userAudio.getAudioData(1D);
        try {
//            AudioData data = new AudioData(audioData);
//            AudioDataStream audioStream = new AudioDataStream(data);
//            AudioPlayer.player.start(audioStream);
            dataLine.start();
            dataLine.write(audioData, 0, audioData.length);
            dataLine.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleUserTalking(User user, boolean talking) {
//        if (talking) destination.getGuild().getTextChannels().get(0).sendMessage(user.getAsMention() + " is talking");
    }

    public static void main(String[] args) {
        new Machine(args);
    }

}
