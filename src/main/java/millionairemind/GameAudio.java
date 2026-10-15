package millionairemind;

import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/** All mixer operations run on one worker, never while holding a Swing-facing lock. */
final class GameAudio implements AutoCloseable {
    enum Channel { MUSIC, SPLASH, RESULTS, EFFECT }

    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "game-audio");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Channel, Clip> clips = new EnumMap<>(Channel.class);
    private final Map<Channel, String> resources = new EnumMap<>(Channel.class);
    private final Map<Channel, Integer> versions = new EnumMap<>(Channel.class);
    private boolean closed;

    synchronized void play(Channel channel, String resource, float volume, boolean loop) {
        if (closed || resource.equals(resources.get(channel))) return;
        int version = versions.merge(channel, 1, Integer::sum);
        resources.put(channel, resource);
        worker.execute(() -> load(channel, resource, volume, loop, version));
    }

    private synchronized boolean current(Channel channel, int version) {
        return !closed && versions.get(channel) == version;
    }

    private void load(Channel channel, String resource, float volume, boolean loop, int version) {
        if (!current(channel, version)) return;
        stopClip(clips.remove(channel));
        Clip clip = null;
        try {
            URL url = GameAudio.class.getResource(resource);
            if (url == null) throw new IOException("Missing bundled sound: " + resource);
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(url)) {
                AudioFormat original = stream.getFormat();
                AudioFormat playback = new AudioFormat(original.getSampleRate(), 16,
                        original.getChannels(), true, false);
                // Common mixers cannot play the bundled 24-bit recordings directly.
                try (AudioInputStream converted = AudioSystem.getAudioInputStream(playback, stream)) {
                    clip = AudioSystem.getClip();
                    clip.open(converted);
                }
            }
            if (!current(channel, version)) {
                stopClip(clip);
                return;
            }
            Clip loaded = clip;
            if (!loop) {
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) finish(channel, loaded, version);
                });
            }
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float decibels = (float) (20 * Math.log10(volume));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
            }
            clips.put(channel, clip);
            if (loop) clip.loop(Clip.LOOP_CONTINUOUSLY);
            else clip.start();
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException
                | IllegalArgumentException | SecurityException e) {
            stopClip(clip);
            synchronized (this) {
                if (current(channel, version)) {
                    resources.remove(channel);
                    System.err.println("Audio unavailable for " + resource + ": " + e.getMessage());
                }
            }
        }
    }

    private void finish(Channel channel, Clip clip, int version) {
        try {
            worker.execute(() -> {
                if (clips.get(channel) != clip) return;
                clips.remove(channel);
                synchronized (this) {
                    if (current(channel, version)) resources.remove(channel);
                }
                stopClip(clip);
            });
        } catch (RejectedExecutionException ignored) {
            // close() already queued cleanup before shutting down the worker.
        }
    }

    synchronized void stop(Channel channel) {
        if (closed) return;
        versions.merge(channel, 1, Integer::sum);
        resources.remove(channel);
        worker.execute(() -> stopClip(clips.remove(channel)));
    }

    private static void stopClip(Clip clip) {
        if (clip == null) return;
        // Stop before close: DirectClip.close can wait on its own mixer thread.
        clip.stop();
        clip.flush();
        clip.close();
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        resources.clear();
        worker.execute(() -> {
            for (Clip clip : clips.values()) stopClip(clip);
            clips.clear();
        });
        worker.shutdown();
    }
}
