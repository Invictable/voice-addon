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
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class ExampleVoicechatPlugin implements VoicechatPlugin {
    int time = 0;
    HashMap<UUID, short[]> playerSounds = new HashMap<>();
    HashMap<UUID, Integer> lastSend = new HashMap<>();
    static VoicechatServerApi vcServer;
    static StaticSoundPacket.Builder pBuilder;
    static VoicechatApi api;
    OpusDecoder decoder;
    private static final short MAX_AMPLIFICATION = Short.MAX_VALUE - 1;
    private final float[] maxVolumes = new float[50];
    private int index;
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
        decoder = api.createDecoder();
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

        byte[] PCMdata = packet.getOpusEncodedData();
        short[] audio = decoder.decode(PCMdata);

        if(playerSounds.containsKey(player.getUniqueId())) {
            short[] audio2 = playerSounds.get(player.getUniqueId());
            int send = lastSend.get(player.getUniqueId());
            int offset = 960*send;

            lastSend.put(player.getUniqueId(), send+1);

            for(int i = 0; i < audio.length; i++)
            {
                audio2[offset+i] = audio[i];
            }
        }

    }

    /**
     * Changes the volume of 16 bit audio
     * Note that this modifies the input array
     *
     * @param audio  the audio data
     * @param volume the amplification
     * @return the adjusted audio
     */
    public short[] adjustVolumeMono(short[] audio, float volume) {
        maxVolumes[index] = getMaximumMultiplier(audio, volume);
        index = (index + 1) % maxVolumes.length;
        float min = -1F;
        for (float mul : maxVolumes) {
            if (mul < 0F) {
                continue;
            }
            if (min < 0F) {
                min = mul;
                continue;
            }
            if (mul < min) {
                min = mul;
            }
        }

        float maxVolume = Math.min(min, volume);

        for (int i = 0; i < audio.length; i++) {
            audio[i] = (short) ((float) audio[i] * maxVolume);
        }
        return audio;
    }

    private static float getMaximumMultiplier(short[] audio, float multiplier) {
        short max = 0;

        for (short value : audio) {
            short abs;
            if (value <= Short.MIN_VALUE) {
                abs = (short) Math.abs(value + 1);
            } else {
                abs = (short) Math.abs(value);
            }
            if (abs > max) {
                max = abs;
            }
        }

        return Math.min(multiplier, (float) MAX_AMPLIFICATION / (float) max);
    }

    public void rawToWave(short[] audio, final File waveFile) throws IOException {
        int j = audio.length-1;
        while(j >= 0 && audio[j] == 0)
        {
            j--;
        }
        j += 1;

        short[] temp = new short[j];
        for(int i = 0; i < temp.length; i++)
        {
            temp[i] = audio[i];
        }
        audio = temp;

        byte[] rawData = new byte[(j+1)*2];
        final int RECORDER_SAMPLERATE = 1800;
        for (int i = 0; i < audio.length; i++) {
            rawData[i * 2] = (byte) (audio[i] & 0x00FF);
            rawData[(i * 2) + 1] = (byte) (audio[i] >> 8);
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + audio.length*2); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, 44100); // sample rate
            writeInt(output, RECORDER_SAMPLERATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, audio.length*2); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            ByteBuffer bytes = ByteBuffer.allocate(audio.length * 2);
            for (short s : audio) {
                bytes.putShort(s);
            }

            output.write(rawData);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
}
