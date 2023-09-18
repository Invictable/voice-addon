package com.example.voicechatplugin;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.io.IOException;

public final class ExamplePlugin extends JavaPlugin implements Listener
{

    public static final String PLUGIN_ID = "voice-record";
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);

    @Nullable
    private ExampleVoicechatPlugin voicechatPlugin;

    @Override
    public void onEnable() {
        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voicechatPlugin = new ExampleVoicechatPlugin();
            service.registerPlugin(voicechatPlugin);
            LOGGER.info("Successfully registered example plugin");
        } else {
            LOGGER.info("Failed to register example plugin");
        }
        new TestCommand(this);
        soundLoop();
    }
    @Override
    public void onDisable() {
        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
            LOGGER.info("Successfully unregistered example plugin");
        }
    }

    private void soundLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                voicechatPlugin.time = (voicechatPlugin.time + 1) % 600;
                if(voicechatPlugin.time == 0)
                {
                    for(Player p : Bukkit.getOnlinePlayers())
                    {
                        short[] audio = voicechatPlugin.playerSounds.get(p.getUniqueId());
                        voicechatPlugin.playerSounds.put(p.getUniqueId(), new short[960*600]);
                        try {
                            if(audio != null) {
                                voicechatPlugin.saveAudioFile(audio, p.getUniqueId());
                            }
                        } catch (IOException e) {

                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 1);
    }

}
