package com.example.voicechatplugin;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class ExampleVoicechatPlugin implements VoicechatPlugin {
    int time = 0;
    HashMap<UUID, short[]> playerSounds = new HashMap<>();
    static VoicechatServerApi vcServer;
    static StaticSoundPacket.Builder pBuilder;
    static VoicechatApi api;
    /**
     * @return the unique ID for this voice chat plugin
     */
    @Override
    public String getPluginId() {
        return ExamplePlugin.PLUGIN_ID;
    }

    /**
     * Called when the voice chat initializes the plugin.
     *
     * @param api_ the voice chat API
     */
    @Override
    public void initialize(VoicechatApi api_) {
        api = api_;
    }

    /**
     * Called once by the voice chat to register all events.
     *
     * @param registration the event registration
     */
    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStart);
    }

    private void onServerStart(VoicechatServerStartedEvent event)
    {
        vcServer = event.getVoicechat();
    }

    private void onMicrophone(MicrophonePacketEvent event)
    {
        pBuilder = event.getPacket().staticSoundPacketBuilder();
        // The connection might be null if the event is caused by other means
        if (event.getSenderConnection() == null)
        {
            return;
        }

        // Cast the generic player object of the voice chat API to an actual bukkit player
        // This object should always be a bukkit player object on bukkit based servers
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof Player))
        {
            return;
        }
        Player player = (Player) event.getSenderConnection().getPlayer().getPlayer();

        MicrophonePacket packet = event.getPacket();

        OpusDecoder decoder = api.createDecoder();
        byte[] PCMdata = packet.getOpusEncodedData();
        short[] audio = decoder.decode(PCMdata);

        if(playerSounds.containsKey(player.getUniqueId())) {
            short[] audio2 = playerSounds.get(player.getUniqueId());
            int offset = 960*time;
            for(int i = 0; i < audio.length; i++)
            {
                audio2[offset+i] = audio[i];
            }
        }

    }

    public void saveAudioFile(short[] audio, UUID id) throws IOException {
        File file = new File(id.toString() + ".opus");
        OutputStream out = new FileOutputStream(file);
        byte[] bOut = new byte[audio.length*2];
        for(int i = 0; i < audio.length; i++)
        {
            bOut[i*2] = (byte)((audio[i] & 0xff00) >> 8);
            bOut[i*2+1] = (byte)(audio[i] & 0xff);
        }
        out.write(bOut);
        out.close();
    }
}
