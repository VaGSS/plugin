package com.vagsinhooo.whitelist;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WhitelistFromDiscord extends JavaPlugin {

    private Set<String> allowedChannels = new HashSet<>();
    private Set<String> requiredRoles = new HashSet<>();
    private String cmdAdd;
    private String cmdRemove;
    private String cmdCheck;
    private boolean replyInDiscord;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        // Subscribe to DiscordSRV event bus
        DiscordSRV.api.subscribe(this);
        getLogger().info("DiscordWhitelistPlugin enabled. Listening for Discord commands.");
    }

    @Override
    public void onDisable() {
        try {
            DiscordSRV.api.unsubscribe(this);
        } catch (Throwable ignored) {}
    }

    private void loadConfigValues() {
        allowedChannels = new HashSet<>(getConfig().getStringList("allowedChannelIds"));
        requiredRoles = new HashSet<>(getConfig().getStringList("requiredRoleIds"));
        cmdAdd = getConfig().getString("commandPrefixAdd", "!wl");
        cmdRemove = getConfig().getString("commandPrefixRemove", "!uwl");
        cmdCheck = getConfig().getString("commandPrefixCheck", "!wlcheck");
        replyInDiscord = getConfig().getBoolean("replyInDiscord", true);
    }

    @Subscribe
    public void onDiscordMessage(DiscordGuildMessageReceivedEvent event) {
        Message message = event.getMessage();
        if (message.getAuthor().isBot()) return;

        // Only guild text channels are supported for whitelist commands
        if (!(message.getChannel() instanceof GuildMessageChannel)) return;

        GuildMessageChannel channel = (GuildMessageChannel) message.getChannel();

        // Channel allowlist check
        if (!allowedChannels.isEmpty() && !allowedChannels.contains(channel.getId())) {
            return;
        }

        String content = message.getContentRaw().trim();
        if (!(content.startsWith(cmdAdd + " ") || content.startsWith(cmdRemove + " ") || content.startsWith(cmdCheck + " "))) {
            return;
        }

        // Role requirement check
        if (!requiredRoles.isEmpty()) {
            Member member = message.getMember();
            if (member == null) return;
            Set<String> memberRoleIds = member.getRoles().stream().map(r -> r.getId()).collect(Collectors.toSet());
            boolean hasAnyRequired = requiredRoles.stream().anyMatch(id -> memberRoleIds.contains(id));
            if (!hasAnyRequired) {
                send(channel, "⛔ Nie masz uprawnień do używania tej komendy na tym kanale.");
                return;
            }
        }

        // Parse command
        boolean add = content.startsWith(cmdAdd + " ");
        boolean remove = content.startsWith(cmdRemove + " ");
        boolean check = content.startsWith(cmdCheck + " ");
        String[] parts = content.split("\s+");
        if (parts.length < 2) {
            send(channel, "Użycie: " + cmdAdd + " <nick> | " + cmdRemove + " <nick> | " + cmdCheck + " <nick>");
            return;
        }
        String nickname = parts[1];

        if (add) {
            Bukkit.getScheduler().runTask(this, () -> {
                // Use Bukkit API to whitelist
                OfflinePlayer player = Bukkit.getOfflinePlayer(nickname);
                player.setWhitelisted(true);
                if (replyInDiscord) send(channel, "✅ Dodano **" + nickname + "** do whitelisty.");
                getLogger().info("Dodano do whitelisty: " + nickname + " (z Discorda).");
            });
        } else if (remove) {
            Bukkit.getScheduler().runTask(this, () -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(nickname);
                player.setWhitelisted(false);
                if (replyInDiscord) send(channel, "🗑️ Usunięto **" + nickname + "** z whitelisty.");
                getLogger().info("Usunięto z whitelisty: " + nickname + " (z Discorda).");
            });
        } else if (check) {
            Bukkit.getScheduler().runTask(this, () -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(nickname);
                boolean whitelisted = player.isWhitelisted();
                if (replyInDiscord) send(channel, (whitelisted ? "🟢 " : "🔴 ") + "**" + nickname + "** " + (whitelisted ? "jest" : "nie jest") + " na whiteliście.");
            });
        }
    }

    private void send(MessageChannel channel, String msg) {
        try {
            channel.sendMessage(msg).queue();
        } catch (Throwable t) {
            getLogger().warning("Nie udało się wysłać wiadomości na Discord: " + t.getMessage());
        }
    }
}
