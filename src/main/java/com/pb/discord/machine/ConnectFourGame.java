package com.pb.discord.machine;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

import java.util.ArrayList;

public class ConnectFourGame extends ListenerAdapter {

    public enum Counter {
        EMPTY("E", ":white_circle:"),
        RED("R", ":red_circle:", State.RED),
        BLUE("B", ":large_blue_circle:", State.BLUE);

        String string;
        String emote;
        State state;

        Counter(String string, String emote, State state) {
            this.string = string;
            this.emote = emote;
            this.state = state;
        }

        Counter(String string, String emote) {
            this(string, emote, null);
        }

        @Override
        public String toString() {
            return string;
        }
    }

    public enum State {
        IDLE, RED, BLUE
    }

    private ArrayList<User> redTeam = new ArrayList<>();
    private ArrayList<User> blueTeam = new ArrayList<>();
    private Counter[][] board = new Counter[7][7];
    private State state = State.IDLE;

    public ConnectFourGame() {
        clearBoard();
    }

    void clearBoard() {
        board = new Counter[7][7];
        state = State.IDLE;
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                board[y][x] = Counter.EMPTY;
            }
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        TextChannel channel = event.getChannel();
        Message message = event.getMessage();
        User author = event.getAuthor();
        String content = message.getContent();

        // Not for this bot!
        if (!content.startsWith("!c4") || author.getId().equals(jda.getSelfInfo().getId())) return;

        if (content.length() == 3) {
            sendHelp(channel);
        } else {
            String[] cmd = content.replace("!c4 ", "").split(" ");
            switch (cmd[0]) {
                case "help":
                    sendHelp(channel);
                    break;
                case "team":
                    if (cmd.length >= 2) {
                        if (isOngoing()) {
                            channel.sendMessage("**You cannot change team whilst a game is ongoing!**");
                            return;
                        }

                        String team = cmd[1];
                        switch (team.toLowerCase()) {
                            case "red":
                                if (redTeam.contains(author)) channel.sendMessage("**You are already on red team!**");
                                else {
                                    if (blueTeam.contains(author)) blueTeam.remove(author);
                                    redTeam.add(author);
                                    channel.sendMessage("You have joined *red* team!");
                                }
                                break;
                            case "blue":
                                if (blueTeam.contains(author)) channel.sendMessage("**You are already on blue team!**");
                                else {
                                    if (redTeam.contains(author)) redTeam.remove(author);
                                    blueTeam.add(author);
                                    channel.sendMessage("You have joined *blue* team!");
                                }
                                break;
                            default:
                                channel.sendMessage("**You must specify a valid team!**");
                        }
                    } else {
                        channel.sendMessage("**You must specify a team to join!**");
                    }
                    break;
                case "play":
                    if (!isOngoing() && !(redTeam.size() > 0 && blueTeam.size() > 0)) {
                        channel.sendMessage("**There must be at least one player on each team!**");
                        return;
                    } else if (!redTeam.contains(author) && !blueTeam.contains(author)) {
                        channel.sendMessage("**You have not joined a team!**");
                        return;
                    }

                    Counter counter = getCounter(author);
                    if (state != counter.state && state != State.IDLE) {
                        channel.sendMessage("**It is not your turn!**");
                        return;
                    }

                    if (cmd.length >= 2) {
                        int column;
                        try {
                            column = Integer.valueOf(cmd[1]);
                        } catch (Exception ignored) {
                            channel.sendMessage("**Invalid input!** You must enter a number for the column!");
                            return;
                        }

                        if (column > 0 && column < 8) {
                            if (dropCounter(counter, column)) {
                                channel.sendMessage(counter.emote + " dropped in column " + column);
                                renderBoard(channel);
                                channel.sendMessage("**It is the turn of** " + opposite(counter).emote);
                                state = opposite(counter).state;
                            } else {
                                channel.sendMessage("**Column " + column + " is full!** Please try again.");
                            }
                        } else {
                            channel.sendMessage("**You must provide a column number between 1 and 7**");
                        }
                    }

                    break;
                case "end":
                    clearBoard();
                    channel.sendMessage("Board cleared. State reset." +
                            "\nYou can now change or join a team, or start the game by making a move.");
                    renderBoard(channel);
                    break;
                default:
                    channel.sendMessage("`" + cmd[0] + "` **is not a valid command!**");
                    sendHelp(channel);
            }
        }
    }

    Counter opposite(Counter counter) {
        return counter == Counter.BLUE ? Counter.RED : Counter.BLUE;
    }

    boolean isOngoing() {
        return state == State.BLUE || state == State.RED;
    }

    void sendHelp(TextChannel channel) {
        channel.sendMessage("**Help:**" +
                "\nThe game begins once there is at least one player on each team and either player makes a move." +
                "A player from either team can make the first move. You cannot change teams once the game has begun." +
                "\n    !c4 help" +
                "\n    !c4 team red|blue" +
                "\n    !c4 play [column no.]" +
                "\n    !c4 end");
    }

    boolean dropCounter(Counter counter, int columnNo) {
        columnNo -= 1;

        Counter[] column = board[columnNo];
        if (column[0] != Counter.EMPTY) return false;
        int y = 0;
        while (y < 6 && column[y + 1] == Counter.EMPTY) {
            y += 1;
        }
        column[y] = counter;
        return true;
    }

    Counter getCounter(User user) {
        return redTeam.contains(user) ? Counter.RED : Counter.BLUE;
    }

    void renderBoard(TextChannel channel) {
        String message = ".\n";
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                Counter counter = board[x][y];
                message += counter.emote;
            }
            message += "\n";
        }
        channel.sendMessage(message);
    }

}
