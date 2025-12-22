package de.danoeh.antennapod.ui.screen.playback.audio;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.Configuration;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import de.danoeh.antennapod.ui.chapters.ChapterUtils;
import de.danoeh.antennapod.ui.screen.chapter.ChaptersFragment;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.ui.common.DateFormatter;
import de.danoeh.antennapod.databinding.CoverFragmentBinding;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.EmbeddedChapterImage;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.episodes.ImageResourceUtils;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import de.danoeh.antennapod.ui.visualizer.VisualizerBridge;
import de.danoeh.antennapod.ui.visualizer.VisualizerViewModel;

import static android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;

/**
 * Displays the cover and the title of a FeedItem.
 */
public class CoverFragment extends Fragment {
    private static final String TAG = "CoverFragment";
    private CoverFragmentBinding viewBinding;
    private PlaybackController controller;
    private Disposable disposable;
    private int displayedChapterIndex = -1;
    private Playable media;
    private static final String KEY_VISUALIZER_SHOWING = "visualizer_showing";
    private VisualizerViewModel visualizerViewModel;
    private volatile boolean isVisualizerShowing = false;
    private volatile boolean isVisualizerInitialized = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                visualizerViewModel.onPermissionResult(isGranted);
                if (isGranted) {
                    // Permission granted, now show visualizer
                    showVisualizer();
                } else {
                    // Permission denied - user can't use visualizer without microphone permission
                    Log.w(TAG, "RECORD_AUDIO permission denied for visualizer");
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = CoverFragmentBinding.inflate(inflater);
        // Single tap on cover to play/pause (original behavior)
        viewBinding.imgvCover.setOnClickListener(v -> onPlayPause());
        // Double tap on cover to toggle visualizer
        viewBinding.imgvCover.setOnLongClickListener(v -> {
            toggleVisualizer();
            return true;
        });
        viewBinding.openDescription.setOnClickListener(view -> ((AudioPlayerFragment) requireParentFragment())
                .scrollToPage(AudioPlayerFragment.POS_DESCRIPTION, true));
        ColorFilter colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                viewBinding.txtvPodcastTitle.getCurrentTextColor(), BlendModeCompat.SRC_IN);
        viewBinding.butNextChapter.setColorFilter(colorFilter);
        viewBinding.butPrevChapter.setColorFilter(colorFilter);
        viewBinding.descriptionIcon.setColorFilter(colorFilter);
        viewBinding.chapterButton.setOnClickListener(v ->
                new ChaptersFragment().show(getChildFragmentManager(), ChaptersFragment.TAG));
        viewBinding.butPrevChapter.setOnClickListener(v -> seekToPrevChapter());
        viewBinding.butNextChapter.setOnClickListener(v -> seekToNextChapter());
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        configureForOrientation(getResources().getConfiguration());
        setupVisualizer();

