package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.util.Consumer;

import de.danoeh.antennapod.storage.preferences.UserPreferences;

import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.AudioAttributes;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelectionArray;

import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.mp3.Mp3Extractor;
import androidx.media3.ui.DefaultTrackNameProvider;
import androidx.media3.ui.TrackNameProvider;
import de.danoeh.antennapod.net.common.UserAgentInterceptor;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.playback.service.R;
import de.danoeh.antennapod.net.common.HttpCredentialEncoder;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.model.playback.Playable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@OptIn(markerClass = UnstableApi.class)
public class ExoPlayerWrapper {
    public static final int BUFFERING_STARTED = -1;
    public static final int BUFFERING_ENDED = -2;
    private static final String TAG = "ExoPlayerWrapper";

    private final Context context;
    private final Disposable bufferingUpdateDisposable;
    private ExoPlayer exoPlayer;
    private MediaSource mediaSource;
    private Runnable audioSeekCompleteListener;
    private Runnable audioCompletionListener;
    private Consumer<String> audioErrorListener;
    private Consumer<Integer> bufferingUpdateListener;
    private PlaybackParameters playbackParameters;
    private DefaultTrackSelector trackSelector;
    private SimpleCache simpleCache;
    @Nullable
    private LoudnessEnhancer loudnessEnhancer = null;
    @Nullable
    private DynamicsProcessing dynamicsProcessing = null;
    private boolean volumeNormalizationEnabled = false;

    // Crossfade support - secondary player for true overlapping crossfade
    @Nullable
    private ExoPlayer crossfadePlayer = null;
    @Nullable
    private MediaSource crossfadeMediaSource = null;
    private boolean crossfadeInProgress = false;
    private Disposable crossfadeDisposable = null;

    ExoPlayerWrapper(Context context) {
        this.context = context;
        createPlayer();
        playbackParameters = exoPlayer.getPlaybackParameters();
        bufferingUpdateDisposable = Observable.interval(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tickNumber -> {
                    if (bufferingUpdateListener != null) {
                        bufferingUpdateListener.accept(exoPlayer.getBufferedPercentage());
                    }
                });
    }

