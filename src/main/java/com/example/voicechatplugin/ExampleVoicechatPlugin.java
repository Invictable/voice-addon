package com.example.voicechatplugin;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import org.bukkit.entity.Player;

import javax.sound.sampled.AudioFormat;
import java.util.Arrays;

public class ExampleVoicechatPlugin implements VoicechatPlugin {

    VoicechatApi api;
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
    }

    private void onMicrophone(MicrophonePacketEvent event)
    {
        // The connection might be null if the event is caused by other means
        if (event.getSenderConnection() == null)
        {
            return;
        }
        // Cast the generic player object of the voice chat API to an actual bukkit player
        // This object should always be a bukkit player object on bukkit based servers
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof Player player))
        {
            return;
        }

        MicrophonePacket packet = event.getPacket();
        player.sendMessage(Arrays.toString(packet.getOpusEncodedData()));
        OpusDecoder decoder = api.createDecoder();
        short[] PCMdata = decoder.decode(packet.getOpusEncodedData());
       // Mp3Encoder encoder = api.createMp3Encoder(),1,1,"output.mp3");


    }
}