        // Restore visualizer visibility state after config change
        if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_VISUALIZER_SHOWING, false)) {
            // Defer showing visualizer until after onStart when controller is available
            view.post(() -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    showVisualizer();
                }
            });
        }
    }

    private void setupVisualizer() {
        visualizerViewModel = new ViewModelProvider(this).get(VisualizerViewModel.class);

        VisualizerBridge.setupVisualizerView(
            viewBinding.visualizerView,
            visualizerViewModel,
            () -> media != null ? media.getImageLocation() : null
        );

        // Single tap on visualizer to play/pause (same as cover)
        viewBinding.visualizerView.setOnClickListener(v -> onPlayPause());
        // Long press on visualizer to toggle back to cover
        viewBinding.visualizerView.setOnLongClickListener(v -> {
            toggleVisualizer();
            return true;
        });
    }

    private void toggleVisualizer() {
        if (isVisualizerShowing) {
            // Hide visualizer, show cover
            hideVisualizer();
        } else {
            // Check permission before showing visualizer
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                visualizerViewModel.onPermissionResult(true);
                showVisualizer();
            } else {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        }
    }

    private void showVisualizer() {
        isVisualizerShowing = true;

        // Show visualizer, hide cover
        viewBinding.imgvCover.setVisibility(View.GONE);
        viewBinding.visualizerView.setVisibility(View.VISIBLE);

        // Try to initialize visualizer with audio session from playback service
        tryInitializeVisualizer();
        visualizerViewModel.setVisibility(true);
    }

    private void tryInitializeVisualizer() {
        if (isVisualizerInitialized) {
            return; // Already initialized
        }
        if (controller != null) {
            int audioSessionId = controller.getAudioSessionId();
            if (audioSessionId != 0) {
                Log.d(TAG, "Initializing visualizer with audio session ID: " + audioSessionId);
                visualizerViewModel.initializeVisualizer(audioSessionId);
                isVisualizerInitialized = true;
            } else {
                Log.d(TAG, "Cannot initialize visualizer yet - no audio session ID (playback may not have started)");
            }
        }
    }

    private void hideVisualizer() {
        isVisualizerShowing = false;
        isVisualizerInitialized = false; // Reset so it can be re-initialized next time

        // Show cover, hide visualizer
        viewBinding.imgvCover.setVisibility(View.VISIBLE);
        viewBinding.visualizerView.setVisibility(View.GONE);
        visualizerViewModel.setVisibility(false);
    }

    private void loadMediaInfo(boolean includingChapters) {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.<Playable>create(emitter -> {
            Playable media = controller.getMedia();
            if (media != null) {
                if (includingChapters) {
                    ChapterUtils.loadChapters(media, getContext(), false);
                }
                emitter.onSuccess(media);
            } else {
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(media -> {
                    this.media = media;
                    displayMediaInfo(media);
                    if (media.getChapters() == null && !includingChapters) {
                        loadMediaInfo(true);
                    }
                    // Re-initialize visualizer when media changes (new podcast = new audio session)
                    if (isVisualizerShowing) {
                        isVisualizerInitialized = false; // Reset to force re-initialization
                        tryInitializeVisualizer();
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void displayMediaInfo(@NonNull Playable media) {
        String pubDateStr = DateFormatter.formatAbbrev(getActivity(), media.getPubDate());
        viewBinding.txtvPodcastTitle.setText(StringUtils.stripToEmpty(media.getFeedTitle())
                + "\u00A0"
                + "・"
                + "\u00A0"
                + StringUtils.replace(StringUtils.stripToEmpty(pubDateStr), " ", "\u00A0"));
        if (media instanceof FeedMedia) {
            viewBinding.txtvPodcastTitle.setOnClickListener(v -> openFeed(((FeedMedia) media).getItem().getFeed()));
        } else {
            viewBinding.txtvPodcastTitle.setOnClickListener(null);
        }
        viewBinding.txtvPodcastTitle.setOnLongClickListener(v -> copyText(media.getFeedTitle()));
        viewBinding.txtvEpisodeTitle.setText(media.getEpisodeTitle());
        viewBinding.txtvEpisodeTitle.setOnLongClickListener(v -> copyText(media.getEpisodeTitle()));
        viewBinding.txtvEpisodeTitle.setOnClickListener(v -> {
            int lines = viewBinding.txtvEpisodeTitle.getLineCount();
            int animUnit = 1500;
            if (lines > viewBinding.txtvEpisodeTitle.getMaxLines()) {
                int titleHeight = viewBinding.txtvEpisodeTitle.getHeight()
                        - viewBinding.txtvEpisodeTitle.getPaddingTop()
                        - viewBinding.txtvEpisodeTitle.getPaddingBottom();
                ObjectAnimator verticalMarquee = ObjectAnimator.ofInt(
                        viewBinding.txtvEpisodeTitle, "scrollY", 0, (lines - viewBinding.txtvEpisodeTitle.getMaxLines())
                                        * (titleHeight / viewBinding.txtvEpisodeTitle.getMaxLines()))
                        .setDuration(lines * animUnit);
                ObjectAnimator fadeOut = ObjectAnimator.ofFloat(
                        viewBinding.txtvEpisodeTitle, "alpha", 0);
                fadeOut.setStartDelay(animUnit);
                fadeOut.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewBinding.txtvEpisodeTitle.scrollTo(0, 0);
                    }
                });
                ObjectAnimator fadeBackIn = ObjectAnimator.ofFloat(
                        viewBinding.txtvEpisodeTitle, "alpha", 1);
                AnimatorSet set = new AnimatorSet();
                set.playSequentially(verticalMarquee, fadeOut, fadeBackIn);
                set.start();
            }
        });
        
        displayedChapterIndex = -1;
        refreshChapterData(Chapter.getAfterPosition(media.getChapters(), media.getPosition()));
        updateChapterControlVisibility();
    }

    private void openFeed(Feed feed) {
        if (feed == null) {
            return;
        }
        if (feed.getState() == Feed.STATE_NOT_SUBSCRIBED) {
            startActivity(new OnlineFeedviewActivityStarter(getContext(), feed.getDownloadUrl()).getIntent());
        } else {
            new MainActivityStarter(getContext()).withOpenFeed(feed.getId()).withClearTop().start();
        }
    }

    private void updateChapterControlVisibility() {
        boolean chapterControlVisible = false;
        if (media.getChapters() != null) {
            chapterControlVisible = media.getChapters().size() > 0;
        } else if (media instanceof FeedMedia) {
            FeedMedia fm = ((FeedMedia) media);
            // If an item has chapters but they are not loaded yet, still display the button.
            chapterControlVisible = fm.getItem() != null && fm.getItem().hasChapters();
        }
        int newVisibility = chapterControlVisible ? View.VISIBLE : View.GONE;
        if (viewBinding.chapterButton.getVisibility() != newVisibility) {
            viewBinding.chapterButton.setVisibility(newVisibility);
            ObjectAnimator.ofFloat(viewBinding.chapterButton,
                    "alpha",
                    chapterControlVisible ? 0 : 1,
                    chapterControlVisible ? 1 : 0)
                    .start();
        }
    }

    private void refreshChapterData(int chapterIndex) {
        if (chapterIndex > -1 && media != null && media.getChapters() != null) {
            if (media.getPosition() > media.getDuration() || chapterIndex >= media.getChapters().size() - 1) {
                displayedChapterIndex = media.getChapters().size() - 1;
                viewBinding.butNextChapter.setVisibility(View.INVISIBLE);
            } else {
                displayedChapterIndex = chapterIndex;
                viewBinding.butNextChapter.setVisibility(View.VISIBLE);
            }
        }

        displayCoverImage();
    }

    private Chapter getCurrentChapter() {
        if (media == null || media.getChapters() == null || displayedChapterIndex == -1) {
            return null;
        }
        return media.getChapters().get(displayedChapterIndex);
    }

    private void seekToPrevChapter() {
        Chapter curr = getCurrentChapter();

        if (controller == null || curr == null || displayedChapterIndex == -1) {
            return;
        }

        if (displayedChapterIndex < 1) {
            controller.seekTo(0);
        } else if ((controller.getPosition() - 10000 * controller.getCurrentPlaybackSpeedMultiplier())
                < curr.getStart()) {
            refreshChapterData(displayedChapterIndex - 1);
            controller.seekTo((int) media.getChapters().get(displayedChapterIndex).getStart());
        } else {
            controller.seekTo((int) curr.getStart());
        }
    }

    private void seekToNextChapter() {
        if (controller == null || media == null || media.getChapters() == null
                || displayedChapterIndex == -1 || displayedChapterIndex + 1 >= media.getChapters().size()) {
            return;
        }

        refreshChapterData(displayedChapterIndex + 1);
        controller.seekTo((int) media.getChapters().get(displayedChapterIndex).getStart());
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                CoverFragment.this.loadMediaInfo(false);
            }
        };
        controller.init();
        loadMediaInfo(false);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Release visualizer resources to prevent battery drain and AudioRecord conflicts
        if (visualizerViewModel != null) {
            visualizerViewModel.releaseVisualizer();
        }
        isVisualizerInitialized = false;

        if (disposable != null) {
            disposable.dispose();
        }
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VISUALIZER_SHOWING, isVisualizerShowing);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (media == null) {
            return;
        }
        int newChapterIndex = Chapter.getAfterPosition(media.getChapters(), event.getPosition());
        if (newChapterIndex > -1 && newChapterIndex != displayedChapterIndex) {
            refreshChapterData(newChapterIndex);
        }

        // If visualizer is showing but not initialized, try to initialize it now
        // (playback has started, so we should have an audio session ID)
        if (isVisualizerShowing && !isVisualizerInitialized) {
            tryInitializeVisualizer();
        }
    }

    private void displayCoverImage() {
        RequestOptions options = new RequestOptions()
                .dontAnimate()
                .transform(new FitCenter(),
                        new RoundedCorners((int) (16 * getResources().getDisplayMetrics().density)));

        RequestBuilder<Drawable> cover = Glide.with(this)
                .load(media.getImageLocation())
                .error(Glide.with(this)
                        .load(ImageResourceUtils.getFallbackImageLocation(media))
                        .apply(options))
                .apply(options);

        if (displayedChapterIndex == -1 || media == null || media.getChapters() == null
                || TextUtils.isEmpty(media.getChapters().get(displayedChapterIndex).getImageUrl())) {
            cover.into(viewBinding.imgvCover);
        } else {
            Glide.with(this)
                    .load(EmbeddedChapterImage.getModelFor(media, displayedChapterIndex))
                    .apply(options)
                    .thumbnail(cover)
                    .error(cover)
                    .into(viewBinding.imgvCover);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configureForOrientation(newConfig);
    }

    private void configureForOrientation(Configuration newConfig) {
        boolean isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;

        viewBinding.coverFragment.setOrientation(isPortrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

        if (isPortrait) {
            viewBinding.coverHolder.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
            viewBinding.coverFragmentTextContainer.setLayoutParams(
                    new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        } else {
            viewBinding.coverHolder.setLayoutParams(new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));
            viewBinding.coverFragmentTextContainer.setLayoutParams(new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));
        }

        ((ViewGroup) viewBinding.episodeDetails.getParent()).removeView(viewBinding.episodeDetails);
        if (isPortrait) {
            viewBinding.coverFragment.addView(viewBinding.episodeDetails);
        } else {
            viewBinding.coverFragmentTextContainer.addView(viewBinding.episodeDetails);
        }
    }

    void onPlayPause() {
        if (controller == null) {
            return;
        }
        controller.playPause();
    }

    private boolean copyText(String text) {
        ClipboardManager clipboardManager = ContextCompat.getSystemService(requireContext(), ClipboardManager.class);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("AntennaPod", text));
        }
        if (Build.VERSION.SDK_INT <= 32) {
            EventBus.getDefault().post(new MessageEvent(getString(R.string.copied_to_clipboard)));
        }
        return true;
    }
}
