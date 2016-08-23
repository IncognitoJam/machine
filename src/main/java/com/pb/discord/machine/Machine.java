package com.pb.discord.machine;

import com.ibm.watson.developer_cloud.http.ServiceCallback;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDA.Status;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.audio.AudioReceiveHandler;
import net.dv8tion.jda.audio.CombinedAudio;
import net.dv8tion.jda.audio.UserAudio;
import net.dv8tion.jda.audio.player.FilePlayer;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class Machine extends ListenerAdapter implements AudioReceiveHandler, ServiceCallback<InputStream>
//        ,AudioSendHandler
{

    private static final SimpleLog LOG = SimpleLog.getLog("Machine");

    private HashMap<String, String> activeGuildVoiceChannel = new HashMap<>();
    private HashMap<String, FilePlayer> voiceChannelFilePlayers = new HashMap<>();

    //    private VoiceChannel source, destination;
    private DataLine.Info dataLineInfo;
    private SourceDataLine dataLine;

    public Machine(String[] args) {
        try {
//            textToSpeech = new TextToSpeech(Configuration.IBM_USERNAME, Configuration.IBM_PASSWORD);

            dataLineInfo = new DataLine.Info(SourceDataLine.class, new AudioFormat(48000, 16, 2, true, true));
            dataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            dataLine.open(new AudioFormat(48000, 16, 2, true, true), 4096);

            new JDABuilder()
                    .setBotToken(Configuration.TARGET_BOT.getToken())
                    .addListener(this)
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

        Guild guild = event.getGuild();
        User user = event.getAuthor();
        TextChannel textChannel = event.getTextChannel();
        String content = event.getMessage().getContent();

//        if (content.equalsIgnoreCase("!source")) {
//            source = findUserVoiceChannel(guild, user);
//            if (source == null) source = findActiveVoiceChannel(guild);
//            guild.getAudioManager().setReceivingHandler(this);
//            guild.getAudioManager().openAudioConnection(source);
//        } else if (content.equalsIgnoreCase("!link")) {
//            source = event.getJDA().getVoiceChannelById("216280735961579523");
//
//            Guild sourceGuild = event.getJDA().getGuildById("216280735961579522");
//            sourceGuild.getAudioManager().setReceivingHandler(this);
//            sourceGuild.getAudioManager().openAudioConnection(source);
//
//            destination = findUserVoiceChannel(guild, user);
//            if (destination == null) destination = findActiveVoiceChannel(guild);
//            guild.getAudioManager().openAudioConnection(destination);
//        } else if (content.startsWith("!message")) {
//            source.getGuild().getTextChannels().get(0).sendMessage(content.replace("!message ", ""));
//        }

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

        } else if (content.equalsIgnoreCase("!clearchat")) {

        } else if (content.equalsIgnoreCase("!tts")) {
            textChannel.sendMessage("Sorry, " + user.getAsMention() + ", but that command isn't implemented yet!");
        }
    }

    private void sendMessage(String message, TextChannel textChannel) {
        sendMessage(message, textChannel, true);
    }

    private void sendMessage(String message, TextChannel textChannel, boolean speech) {
        JDA jda = textChannel.getJDA();
        Guild guild = textChannel.getGuild();
        String guildId = guild.getId();

        if (activeGuildVoiceChannel.containsKey(guildId)) {
            VoiceChannel voiceChannel = jda.getVoiceChannelById(activeGuildVoiceChannel.get(guildId));
            if (voiceChannel != null) {
                VoiceChannel connectedChannel = guild.getAudioManager().getConnectedChannel();
                if (connectedChannel != null) {
                    if (voiceChannel != connectedChannel) {
                        guild.getAudioManager().moveAudioConnection(voiceChannel);
                    }
                } else {
                    guild.getAudioManager()
                }
            }
        }
    }

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
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        // Ignore
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

    @Override
    public void onResponse(InputStream response) {
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(Exception e) {
        e.printStackTrace();
    }

//    @Override
//    public boolean canProvide() {
//        return buffer.size() >= AudioConnection.OPUS_FRAME_SIZE * PCM_FRAME_SIZE;
//    }
//
//    @Override
//    public byte[] provide20MsAudio() {
//        System.out.println("provide20MsAudio");
//        System.out.println("provide20MsAudio");
//        System.out.println("provide20MsAudio");
//
//        byte[] data = new byte[AudioConnection.OPUS_FRAME_SIZE * PCM_FRAME_SIZE];
//
//        for (int i = 0; i < AudioConnection.OPUS_FRAME_SIZE * PCM_FRAME_SIZE; i++) {
//            data[i] = buffer.poll();
//        }
//
//        return data;
//    }

}
