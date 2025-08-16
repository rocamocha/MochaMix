package circuitlord.reactivemusic.audio;

import circuitlord.reactivemusic.api.RMPlayer;
import circuitlord.reactivemusic.api.RMPlayerOptions;
import circuitlord.reactivemusic.RMSongpackLoader;               // or RMSongpackLoader you use to resolve music/<name>.mp3
import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.MusicPackResource;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import rm_javazoom.jl.player.advanced.AdvancedPlayer;
import rm_javazoom.jl.player.AudioDevice;
import rm_javazoom.jl.player.JavaSoundAudioDevice;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RMPlayerImpl implements RMPlayer, Closeable {

    public static final String MOD_ID = "reactive_music";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ----- identity / grouping -----
    private final String id;
    private final String namespace;
    private volatile String group;

    // ----- options / state -----
    private final boolean linkToMcVolumes;
    private final boolean quietWhenPaused;
    private volatile boolean loop;
    private volatile boolean mute;

    private volatile float gainPercent;        // user layer (your old gainPercentage)
    private volatile float duckPercent;        // per-player duck
    private final Supplier<Float> groupDuckSupplier; // from manager: returns 1.0f unless group ducked

    // ----- source -----
    private volatile String songId;                          // resolved via songpack (e.g., "music/Foo")
    private volatile Supplier<InputStream> streamSupplier;   // optional direct supplier
    private volatile String fileId;
    private MusicPackResource currentResource;

    // ----- thread & playback -----
    private volatile boolean kill;           // thread exit
    private volatile boolean queued;         // new source queued
    private volatile boolean queuedToStop;   // stop request
    private volatile boolean paused;         // soft pause flag
    private volatile boolean playing;        // simplified “is playing”
    private volatile boolean complete;       // set by AdvancedPlayer when finished
    private volatile float realGainDb;       // last applied dB

    private AdvancedPlayer player;           // JavaZoom player
    private AudioDevice audio;      // audio device for gain control
    private Thread worker;                   // daemon worker thread

    // callbacks
    private final CopyOnWriteArrayList<Runnable> completeHandlers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Throwable>> errorHandlers = new CopyOnWriteArrayList<>();

    // constants (match your thread’s range)
    private static final float MIN_POSSIBLE_GAIN = -80f;
    private static final float MIN_GAIN = -50f;
    private static final float MAX_GAIN = 0f;

    // This is included just in case we need it down the road somewhere
    @SuppressWarnings("unused")
    private static String normalizeSongFileName(String logicalId) {
        if (logicalId == null || logicalId.isBlank()) return null;
        String name = logicalId.replace('\\','/');     // windows-safe
        if (!name.contains("/")) name = "music/" + name;
        if (!name.endsWith(".mp3")) name = name + ".mp3";
        return name;
    }

    public RMPlayerImpl(String id, RMPlayerOptions opts, Supplier<Float> groupDuckSupplier) {
        this.id = Objects.requireNonNull(id);
        this.namespace = opts.pluginNamespace() != null ? opts.pluginNamespace() : "core";
        this.group = opts.group() != null ? opts.group() : "music";
        this.linkToMcVolumes = opts.linkToMinecraftVolumes();
        this.quietWhenPaused = opts.quietWhenGamePaused();
        this.loop = opts.loop();
        this.gainPercent = opts.initialGainPercent();
        this.duckPercent = opts.initialDuckPercent();
        this.groupDuckSupplier = groupDuckSupplier != null ? groupDuckSupplier : () -> 1.0f;

        this.worker = new Thread(this::runLoop, "ReactiveMusic Player [" + id + "]");
        this.worker.setDaemon(true);
        this.worker.start();

        if (opts.autostart() && (songId != null || streamSupplier != null)) {
            queued = true;
        }
    }

    //package helper - links to RMPlayerManagerImpl
    String getNamespace() { 
        return this.namespace; 
    }

    /** Nudge the player to recompute its effective gain immediately. */
    void recomputeGainNow() { 
        requestGainRecompute(); 
    }

    // ===== RMPlayer =====

    @Override public String id() { return id; }

    @Override public boolean isPlaying() { return playing && !complete; }

    @Override public boolean isPaused() { return paused; }

    @Override public void setSong(String songId) {
        this.songId = songId;
        this.streamSupplier = null;
        queueStart();
    }

    @Override public void setStream(Supplier<InputStream> stream) {
        this.streamSupplier = stream;
        this.songId = null;
        queueStart();
    }

    @Override public void setFile(String filename) {
        this.fileId = fileId;
        this.streamSupplier = null;
        queueStart();
    }

    @Override public void play() {
        // restart from beginning of current source
        queueStart();
    }

    @Override public void stop() {
        ReactiveMusic.LOGGER.info("Stopping player...");
        if(player != null) {
            player.close();
            queuedToStop = true;
            complete = true;
            queued = false;
        }
		if (currentResource != null && currentResource.fileSystem != null) {
            try {
				currentResource.close();
                ReactiveMusic.LOGGER.info("Resource closed!");
            } catch (Exception e) {
                ReactiveMusic.LOGGER.error("Failed to close file system/input stream " + e.getMessage());
            }
        }
		currentResource = null;
    }

    @Override public boolean isIdle() {
        // Idle when we have no active/queued playback work
        return !playing && !queued;
    }

    @Override public void pause() { paused = true; }

    @Override public void resume() { paused = false; }

    @Override public void setGainPercent(float p) { gainPercent = clamp01(p); requestGainRecompute(); }

    @Override public void setDuckPercent(float p) { duckPercent = clamp01(p); requestGainRecompute(); }

    @Override public void setMute(boolean v) { mute = v; requestGainRecompute(); }

    @Override public float getRealGainDb() { return realGainDb; }

    @Override public void setGroup(String group) { this.group = group; requestGainRecompute(); }

    @Override public String getGroup() { return group; }

    @Override public void onComplete(Runnable r) { if (r != null) completeHandlers.add(r); }

    @Override public void onError(Consumer<Throwable> c) { if (c != null) errorHandlers.add(c); }

    @Override public void close() {
        stop();
        kill = true;
        if (worker != null) worker.interrupt();
        closeQuiet(player);
        player = null;
        audio = null;
    }

    // ===== internal =====

    private void queueStart() {
        this.queuedToStop = false;
        this.complete = false;
        this.queued = true;       // worker will open & play
        this.paused = false;
    }

    private void runLoop() {
        while (!kill) {
            try {
                if (queued) {
                    InputStream in = null;
                    try {
                        if (streamSupplier != null) {
                            in = streamSupplier.get();
                            currentResource = null; // external stream, nothing to close here
                        } else if (fileId != null) {
                            currentResource = openFromFile(fileId); // use a custom file found in the songpack
                        } else {
                            currentResource = openFromSongpack(songId);
                            if (currentResource == null || currentResource.inputStream == null) {
                                queued = false;
                                continue;
                            }
                            in = currentResource.inputStream; // like your original PlayerThread
                        }

                        audio = new JavaSoundAudioDevice();
                        player = new AdvancedPlayer(in, audio);
                        queued = false;
                        playing = true;
                        complete = false;

                        requestGainRecompute();

                        if (player.getAudioDevice() != null && !queuedToStop) {
                            player.play();
                        }
                    } finally {
                        // Cleanup player & audio
                        LOGGER.info("RM:[runLoop]: Closing player: " + this.namespace + ":" + this.group);
                        closeQuiet(player);
                        player = null;
                        audio = null;
                        playing = false;
                        complete = true;

                        // Close MusicPackResource like your old resetPlayer() did
                        if (currentResource != null) {
                            try { currentResource.close(); } catch (Exception ignored) {}
                            currentResource = null;
                        }
                    }

                    if (complete && !queuedToStop) {
                        completeHandlers.forEach(RMPlayerImpl::safeRun);
                        if (loop && !kill) queued = true;
                    }
                    queuedToStop = false;
                }

                Thread.sleep(5);
            } catch (Throwable t) {
                for (Consumer<Throwable> c : errorHandlers) safeRun(() -> c.accept(t));
                // reset on error
                closeQuiet(player);
                player = null;
                audio = null;
                playing = false;
                queuedToStop = false;
                queued = false;
                complete = true;
            }
        }
    }

    private static void closeQuiet(AdvancedPlayer p) {
        try { if (p != null) p.close(); } catch (Throwable ignored) {}
    }

    private static void safeRun(Runnable r) {
        try { r.run(); } catch (Throwable ignored) {}
    }

    private MusicPackResource openFromSongpack(String logicalId) {
        if (logicalId == null) return null;

        // Accept "Foo", "music/Foo", or full "music/Foo.mp3"
        String fileName;
        if (logicalId.endsWith(".mp3")) {
            fileName = logicalId;
        } else if (logicalId.startsWith("music/")) {
            fileName = logicalId + ".mp3";
        } else {
            fileName = "music/" + logicalId + ".mp3";
        }

        LOGGER.info("RM:[openFromSongpack]:" + fileName);

        return RMSongpackLoader.getInputStream(
            ReactiveMusic.currentSongpack.path,
            fileName,
            ReactiveMusic.currentSongpack.embedded
        ); // loader returns MusicPackResource{ inputStream, fileSystem? }.
    }

    private MusicPackResource openFromFile(String filename) {
        if (filename == null) return null;
        if (filename.endsWith(".mp3")) {
            // use this
        } else {
            filename = filename + ".mp3";
        }
        return RMSongpackLoader.getInputStream(
            ReactiveMusic.currentSongpack.path,
            filename,
            ReactiveMusic.currentSongpack.embedded
        );
    }

    /** Force a recompute of real dB gain using your existing math. */
    public void requestGainRecompute() {
        if (audio == null) return;
        float minecraftGain = 1.0f;

        if (linkToMcVolumes) {
            // MASTER * MUSIC from Options (same source you used previously)
            minecraftGain = getMasterMusicProduct();          // extract from GameOptions
            // your “less drastic” curve (same intent as your code)
            minecraftGain = (float)Math.pow(minecraftGain, 0.85);
        }

        float quietPct = 1f;
        if (quietWhenPaused && isInGamePausedAndNotOnSoundScreen()) {
            // you targeted ~70% with a gentle lerp; we keep the target value here
            quietPct = 0.7f; 
        }

        float effective = (mute ? 0f : gainPercent) * duckPercent * groupDuckSupplier.get() * quietPct * minecraftGain;
        float db = (minecraftGain == 0f || effective == 0f)
                ? MIN_POSSIBLE_GAIN
                : (MIN_GAIN + (MAX_GAIN - MIN_GAIN) * clamp01(effective));

        // LOGGER.info(String.format(
        //     "RM gain: mute=%s gain=%.2f duck=%.2f group=%.2f quiet=%.2f mc=%.2f -> dB=%.1f",
        //     mute, gainPercent, duckPercent, groupDuckSupplier.get(), 
        //     quietPct, minecraftGain, db
        // ));

        try {
            ((JavaSoundAudioDevice) audio).setGain(db);
            realGainDb = db;
        } catch (Throwable ignored) {}
    }

    private static float clamp01(float f) { return f < 0 ? 0 : Math.min(f, 1); }

    // ==== helpers copied from your thread’s logic ====

    private static boolean isInGamePausedAndNotOnSoundScreen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        Screen s = mc.currentScreen;
        if (s == null) return false;
        // You previously compared the translated title to "options.sounds.title" to avoid quieting on that screen
        Text t = s.getTitle();
        if (t == null) return true;
        String lower = t.getString().toLowerCase();
        // crude but effective: don’t “quiet” while on the sound options screen
        boolean onSoundScreen = lower.contains("sound"); // adapt if you kept the exact key match
        return !onSoundScreen;
    }

    private static float getMasterMusicProduct() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return 1f;
        // Replace with exact getters from 1.21.1 GameOptions
        float master = (float) mc.options.getSoundVolume(net.minecraft.sound.SoundCategory.MASTER);
        float music  = (float) mc.options.getSoundVolume(net.minecraft.sound.SoundCategory.MUSIC);
        return master * music;
    }
}