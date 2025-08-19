package circuitlord.reactivemusic.api;

public interface RMPlayer extends AutoCloseable {
    String id();                         // unique handle, e.g. "myplugin:ambient-1"
    boolean isPlaying();
    boolean isPaused();
    boolean isIdle();
    void play();                         // (re)start from beginning
    void stop();                         // stop + release decoder
    void pause();                        // pause without releasing resources
    void resume();

    // Source
    void setSong(String songId);         // e.g. "music/ForestTheme" -> resolves to music/ForestTheme.mp3 in active songpack
    void setStream(java.util.function.Supplier<java.io.InputStream> streamSupplier); // custom source
    void setFile(String fileId);

    // Gain / routing
    void setGainPercent(float p);        // 0..1 user layer (your existing gainPercentage)
    void setDuckPercent(float p);        // 0..1 “duck” layer (like musicDiscDuckPercentage)
    void setMute(boolean v);
    float getRealGainDb();               // last applied dB to audio device

    // Grouping / coordination
    void setGroup(String group);         // e.g. "music", "ambient", "sfx"
    String getGroup();

    // Events
    void onComplete(Runnable r);         // fires when track completes
    void onError(java.util.function.Consumer<Throwable> c);

    @Override void close();              // same as stop(); also unregister
    
    void fade(float target, int tickDuration);
    float fadeTarget();
    int fadeDuration();
    float fadePercent();
    void setFadePercent(float p);
    boolean stopOnFadeOut();
    void stopOnFadeOut(boolean b);
    boolean resetOnFadeOut();
    void resetOnFadeOut(boolean b);
    void reset();
    boolean overlayFade();
    void overlayFade(boolean set);
    boolean fadingOut();
    void fadingOut(boolean b);
}
