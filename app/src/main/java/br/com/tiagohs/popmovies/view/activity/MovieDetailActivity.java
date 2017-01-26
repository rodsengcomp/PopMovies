package br.com.tiagohs.popmovies.view.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.Inflater;

import javax.inject.Inject;

import br.com.tiagohs.popmovies.BuildConfig;
import br.com.tiagohs.popmovies.R;
import br.com.tiagohs.popmovies.model.atwork.Artwork;
import br.com.tiagohs.popmovies.model.db.MovieDB;
import br.com.tiagohs.popmovies.model.dto.ImageDTO;
import br.com.tiagohs.popmovies.model.dto.ItemListDTO;
import br.com.tiagohs.popmovies.model.dto.ListActivityDTO;
import br.com.tiagohs.popmovies.model.media.Video;
import br.com.tiagohs.popmovies.model.movie.MovieDetails;
import br.com.tiagohs.popmovies.model.response.VideosResponse;
import br.com.tiagohs.popmovies.presenter.MovieDetailsPresenter;
import br.com.tiagohs.popmovies.util.AnimationsUtils;
import br.com.tiagohs.popmovies.util.ImageUtils;
import br.com.tiagohs.popmovies.util.MovieUtils;
import br.com.tiagohs.popmovies.util.PermissionUtils;
import br.com.tiagohs.popmovies.util.ViewUtils;
import br.com.tiagohs.popmovies.util.enumerations.ImageSize;
import br.com.tiagohs.popmovies.util.enumerations.ItemType;
import br.com.tiagohs.popmovies.util.enumerations.ListType;
import br.com.tiagohs.popmovies.util.enumerations.Sort;
import br.com.tiagohs.popmovies.util.enumerations.TypeShowImage;
import br.com.tiagohs.popmovies.view.AppBarMovieListener;
import br.com.tiagohs.popmovies.view.EllipsizingTextView;
import br.com.tiagohs.popmovies.view.MovieDetailsView;
import br.com.tiagohs.popmovies.view.adapters.ListWordsAdapter;
import br.com.tiagohs.popmovies.view.callbacks.ImagesCallbacks;
import br.com.tiagohs.popmovies.view.callbacks.ListMoviesCallbacks;
import br.com.tiagohs.popmovies.view.callbacks.ListWordsCallbacks;
import br.com.tiagohs.popmovies.view.callbacks.MovieDontWantSeeCallback;
import br.com.tiagohs.popmovies.view.callbacks.MovieFavoriteCallback;
import br.com.tiagohs.popmovies.view.callbacks.MovieVideosCallbacks;
import br.com.tiagohs.popmovies.view.callbacks.MovieWantSeeCallback;
import br.com.tiagohs.popmovies.view.callbacks.MovieWatchedCallback;
import br.com.tiagohs.popmovies.view.callbacks.PersonCallbacks;
import br.com.tiagohs.popmovies.view.callbacks.ReviewCallbacks;
import br.com.tiagohs.popmovies.view.fragment.MovieDetailsFragment;
import butterknife.BindView;
import butterknife.OnClick;

