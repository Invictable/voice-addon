package com.example.voicechatplugin;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.packets.SoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestCommand implements CommandExecutor {
    private ExamplePlugin plugin;

    public TestCommand(ExamplePlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("test").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may execute this command");
            return true;
        }
        Player p = (Player) sender;
        VoicechatServerApi vcServer = ExampleVoicechatPlugin.vcServer;
        StaticSoundPacket.Builder pBuilder = ExampleVoicechatPlugin.pBuilder;
        String fileName = "0ad3467b-1c3e-4032-9781-b0583b3c145b.opus";
        File file = new File(fileName);
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            int[] i = {0};
            new BukkitRunnable() {
                @Override
                public void run() {
                    int len = fileContent[i[0]];
                    len += 128;
                    byte[] packet = new byte[len];
                    for(int j = 0; j < len; j++)
                    {
                        packet[j] = fileContent[i[0]+1+j];
                    }
                    VoicechatConnection connection = vcServer.getConnectionOf(p.getUniqueId());
                    // Check if the player is actually connected to the voice chat
                    if (connection != null) {
                        // Send a static audio packet of the microphone data to the connection of each player
                        pBuilder.opusEncodedData(packet);
                        vcServer.sendStaticSoundPacketTo(connection, pBuilder.build().toStaticSoundPacket());
                        //p.sendMessage(String.valueOf(pBuilder.build().toStaticSoundPacket().getOpusEncodedData()[0]));
                    }
                    i[0] += 256;
                    if(i[0] >= 600*256) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);

        } catch (IOException e) {

        }


        return false;
    }
}