    private void createPlayer() {
        DefaultLoadControl.Builder loadControl = new DefaultLoadControl.Builder();
        loadControl.setBufferDurationsMs((int) TimeUnit.HOURS.toMillis(1), (int) TimeUnit.HOURS.toMillis(3),
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
        loadControl.setBackBuffer((int) TimeUnit.MINUTES.toMillis(5), true);
        trackSelector = new DefaultTrackSelector(context);
        exoPlayer = new ExoPlayer.Builder(context, new DefaultRenderersFactory(context))
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl.build())
                .build();
        exoPlayer.setSeekParameters(SeekParameters.EXACT);
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(@Player.State int playbackState) {
                if (audioCompletionListener != null && playbackState == Player.STATE_ENDED) {
                    audioCompletionListener.run();
                } else if (bufferingUpdateListener != null && playbackState == Player.STATE_BUFFERING) {
                    bufferingUpdateListener.accept(BUFFERING_STARTED);
                } else if (bufferingUpdateListener != null) {
                    bufferingUpdateListener.accept(BUFFERING_ENDED);
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                if (audioErrorListener != null) {
                    if (NetworkUtils.wasDownloadBlocked(error)) {
                        audioErrorListener.accept(context.getString(R.string.download_error_blocked));
                    } else {
                        Throwable cause = error.getCause();
                        if (cause instanceof HttpDataSource.HttpDataSourceException) {
                            if (cause.getCause() != null) {
                                cause = cause.getCause();
                            }
                        }
                        if (cause != null && "Source error".equals(cause.getMessage())) {
                            cause = cause.getCause();
                        }
                        if (cause != null && cause.getMessage() != null) {
                            audioErrorListener.accept(cause.getMessage());
                        } else if (error.getMessage() != null && cause != null) {
                            audioErrorListener.accept(error.getMessage() + ": " + cause.getClass().getSimpleName());
                        } else {
                            audioErrorListener.accept(null);
                        }
                    }
                }
            }

            @Override
            public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition,
                                                @NonNull Player.PositionInfo newPosition,
                                                @Player.DiscontinuityReason int reason) {
                if (audioSeekCompleteListener != null && reason == Player.DISCONTINUITY_REASON_SEEK) {
                    audioSeekCompleteListener.run();
                }
            }

            @Override
            public void onAudioSessionIdChanged(int audioSessionId) {
                initLoudnessEnhancer(audioSessionId);
            }
        });
        simpleCache = new SimpleCache(new File(context.getCacheDir(), "streaming"),
                new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024), new StandaloneDatabaseProvider(context));
        initLoudnessEnhancer(exoPlayer.getAudioSessionId());
    }

    public int getCurrentPosition() {
        return (int) exoPlayer.getCurrentPosition();
    }

    public float getCurrentSpeedMultiplier() {
        return playbackParameters.speed;
    }

    public boolean getCurrentSkipSilence() {
        return exoPlayer.getSkipSilenceEnabled();
    }

    public int getDuration() {
        if (exoPlayer.getDuration() == C.TIME_UNSET) {
            return Playable.INVALID_TIME;
        }
        return (int) exoPlayer.getDuration();
    }

    public boolean isPlaying() {
        return exoPlayer.getPlayWhenReady();
    }

    public void pause() {
        exoPlayer.pause();
    }

    public void prepare() throws IllegalStateException {
        exoPlayer.setMediaSource(mediaSource, false);
        exoPlayer.prepare();
    }

    public void release() {
        bufferingUpdateDisposable.dispose();
        // Clean up crossfade resources
        if (crossfadeDisposable != null && !crossfadeDisposable.isDisposed()) {
            crossfadeDisposable.dispose();
            crossfadeDisposable = null;
        }
        releaseCrossfadePlayer();
        crossfadeInProgress = false;

        if (exoPlayer != null) {
            exoPlayer.release();
        }
        if (simpleCache != null) {
            simpleCache.release();
            simpleCache = null;
        }
        if (loudnessEnhancer != null) {
            try {
                loudnessEnhancer.release();
            } catch (Exception e) {
                // Expected if audio effect already released
            }
            loudnessEnhancer = null;
        }
        if (dynamicsProcessing != null) {
            try {
                dynamicsProcessing.release();
            } catch (Exception e) {
                // Expected if audio effect already released
            }
            dynamicsProcessing = null;
        }
        audioSeekCompleteListener = null;
        audioCompletionListener = null;
        audioErrorListener = null;
        bufferingUpdateListener = null;
    }

    public void reset() {
        exoPlayer.release();
        if (simpleCache != null) {
            simpleCache.release();
            simpleCache = null;
        }
        createPlayer();
    }

    public void seekTo(int i) throws IllegalStateException {
        exoPlayer.seekTo(i);
        if (audioSeekCompleteListener != null) {
            audioSeekCompleteListener.run();
        }
    }

    public void setAudioStreamType(int i) {
        AudioAttributes a = exoPlayer.getAudioAttributes();
        AudioAttributes.Builder b = new AudioAttributes.Builder();
        b.setContentType(i);
        b.setFlags(a.flags);
        b.setUsage(a.usage);
        exoPlayer.setAudioAttributes(b.build(), false);
    }

    public void setDataSource(String s, String user, String password)
            throws IllegalArgumentException, IllegalStateException {
        Log.d(TAG, "setDataSource: " + s);
        final DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory();
        httpDataSourceFactory.setUserAgent(UserAgentInterceptor.USER_AGENT);
        httpDataSourceFactory.setAllowCrossProtocolRedirects(true);
        httpDataSourceFactory.setKeepPostFor302Redirects(true);

        if (!TextUtils.isEmpty(user) && !TextUtils.isEmpty(password)) {
            final HashMap<String, String> requestProperties = new HashMap<>();
            requestProperties.put("Authorization", HttpCredentialEncoder.encode(user, password, "ISO-8859-1"));
            httpDataSourceFactory.setDefaultRequestProperties(requestProperties);
        }
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context, httpDataSourceFactory);
        if (s.startsWith("http")) {
            dataSourceFactory = new CacheDataSource.Factory()
                    .setCache(simpleCache)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory);
        }
        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        extractorsFactory.setConstantBitrateSeekingEnabled(true);
        extractorsFactory.setMp3ExtractorFlags(Mp3Extractor.FLAG_DISABLE_ID3_METADATA);
        ProgressiveMediaSource.Factory f = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory);
        final MediaItem mediaItem = MediaItem.fromUri(Uri.parse(s));
        mediaSource = f.createMediaSource(mediaItem);
    }

    public void setDataSource(String s) throws IllegalArgumentException, IllegalStateException {
        setDataSource(s, null, null);
    }

    public void setDisplay(SurfaceHolder sh) {
        exoPlayer.setVideoSurfaceHolder(sh);
    }

    public void setPlaybackParams(float speed, boolean skipSilence) {
        playbackParameters = new PlaybackParameters(speed, playbackParameters.pitch);
        exoPlayer.setSkipSilenceEnabled(skipSilence);
        exoPlayer.setPlaybackParameters(playbackParameters);
    }

    public void setVolume(float v, float v1) {
        if (v > 1) {
            exoPlayer.setVolume(1f);
            try {
                if (loudnessEnhancer != null) {
                    loudnessEnhancer.setEnabled(true);
                    loudnessEnhancer.setTargetGain((int) (1000 * (v - 1)));
                }
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        } else {
            exoPlayer.setVolume(v);
            try {
                if (loudnessEnhancer != null) {
                    loudnessEnhancer.setEnabled(false);
                }
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }
    }

    public void start() {
        exoPlayer.play();
        // Can't set params when paused - so always set it on start in case they changed
        exoPlayer.setPlaybackParameters(playbackParameters);
    }

    public void stop() {
        exoPlayer.stop();
    }

    public List<String> getAudioTracks() {
        List<String> trackNames = new ArrayList<>();
        TrackNameProvider trackNameProvider = new DefaultTrackNameProvider(context.getResources());
        for (Format format : getFormats()) {
            trackNames.add(trackNameProvider.getTrackName(format));
        }
        return trackNames;
    }

    private List<Format> getFormats() {
        List<Format> formats = new ArrayList<>();
        MappingTrackSelector.MappedTrackInfo trackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (trackInfo == null) {
            return Collections.emptyList();
        }
        TrackGroupArray trackGroups = trackInfo.getTrackGroups(getAudioRendererIndex());
        for (int i = 0; i < trackGroups.length; i++) {
            formats.add(trackGroups.get(i).getFormat(0));
        }
        return formats;
    }

    public void setAudioTrack(int track) {
        MappingTrackSelector.MappedTrackInfo trackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (trackInfo == null) {
            return;
        }
        TrackGroupArray trackGroups = trackInfo.getTrackGroups(getAudioRendererIndex());
        DefaultTrackSelector.SelectionOverride override = new DefaultTrackSelector.SelectionOverride(track, 0);
        DefaultTrackSelector.Parameters params = trackSelector.buildUponParameters()
                .setSelectionOverride(getAudioRendererIndex(), trackGroups, override).build();
        trackSelector.setParameters(params);
    }

    private int getAudioRendererIndex() {
        for (int i = 0; i < exoPlayer.getRendererCount(); i++) {
            if (exoPlayer.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
                return i;
            }
        }
        return -1;
    }

    public int getSelectedAudioTrack() {
        TrackSelectionArray trackSelections = exoPlayer.getCurrentTrackSelections();
        List<Format> availableFormats = getFormats();
        for (int i = 0; i < trackSelections.length; i++) {
            ExoTrackSelection track = (ExoTrackSelection) trackSelections.get(i);
            if (track == null) {
                continue;
            }
            if (availableFormats.contains(track.getSelectedFormat())) {
                return availableFormats.indexOf(track.getSelectedFormat());
            }
        }
        return -1;
    }

    void setOnCompletionListener(Runnable audioCompletionListener) {
        this.audioCompletionListener = audioCompletionListener;
    }

    void setOnSeekCompleteListener(Runnable audioSeekCompleteListener) {
        this.audioSeekCompleteListener = audioSeekCompleteListener;
    }

    void setOnErrorListener(Consumer<String> audioErrorListener) {
        this.audioErrorListener = audioErrorListener;
    }

    int getVideoWidth() {
        if (exoPlayer.getVideoFormat() == null) {
            return 0;
        }
        return exoPlayer.getVideoFormat().width;
    }

    int getVideoHeight() {
        if (exoPlayer.getVideoFormat() == null) {
            return 0;
        }
        return exoPlayer.getVideoFormat().height;
    }

    void setOnBufferingUpdateListener(Consumer<Integer> bufferingUpdateListener) {
        this.bufferingUpdateListener = bufferingUpdateListener;
    }

    private void initLoudnessEnhancer(int audioStreamId) {
        if (!VolumeAdaptionSetting.isBoostSupported()) {
            return;
        }

        LoudnessEnhancer newEnhancer = new LoudnessEnhancer(audioStreamId);
        LoudnessEnhancer oldEnhancer = this.loudnessEnhancer;
        if (oldEnhancer != null) {
            try {
                newEnhancer.setEnabled(oldEnhancer.getEnabled());
                if (oldEnhancer.getEnabled()) {
                    newEnhancer.setTargetGain((int) oldEnhancer.getTargetGain());
                }
                oldEnhancer.release();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }

        this.loudnessEnhancer = newEnhancer;

        // Initialize volume normalization if enabled
        initVolumeNormalization(audioStreamId);
    }

    /**
     * Initialize DynamicsProcessing for automatic volume normalization.
     * Uses limiter and compressor to normalize loudness across different podcasts.
     * Requires Android P (API 28) or higher.
     */
    private void initVolumeNormalization(int audioSessionId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.d(TAG, "Volume normalization requires Android P or higher");
            return;
        }

        boolean shouldEnable = UserPreferences.isAutoNormalizeVolume() || UserPreferences.isRadioMode();

        try {
            // Release old processor if exists
            if (dynamicsProcessing != null) {
                dynamicsProcessing.release();
                dynamicsProcessing = null;
            }

            if (!shouldEnable) {
                volumeNormalizationEnabled = false;
                return;
            }

            // Create DynamicsProcessing configuration for volume normalization
            // Using limiter to prevent clipping and compressor for consistent loudness
            DynamicsProcessing.Config.Builder configBuilder = new DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1,      // channel count
                    true,   // pre-eq enabled
                    0,      // pre-eq band count
                    true,   // multi-band compressor enabled
                    1,      // multi-band compressor band count
                    true,   // post-eq enabled
                    0,      // post-eq band count
                    true    // limiter enabled
            );

            DynamicsProcessing.Config config = configBuilder.build();
            dynamicsProcessing = new DynamicsProcessing(0, audioSessionId, config);

            // Configure limiter to prevent clipping and normalize peaks
            // Limiter settings for consistent output level
            DynamicsProcessing.Limiter limiter = dynamicsProcessing.getLimiterByChannelIndex(0);
            limiter.setEnabled(true);
            limiter.setLinkGroup(0);
            limiter.setAttackTime(1.0f);      // Fast attack (1ms)
            limiter.setReleaseTime(100.0f);   // Medium release (100ms)
            limiter.setRatio(10.0f);          // High ratio for limiting
            limiter.setThreshold(-1.0f);      // Threshold at -1dB to prevent clipping
            limiter.setPostGain(0.0f);        // No additional gain
            dynamicsProcessing.setLimiterByChannelIndex(0, limiter);

            // Configure multi-band compressor for dynamic range compression
            // This helps normalize loudness differences between podcasts
            DynamicsProcessing.MbcBand mbcBand = dynamicsProcessing.getMbcBandByChannelIndex(0, 0);
            mbcBand.setEnabled(true);
            mbcBand.setAttackTime(10.0f);     // 10ms attack
            mbcBand.setReleaseTime(100.0f);   // 100ms release
            mbcBand.setRatio(3.0f);           // 3:1 compression ratio
            mbcBand.setThreshold(-18.0f);     // Start compressing at -18dB
            mbcBand.setKneeWidth(6.0f);       // Soft knee for natural sound
            mbcBand.setPreGain(6.0f);         // Add 6dB makeup gain
            mbcBand.setPostGain(0.0f);        // No post gain
            dynamicsProcessing.setMbcBandByChannelIndex(0, 0, mbcBand);

            dynamicsProcessing.setEnabled(true);
            volumeNormalizationEnabled = true;
            Log.d(TAG, "Volume normalization enabled with DynamicsProcessing");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize volume normalization: " + e.getMessage());
            volumeNormalizationEnabled = false;
            if (dynamicsProcessing != null) {
                try {
                    dynamicsProcessing.release();
                } catch (Exception ignored) {
                    // Expected if audio effect already released
                }
                dynamicsProcessing = null;
            }
        }
    }

    /**
     * Update volume normalization state based on current preferences.
     * Call this when Radio Mode or Auto Normalize Volume settings change.
     */
    public void updateVolumeNormalization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && exoPlayer != null) {
            initVolumeNormalization(exoPlayer.getAudioSessionId());
        }
    }

    public boolean isVolumeNormalizationEnabled() {
        return volumeNormalizationEnabled;
    }

    // ==================== TRUE CROSSFADE SUPPORT ====================
    // Dual-player approach: secondary player loads next track while primary plays
    // Both tracks play simultaneously during crossfade with overlapping volume fades

    /**
     * Create a secondary ExoPlayer instance for crossfade.
     * This player loads and plays the next track during crossfade.
     */
    private ExoPlayer createCrossfadePlayer() {
        DefaultLoadControl.Builder loadControl = new DefaultLoadControl.Builder();
        loadControl.setBufferDurationsMs(
                (int) TimeUnit.SECONDS.toMillis(30),  // Smaller buffer for crossfade
                (int) TimeUnit.MINUTES.toMillis(1),
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);

        ExoPlayer player = new ExoPlayer.Builder(context, new DefaultRenderersFactory(context))
                .setLoadControl(loadControl.build())
                .build();
        player.setSeekParameters(SeekParameters.EXACT);
        player.setVolume(0f);  // Start at zero volume for fade-in

        // Copy playback parameters from main player
        player.setPlaybackParameters(playbackParameters);
        player.setSkipSilenceEnabled(exoPlayer.getSkipSilenceEnabled());

        return player;
    }

    /**
     * Prepare the next track for crossfade.
     * Call this ahead of time so the track is ready when crossfade begins.
     *
     * @param url      Media URL to load
     * @param user     HTTP auth user (optional)
     * @param password HTTP auth password (optional)
     */
    public void prepareNextForCrossfade(String url, String user, String password) {
        Log.d(TAG, "prepareNextForCrossfade: " + url);

        // Release any existing crossfade player
        releaseCrossfadePlayer();

        // Create new crossfade player
        crossfadePlayer = createCrossfadePlayer();

        // Build the media source (same as setDataSource)
        final DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory();
        httpDataSourceFactory.setUserAgent(UserAgentInterceptor.USER_AGENT);
        httpDataSourceFactory.setAllowCrossProtocolRedirects(true);
        httpDataSourceFactory.setKeepPostFor302Redirects(true);

        if (!TextUtils.isEmpty(user) && !TextUtils.isEmpty(password)) {
            final HashMap<String, String> requestProperties = new HashMap<>();
            requestProperties.put("Authorization", HttpCredentialEncoder.encode(user, password, "ISO-8859-1"));
            httpDataSourceFactory.setDefaultRequestProperties(requestProperties);
        }

        DataSource.Factory dataSourceFactory;
        if (url.startsWith("http")) {
            dataSourceFactory = new CacheDataSource.Factory()
                    .setCache(simpleCache)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory);
        } else {
            dataSourceFactory = new DefaultDataSource.Factory(context, httpDataSourceFactory);
        }

        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        extractorsFactory.setConstantBitrateSeekingEnabled(true);
        extractorsFactory.setMp3ExtractorFlags(Mp3Extractor.FLAG_DISABLE_ID3_METADATA);
        ProgressiveMediaSource.Factory f = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory);
        final MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        crossfadeMediaSource = f.createMediaSource(mediaItem);

        // Prepare the media source
        crossfadePlayer.setMediaSource(crossfadeMediaSource, false);
        crossfadePlayer.prepare();

        Log.d(TAG, "Crossfade player prepared and ready");
    }

    /**
     * Check if next track is ready for crossfade.
     *
     * @return true if crossfade player is ready to play
     */
    public boolean isCrossfadeReady() {
        return crossfadePlayer != null
                && crossfadePlayer.getPlaybackState() == Player.STATE_READY;
    }

    /**
     * Start the crossfade transition.
     * Both tracks play simultaneously while volumes cross-fade.
     *
     * @param durationMs Crossfade duration in milliseconds
     * @param onComplete Callback when crossfade completes
     */
    public void startCrossfade(long durationMs, Runnable onComplete) {
        if (crossfadePlayer == null || crossfadeInProgress) {
            Log.w(TAG, "Cannot start crossfade: player=" + crossfadePlayer + ", inProgress=" + crossfadeInProgress);
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        Log.d(TAG, "Starting true crossfade over " + durationMs + "ms");
        crossfadeInProgress = true;

        // Get starting volumes
        final float startVolumeMain = exoPlayer.getVolume();
        final float startVolumeCrossfade = 0f;

        // Start the crossfade player
        crossfadePlayer.setVolume(0f);
        crossfadePlayer.play();

        // Calculate fade steps (update every 50ms for smooth transitions)
        final long intervalMs = 50;
        final int totalSteps = (int) (durationMs / intervalMs);

        // Dispose of any existing crossfade disposable
        if (crossfadeDisposable != null && !crossfadeDisposable.isDisposed()) {
            crossfadeDisposable.dispose();
        }

        // Create the crossfade animation using RxJava interval
        crossfadeDisposable = Observable.intervalRange(1, totalSteps, 0, intervalMs, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        step -> {
                            // Calculate current volumes
                            float progress = step.floatValue() / totalSteps;
                            float mainVolume = startVolumeMain * (1f - progress);
                            float crossfadeVolume = startVolumeMain * progress;

                            // Apply volumes
                            exoPlayer.setVolume(Math.max(0f, mainVolume));
                            if (crossfadePlayer != null) {
                                crossfadePlayer.setVolume(Math.min(1f, crossfadeVolume));
                            }

                            Log.v(TAG, "Crossfade step " + step + "/" + totalSteps
                                    + " - main: " + mainVolume + ", next: " + crossfadeVolume);
                        },
                        error -> {
                            Log.e(TAG, "Crossfade error: " + error.getMessage());
                            completeCrossfade(onComplete);
                        },
                        () -> {
                            Log.d(TAG, "Crossfade complete, swapping players");
                            completeCrossfade(onComplete);
                        }
                );
    }

    /**
     * Complete the crossfade by swapping players.
     * The crossfade player becomes the main player.
     *
     * @param onComplete Callback to run after swap
     */
    private void completeCrossfade(Runnable onComplete) {
        if (crossfadePlayer == null) {
            crossfadeInProgress = false;
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        // Stop and release the old main player
        ExoPlayer oldPlayer = exoPlayer;
        oldPlayer.stop();
        oldPlayer.release();

        // Swap: crossfade player becomes the main player
        exoPlayer = crossfadePlayer;
        mediaSource = crossfadeMediaSource;

        // Ensure full volume on new main player
        exoPlayer.setVolume(1f);

        // Clear crossfade references
        crossfadePlayer = null;
        crossfadeMediaSource = null;
        crossfadeInProgress = false;

        // Re-initialize loudness enhancer for new player
        initLoudnessEnhancer(exoPlayer.getAudioSessionId());

        // Set up listener for the new main player
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(@Player.State int playbackState) {
                if (audioCompletionListener != null && playbackState == Player.STATE_ENDED) {
                    audioCompletionListener.run();
                } else if (bufferingUpdateListener != null && playbackState == Player.STATE_BUFFERING) {
                    bufferingUpdateListener.accept(BUFFERING_STARTED);
                } else if (bufferingUpdateListener != null) {
                    bufferingUpdateListener.accept(BUFFERING_ENDED);
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                if (audioErrorListener != null) {
                    Throwable cause = error.getCause();
                    if (cause != null && cause.getMessage() != null) {
                        audioErrorListener.accept(cause.getMessage());
                    } else {
                        audioErrorListener.accept(error.getMessage());
                    }
                }
            }

            @Override
            public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition,
                                                @NonNull Player.PositionInfo newPosition,
                                                @Player.DiscontinuityReason int reason) {
                if (audioSeekCompleteListener != null && reason == Player.DISCONTINUITY_REASON_SEEK) {
                    audioSeekCompleteListener.run();
                }
            }

            @Override
            public void onAudioSessionIdChanged(int audioSessionId) {
                initLoudnessEnhancer(audioSessionId);
            }
        });

        Log.d(TAG, "Player swap complete");

        if (onComplete != null) {
            onComplete.run();
        }
    }

    /**
     * Cancel an in-progress crossfade.
     */
    public void cancelCrossfade() {
        if (crossfadeDisposable != null && !crossfadeDisposable.isDisposed()) {
            crossfadeDisposable.dispose();
            crossfadeDisposable = null;
        }
        releaseCrossfadePlayer();
        crossfadeInProgress = false;
        // Restore main player volume
        if (exoPlayer != null) {
            exoPlayer.setVolume(1f);
        }
    }

    /**
     * Release the crossfade player and clean up resources.
     */
    private void releaseCrossfadePlayer() {
        if (crossfadePlayer != null) {
            try {
                crossfadePlayer.stop();
                crossfadePlayer.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing crossfade player: " + e.getMessage());
            }
            crossfadePlayer = null;
            crossfadeMediaSource = null;
        }
    }

    /**
     * Check if crossfade is currently in progress.
     */
    public boolean isCrossfadeInProgress() {
        return crossfadeInProgress;
    }

    /**
     * Get the crossfade player for seeking (if needed).
     */
    @Nullable
    public ExoPlayer getCrossfadePlayer() {
        return crossfadePlayer;
    }
}