public class MovieDetailActivity extends BaseActivity implements MovieDetailsView,
        MovieVideosCallbacks, ImagesCallbacks,
        PersonCallbacks, ReviewCallbacks, MovieFavoriteCallback,
        ListMoviesCallbacks, ListWordsCallbacks, MovieWantSeeCallback, MovieDontWantSeeCallback {
    private static final String TAG = MovieDetailActivity.class.getSimpleName();

    private static final int REQ_START_STANDALONE_PLAYER = 1;
    private static final int REQ_RESOLVE_SERVICE_MISSING = 2;
    private static final int RECYCLER_VIEW_ORIENTATION = LinearLayoutManager.HORIZONTAL;

    private static final String MOVIE = "movie";
    private static final String START = "start";
    private static final String EXTRA_MOVIE_ID = "br.com.tiagohs.popmovies.movie";

    @BindView(R.id.movie_detail_app_bar)          AppBarLayout mAppBarLayout;
    @BindView(R.id.poster_movie)                  ImageView mPosterMovie;
    @BindView(R.id.background_movie)              ImageView mBackgroundMovie;
    @BindView(R.id.title_movie)                   TextView mTitleMovie;
    @BindView(R.id.ano_lancamento_movie)          TextView mAnoLancamento;
    @BindView(R.id.duracao_movie)                 TextView mDuracao;
    @BindView(R.id.diretores_recycler_view)       RecyclerView mDiretoresRecyclerView;
    @BindView(R.id.play_image_movie_principal)    ImageView playButtonImageView;
    @BindView(R.id.movies_btn_ja_assisti)         FloatingActionButton mJaAssistiButton;
    @BindView(R.id.progress_movies_details)       ProgressBar mProgressMovieDetails;
    @BindView(R.id.movie_details_fragment)        LinearLayout mContainerTabs;
    @BindView(R.id.share_progress)                ProgressWheel mProgressShare;
    @BindView(R.id.duracao_movie_container)       LinearLayout mDuracaoMovieContainer;

    @Inject MovieDetailsPresenter mPresenter;

    private int mMovieID;
    private boolean mIsWatchPressed;
    private MovieDetails mMovie;
    private ListWordsAdapter mDiretoresAdapter;
    private boolean isStarted;
    private Bitmap mImageToShare;
    private MovieWatchedCallback mCallback;
    private LinearLayout mMovieShareContainer;

    private boolean mIsNewMovie = false;

    public static Intent newIntent(Context context, int movieID) {
        Intent intent = new Intent(context, MovieDetailActivity.class);
        intent.putExtra(EXTRA_MOVIE_ID, movieID);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getApplicationComponent().inject(this);

        mPresenter.setView(this);
        mPresenter.setContext(this);
        mJaAssistiButton.hide();

    }

    public void setCallback(MovieWatchedCallback fragment) {
        mCallback = fragment;
    }

    @Override
    protected int getActivityBaseViewID() {
        return R.layout.activity_movie_detail;
    }

    @Override
    protected int getMenuLayoutID() {
        return R.menu.menu_detail_default;
    }

    @Override
    protected void onStart() {
        super.onStart();

        mMovieID = (int) getIntent().getSerializableExtra(EXTRA_MOVIE_ID);
        mPresenter.getMovieDetails(mMovieID, TAG);

    }

    @Override
    protected void onStop() {
        super.onStop();

//        if (mPresenter != null)
//            mPresenter.onCancellRequest(this, TAG);
    }

    public void setJaAssistido() {
        mIsWatchPressed = true;
        mJaAssistiButton.setImageDrawable(ViewUtils.getDrawableFromResource(this, R.drawable.ic_assistido));
        mJaAssistiButton.setBackgroundTintList(ColorStateList.valueOf(ViewUtils.getColorFromResource(this, android.R.color.holo_green_dark)));
    }

    @OnClick(R.id.movies_btn_ja_assisti)
    public void onClickJaAssisti() {
        mIsWatchPressed = !mIsWatchPressed;

        mPresenter.onClickJaAssisti(mMovie, mIsWatchPressed);

        if (mIsWatchPressed) {
            updateState(R.drawable.ic_assistido, android.R.color.holo_green_dark);
            mCallback.onWatchedPressed();
            ViewUtils.createToastMessage(this, getString(R.string.marked_watched));
        } else {
            updateState(R.drawable.ic_assitir_eye, R.color.yellow);
            ViewUtils.createToastMessage(this, getString(R.string.desmarked_watched));
        }
    }

    private void updateState(int iconID, int iconColor) {
        mJaAssistiButton.setImageDrawable(ViewUtils.getDrawableFromResource(this, iconID));
        mJaAssistiButton.setBackgroundTintList(ColorStateList.valueOf(ViewUtils.getColorFromResource(this, iconColor)));
    }

    @Override
    public void onWantSeePressed() {
        if (mIsWatchPressed) {
            mIsWatchPressed = false;
            updateState(R.drawable.ic_assitir_eye, R.color.yellow);
        }

    }

    @Override
    public void onFavoritePressed() {
        if (!mIsWatchPressed) {
            mIsWatchPressed = true;
            updateState(R.drawable.ic_assistido, android.R.color.holo_green_dark);
        }
    }

    public void setupDirectorsRecyclerView(List<ItemListDTO> directors) {
        mDiretoresRecyclerView.setLayoutManager(new LinearLayoutManager(this, RECYCLER_VIEW_ORIENTATION, false));
        mDiretoresAdapter = new ListWordsAdapter(this, directors, this, ItemType.DIRECTORS, R.layout.item_list_words_default);
        mDiretoresRecyclerView.setAdapter(mDiretoresAdapter);
    }

    public void updateUI(MovieDetails movie) {
        isStarted = true;
        this.mMovie = movie;
        mJaAssistiButton.show();

        if (mMovie.getBackdropPath() != null)
            ImageUtils.loadWithRevealAnimation(MovieDetailActivity.this, mMovie.getBackdropPath(), mBackgroundMovie, R.drawable.ic_image_default_back, ImageSize.BACKDROP_780);
        else {
            ImageUtils.loadWithBlur(this, R.drawable.background_image, mBackgroundMovie);
        }

        ImageUtils.load(this, mMovie.getPosterPath(), mPosterMovie, mMovie.getTitle(), ImageSize.POSTER_185);

        mTitleMovie.setText(mMovie.getTitle());
        mDuracao.setText(getResources().getString(R.string.movie_duracao, mMovie.getRuntime()));
        mAnoLancamento.setText(String.valueOf(mMovie.getYearRelease()));


    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        
    }

    public void setupTabs() {

        if (!isDestroyed())
            startFragment(R.id.movie_details_fragment, MovieDetailsFragment.newInstance(mMovie));

        mAppBarLayout.addOnOffsetChangedListener(onOffsetChangedListener());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(MOVIE, mMovie);
        outState.putBoolean(START, isStarted);
    }

    private AppBarMovieListener onOffsetChangedListener() {
        return new AppBarMovieListener() {

            @Override
            public void onExpanded(AppBarLayout appBarLayout) {
                mToolbar.setBackground(ViewUtils.getDrawableFromResource(getApplicationContext(), R.drawable.background_action_bar_transparent));
                mToolbar.setTitle("");
                mJaAssistiButton.show();
            }

            @Override
            public void onCollapsed(AppBarLayout appBarLayout) {
                mToolbar.setBackgroundColor(ViewUtils.getColorFromResource(getApplicationContext(), R.color.colorPrimary));
                mToolbar.setTitle(mMovie.getTitle());
                mJaAssistiButton.hide();
            }

            @Override
            public void onIdle(AppBarLayout appBarLayout) {
                mToolbar.setBackground(ViewUtils.getDrawableFromResource(getApplicationContext(), R.drawable.background_action_bar_transparent));
            }
        };
    }

    @OnClick({R.id.play_image_movie_principal, R.id.background_movie})
    public void onClickBackgroundMovie() {
        if (null != mMovie) {
            if (isInternetConnected() && null != mMovie.getVideos()) {
                if (!mMovie.getVideos().isEmpty())
                    inflateVideoPlayer(mMovie.getVideos().get(0).getKey());
            }
        }

    }

    public void updateVideos(VideosResponse videos) {
        mMovie.setVideos(videos);
    }

    @Override
    public void onClickVideo(Video video) {
        inflateVideoPlayer(video.getKey());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) return;

        switch (requestCode) {
            case REQ_START_STANDALONE_PLAYER:
                YouTubeInitializationResult errorReason =
                        YouTubeStandalonePlayer.getReturnedInitializationResult(data);
                if (errorReason.isUserRecoverableError())
                    errorReason.getErrorDialog(this, 0).show();
                break;
        }

    }

    private void inflateVideoPlayer(String videoKey) {

        int startTimeMillis = 0;
        boolean autoplay = true;
        boolean lightboxMode = false;

        Intent intent = YouTubeStandalonePlayer.createVideoIntent(
                this, BuildConfig.GOOGLE_KEY, videoKey, startTimeMillis, autoplay, lightboxMode);

        if (intent != null) {
            if (isAuthResolveIntent(intent)) {
                startActivityForResult(intent, REQ_START_STANDALONE_PLAYER);
            } else {
                YouTubeInitializationResult.SERVICE_MISSING
                        .getErrorDialog(this, REQ_RESOLVE_SERVICE_MISSING).show();
            }
        }
    }

    private boolean isAuthResolveIntent(Intent intent) {
        List<ResolveInfo> resolveInfo = this.getPackageManager().queryIntentActivities(intent, 0);
        return resolveInfo != null && !resolveInfo.isEmpty();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onMovieSelected(int movieID, ImageView posterMovie) {
        ViewUtils.startMovieActivityWithTranslation(this, movieID, posterMovie, getString(R.string.poster_movie));
    }

    @Override
    public void onClickPerson(int castID) {
        startActivity(PersonDetailActivity.newIntent(this, castID));
    }

    @Override
    public void onItemSelected(ItemListDTO item, ItemType itemType) {

        switch (itemType) {
            case GENRE:
                startActivity(ListsDefaultActivity.newIntent(this, new ListActivityDTO(item.getItemID(), item.getNameItem(), Sort.GENEROS, R.layout.item_list_movies, ListType.MOVIES), new HashMap<String, String>()));
                break;
            case KEYWORD:
                startActivity(ListsDefaultActivity.newIntent(this, new ListActivityDTO(item.getItemID(), getString(R.string.keyword_name), item.getNameItem(), Sort.KEYWORDS, R.layout.item_list_movies, ListType.MOVIES), new HashMap<String, String>()));
                break;
            case DIRECTORS:
                onClickPerson(item.getItemID());
                break;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_share:
                shareMovie();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void shareMovie() {
        mProgressShare.setVisibility(View.VISIBLE);

        if (isInternetConnected() && mMovie != null) {
            final View view = getLayoutInflater().inflate(R.layout.share_movie_details, null);
            ImageView posterMovie = (ImageView) view.findViewById(R.id.movie_poster);
            ImageView backgroundMovie = (ImageView) view.findViewById(R.id.movie_background);
            TextView titleMovie = (TextView) view.findViewById(R.id.movie_title);
            TextView yearMovie = (TextView) view.findViewById(R.id.movie_year);
            EllipsizingTextView sinopseMovie = (EllipsizingTextView) view.findViewById(R.id.movie_sinopse);

            ImageUtils.load(this, mMovie.getPosterPath(), posterMovie, mMovie.getTitle(), ImageSize.POSTER_185);
            ImageUtils.loadWithBlur(this, mMovie.getBackdropPath(), backgroundMovie, ImageSize.BACKDROP_300);
            titleMovie.setText(mMovie.getTitle());
            titleMovie.setTypeface(Typeface.createFromAsset(getAssets(), "openSansBold.ttf"));
            yearMovie.setText(String.valueOf(mMovie.getYearRelease()));
            yearMovie.setTypeface(Typeface.createFromAsset(getAssets(), "opensans.ttf"));
            sinopseMovie.setText(mMovie.getOverview());
            sinopseMovie.setTypeface(Typeface.createFromAsset(getAssets(), "opensans.ttf"));

            mMovieShareContainer = (LinearLayout) view.findViewById(R.id.share_movie_container);
            new Handler().postDelayed(new Runnable() {
                public void run() {

                    if (!isDestroyed()) {
                        mProgressShare.setVisibility(View.GONE);

                        mImageToShare = ViewUtils.getBitmapFromView(mMovieShareContainer);

                        if (PermissionUtils.validatePermission(MovieDetailActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, getString(R.string.permission_error_write_content)))
                            createShareIntent(mImageToShare);
                    }

                    mProgressShare.setVisibility(View.GONE);
                }
            }, 4000);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (PermissionUtils.onRequestPermissionsResultValidate(grantResults, requestCode))
            createShareIntent(mImageToShare);

    }

    public void createShareIntent(Bitmap imageToShare) {
        ImageUtils.fixMediaDir();
        String pathofBmp = MediaStore.Images.Media.insertImage(getContentResolver(), imageToShare, mMovie.getTitle() , null);

        Uri imageUri = Uri.parse(pathofBmp);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        if (mMovie.getImdbID() != null)
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.movie_imdb, mMovie.getImdbID()));

        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.setType("image/jpeg");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "send"));
        mProgressShare.setVisibility(View.GONE);
    }

    @OnClick(R.id.poster_movie)
    public void onClickPosterMovie() {
        if (mMovie != null) {
            if (!mMovie.getImages().isEmpty())
                onClickImage(getImageDTO(mMovie.getImages().size()), new ImageDTO(mMovie.getId(), null, mMovie.getPosterPath()));
        }

    }

    private List<ImageDTO> getImageDTO(int numImages) {
        List<ImageDTO> imageDTOs = new ArrayList<>();

        for (int cont = 0; cont < numImages; cont++) {
            Artwork image = mMovie.getImages().get(cont);
            imageDTOs.add(new ImageDTO(mMovie.getId(), image.getId(), image.getFilePath()));
        }

        return imageDTOs;

    }


    @Override
    public void onClickImage(List<ImageDTO> imagens,  ImageDTO imageDTO) {
        startActivity(WallpapersDetailActivity.newIntent(this, imagens, imageDTO, getString(R.string.wallpapers_title), mMovie.getTitle(), TypeShowImage.WALLPAPER_IMAGES));
    }

    @Override
    public void setProgressVisibility(int visibityState) {
        mProgressMovieDetails.setVisibility(visibityState);
    }

    @Override
    public boolean isAdded() {
        return this != null;
    }

    @Override
    public void setPlayButtonVisibility(int visibilityState) {
        playButtonImageView.setVisibility(visibilityState);
    }

    public void setDuracaoMovieVisibility(int visibility) {
        mDuracaoMovieContainer.setVisibility(visibility);
    }

    @Override
    public void setTabsVisibility(int visibilityState) {

        if (visibilityState == View.VISIBLE) {
            mContainerTabs.setAnimation(AnimationsUtils.createFadeInAnimation(1000));
            mContainerTabs.setVisibility(visibilityState);
        }

    }

    @Override
    protected View.OnClickListener onSnackbarClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.getMovieDetails(mMovieID, TAG);
                mSnackbar.dismiss();
            }
        };
    }


    @Override
    public void onClickReviewLink(String url) {
        startActivityForResult(WebViewActivity.newIntent(this, url, mMovie.getTitle()), 0);
    }

    @Override
    public void onClickReview(String reviewID) {

    }

    @Override
    public void onDontWantSeePressed() {

    }
}
