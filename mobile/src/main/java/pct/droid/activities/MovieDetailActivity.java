package pct.droid.activities;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nirhart.parallaxscroll.views.ParallaxScrollView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Locale;

import butterknife.InjectView;
import pct.droid.R;
import pct.droid.base.PopcornApplication;
import pct.droid.base.preferences.Prefs;
import pct.droid.base.providers.media.types.Movie;
import pct.droid.base.utils.LogUtils;
import pct.droid.base.utils.NetworkUtils;
import pct.droid.base.utils.PixelUtils;
import pct.droid.base.utils.PrefUtils;
import pct.droid.base.utils.StringUtils;
import pct.droid.base.youtube.YouTubeData;
import pct.droid.fragments.QualitySelectorDialogFragment;
import pct.droid.fragments.SubtitleSelectorDialogFragment;
import pct.droid.fragments.SynopsisDialogFragment;
import pct.droid.fragments.WifiOnlyDialogFragment;
import pct.droid.utils.ActionBarBackground;

public class MovieDetailActivity extends BaseActivity implements QualitySelectorDialogFragment.Listener, SubtitleSelectorDialogFragment.Listener {

    private Movie mItem;
    private Drawable mPlayButtonDrawable;
    private Integer mLastScrollLocation = 0, mPaletteColor = R.color.primary, mOpenBarPos, mHeaderHeight, mToolbarHeight, mParallaxHeight;
    private Boolean mTransparentBar = true, mOpenBar = true, mIsFavourited = false;
    private String mQuality, mSubLanguage;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.scrollView)
    ParallaxScrollView scrollView;
    @InjectView(R.id.coverImage)
    ImageView coverImage;
    @InjectView(R.id.headerProgress)
    ProgressBar headerProgress;
    @InjectView(R.id.mainInfoBlock)
    RelativeLayout mainInfoBlock;
    @InjectView(R.id.playButton)
    ImageButton playButton;
    @InjectView(R.id.titleText)
    TextView titleText;
    @InjectView(R.id.yearText)
    TextView yearText;
    @InjectView(R.id.runtimeText)
    TextView runtimeText;
    @InjectView(R.id.ratingText)
    TextView ratingText;
    @InjectView(R.id.synopsisText)
    TextView synopsisText;
    //@InjectView(R.id.favouriteText)
    //TextView favouriteText;
    @InjectView(R.id.synopsisBlock)
    LinearLayout synopsisBlock;
    @InjectView(R.id.qualityBlock)
    LinearLayout qualityBlock;
    @InjectView(R.id.qualityText)
    TextView qualityText;
    //@InjectView(R.id.favouriteBlock)
    //LinearLayout favouriteBlock;
    @InjectView(R.id.trailerBlock)
    LinearLayout trailerBlock;
    @InjectView(R.id.subtitlesBlock)
    LinearLayout subtitlesBlock;
    @InjectView(R.id.subtitlesText)
    TextView subtitlesText;

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Bundle b;
            switch (v.getId()) {
                case R.id.synopsisBlock:
                    if (getFragmentManager().findFragmentByTag("overlay_fragment") != null)
                        return;
                    SynopsisDialogFragment synopsisDialogFragment = new SynopsisDialogFragment();
                    b = new Bundle();
                    b.putString("text", mItem.synopsis);
                    synopsisDialogFragment.setArguments(b);
                    synopsisDialogFragment.show(getFragmentManager(), "overlay_fragment");
                    break;
                case R.id.qualityBlock:
                    if (getFragmentManager().findFragmentByTag("overlay_fragment") != null)
                        return;
                    QualitySelectorDialogFragment qualitySelectorDialogFragment = new QualitySelectorDialogFragment();
                    b = new Bundle();
                    b.putStringArray(QualitySelectorDialogFragment.QUALITIES, mItem.torrents.keySet().toArray(new String[mItem.torrents.size()]));
                    qualitySelectorDialogFragment.setArguments(b);
                    qualitySelectorDialogFragment.show(getFragmentManager(), "overlay_fragment");
                    break;
                case R.id.subtitlesBlock:
                    if (getFragmentManager().findFragmentByTag("overlay_fragment") != null)
                        return;
                    SubtitleSelectorDialogFragment subtitleSelectorDialogFragment = new SubtitleSelectorDialogFragment();
                    b = new Bundle();
                    b.putStringArray(SubtitleSelectorDialogFragment.LANGUAGES, mItem.subtitles.keySet().toArray(new String[mItem.subtitles.size()]));
                    subtitleSelectorDialogFragment.setArguments(b);
                    subtitleSelectorDialogFragment.show(getFragmentManager(), "overlay_fragment");
                    break;
                case R.id.trailerBlock:
                    Intent trailerIntent = new Intent(MovieDetailActivity.this, TrailerPlayerActivity.class);
                    if (!YouTubeData.isYouTubeUrl(mItem.trailer)) {
                        trailerIntent = new Intent(MovieDetailActivity.this, VideoPlayerActivity.class);
                    }
                    trailerIntent.putExtra(TrailerPlayerActivity.DATA, mItem);
                    trailerIntent.putExtra(TrailerPlayerActivity.LOCATION, mItem.trailer);
                    startActivity(trailerIntent);
                    break;
                case R.id.playButton:
                    final String streamUrl = mItem.torrents.get(mQuality).url;
                    if (PrefUtils.get(MovieDetailActivity.this, Prefs.WIFI_ONLY, true) && !NetworkUtils.isConnectedToWifi() && NetworkUtils.isConnectedToCellular()) {
                        WifiOnlyDialogFragment dialogFragment = new WifiOnlyDialogFragment();
                        dialogFragment.show(getFragmentManager(), "overlay_fragment");
                    } else {
                        Intent streamIntent = new Intent(MovieDetailActivity.this, StreamLoadingActivity.class);
                        streamIntent.putExtra(StreamLoadingActivity.STREAM_URL, streamUrl);
                        streamIntent.putExtra(StreamLoadingActivity.QUALITY, mQuality);
                        streamIntent.putExtra(StreamLoadingActivity.DATA, mItem);
                        if (mSubLanguage != null)
                            streamIntent.putExtra(StreamLoadingActivity.SUBTITLES, mSubLanguage);
                        startActivity(streamIntent);
                    }
                    break;
            }

        }
    };

    private ViewTreeObserver.OnScrollChangedListener mOnScrollListener = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            if (mToolbarHeight == 0) {
                mToolbarHeight = toolbar.getHeight();
                mHeaderHeight = mParallaxHeight - mToolbarHeight;
            }

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) toolbar.getLayoutParams();

            if (scrollView.getScrollY() > mHeaderHeight) {
                if (mLastScrollLocation > scrollView.getScrollY()) {
                    // scroll up
                    if ((mOpenBarPos == null || !mOpenBar) && layoutParams.topMargin <= -mToolbarHeight)
                        mOpenBarPos = scrollView.getScrollY() - mToolbarHeight;
                    mOpenBar = true;
                } else if (mLastScrollLocation < scrollView.getScrollY()) {
                    // scroll down
                    if (mOpenBarPos == null || mOpenBar)
                        mOpenBarPos = scrollView.getScrollY();
                    mOpenBar = false;
                }

                if (layoutParams.topMargin <= 0) {
                    layoutParams.topMargin = mOpenBarPos - scrollView.getScrollY();
                }

                if (layoutParams.topMargin > 0) {
                    layoutParams.topMargin = 0;
                }
            }

            if (layoutParams.topMargin < 0) {
                scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            } else {
                scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            }

                /* Fade out when over header */
            if (mParallaxHeight - scrollView.getScrollY() < 0) {
                if (mTransparentBar) {
                    mTransparentBar = false;
                    ActionBarBackground.changeColor(MovieDetailActivity.this, mPaletteColor, false);
                }
            } else {
                if (!mTransparentBar) {
                    mTransparentBar = true;
                    ActionBarBackground.fadeOut(MovieDetailActivity.this);
                }
            }

            toolbar.setLayoutParams(layoutParams);

            mLastScrollLocation = scrollView.getScrollY();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_moviedetail);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ActionBarBackground.fadeOut(this);

        Drawable playButtonDrawable = PixelUtils.changeDrawableColor(this, R.drawable.ic_av_play_button, getResources().getColor(R.color.primary));
        if (mPlayButtonDrawable == null) playButton.setImageDrawable(playButtonDrawable);

        playButton.setOnClickListener(mOnClickListener);
        synopsisBlock.setOnClickListener(mOnClickListener);
        trailerBlock.setOnClickListener(mOnClickListener);
        subtitlesBlock.setOnClickListener(mOnClickListener);
        //favouriteBlock.setOnClickListener(mOnClickListener);
        qualityBlock.setOnClickListener(mOnClickListener);

        mParallaxHeight = getResources().getDimensionPixelSize(R.dimen.parallax_header_height);
        mToolbarHeight = toolbar.getHeight();
        mHeaderHeight = mParallaxHeight - mToolbarHeight;
        scrollView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollListener);

        mItem = getIntent().getParcelableExtra("item");
        LogUtils.d(mItem.toString());
        titleText.setText(mItem.title);
        yearText.setText(mItem.year);
        ratingText.setText(mItem.rating + "/10");

        if (mItem.runtime != null && !mItem.runtime.isEmpty() && Integer.parseInt(mItem.runtime) > 0) {
            runtimeText.setText(mItem.runtime + " " + getString(R.string.minutes));
        } else {
            runtimeText.setText("n/a " + getString(R.string.minutes));
        }

        if (mItem.synopsis != null) {
            synopsisText.setText(mItem.synopsis);
        } else {
            synopsisBlock.setClickable(false);
        }

        if (mItem.trailer == null) {
            trailerBlock.setVisibility(View.GONE);
        }

        if (mItem.subtitles.containsKey(PrefUtils.get(this, Prefs.SUBTITLE_DEFAULT, "no-subs"))) {
            onSubtitleLanguageSelected(PrefUtils.get(this, Prefs.SUBTITLE_DEFAULT, "no-subs"));
        }

        PopcornApplication.getPicasso().load(mItem.image).into(coverImage, new Callback() {
            @Override
            public void onSuccess() {
                Palette palette = Palette.generate(((BitmapDrawable)coverImage.getDrawable()).getBitmap());

                int vibrantColor = palette.getVibrantColor(R.color.primary);
                if (vibrantColor == R.color.primary) {
                    mPaletteColor = palette.getMutedColor(R.color.primary);
                } else {
                    mPaletteColor = vibrantColor;
                }

                final ObjectAnimator mainInfoBlockColorFade = ObjectAnimator.ofObject(mainInfoBlock, "backgroundColor", new ArgbEvaluator(), getResources().getColor(R.color.primary), mPaletteColor);
                mainInfoBlockColorFade.setDuration(500);
                Drawable oldDrawable = PixelUtils.changeDrawableColor(MovieDetailActivity.this, R.drawable.ic_av_play_button, getResources().getColor(R.color.primary));
                mPlayButtonDrawable = PixelUtils.changeDrawableColor(MovieDetailActivity.this, R.drawable.ic_av_play_button, mPaletteColor);
                final TransitionDrawable td = new TransitionDrawable(new Drawable[]{oldDrawable, mPlayButtonDrawable});

                // Delay to make sure transition is smooth
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        playButton.setImageDrawable(td);
                        Animation fadeInAnim = AnimationUtils.loadAnimation(MovieDetailActivity.this, android.R.anim.fade_in);
                        mainInfoBlockColorFade.start();
                        td.startTransition(500);
                        coverImage.setVisibility(View.VISIBLE);
                        coverImage.startAnimation(fadeInAnim);
                    }
                }, 1000);
            }

            @Override
            public void onError() {
                headerProgress.setVisibility(View.GONE);
            }
        });

        /*PopcornApplication.getPicasso().load(mItem.image).into(new Target() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                Palette palette = Palette.generate(bitmap);

                int vibrantColor = palette.getVibrantColor(R.color.primary);
                if (vibrantColor == R.color.primary) {
                    mPaletteColor = palette.getMutedColor(R.color.primary);
                } else {
                    mPaletteColor = vibrantColor;
                }

                final ObjectAnimator mainInfoBlockColorFade = ObjectAnimator.ofObject(mainInfoBlock, "backgroundColor", new ArgbEvaluator(), getResources().getColor(R.color.primary), mPaletteColor);
                mainInfoBlockColorFade.setDuration(500);
                Drawable oldDrawable = PixelUtils.changeDrawableColor(MovieDetailActivity.this, R.drawable.ic_av_play_button, getResources().getColor(R.color.primary));
                mPlayButtonDrawable = PixelUtils.changeDrawableColor(MovieDetailActivity.this, R.drawable.ic_av_play_button, mPaletteColor);
                final TransitionDrawable td = new TransitionDrawable(new Drawable[]{oldDrawable, mPlayButtonDrawable});

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        playButton.setImageDrawable(td);
                        PopcornApplication.getPicasso().load(mItem.headerImage).into(coverImage, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                Animation fadeInAnim = AnimationUtils.loadAnimation(MovieDetailActivity.this, android.R.anim.fade_in);

                                mainInfoBlockColorFade.start();
                                td.startTransition(500);
                                coverImage.setVisibility(View.VISIBLE);
                                coverImage.startAnimation(fadeInAnim);
                            }

                            @Override
                            public void onError() {
                                headerProgress.setVisibility(View.GONE);
                            }
                        });
                    }
                });
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                headerProgress.setVisibility(View.GONE);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        });*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mQuality == null) {
            String[] keys = mItem.torrents.keySet().toArray(new String[mItem.torrents.size()]);
            onQualitySelected(keys[0]);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scrollView.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollListener);
    }

    @Override
    public void onQualitySelected(String quality) {
        mQuality = quality;
        qualityText.setText(mQuality);
    }

    @Override
    public void onSubtitleLanguageSelected(String language) {
        mSubLanguage = language;
        if (!language.equals("no-subs")) {
            Locale locale;
            if (language.contains("-")) {
                locale = new Locale(language.substring(0, 2), language.substring(3, 5));
            } else {
                locale = new Locale(language);
            }
            subtitlesText.setText(StringUtils.uppercaseFirst(locale.getDisplayName(locale)));
        } else {
            subtitlesText.setText(R.string.no_subs);
        }
    }
}