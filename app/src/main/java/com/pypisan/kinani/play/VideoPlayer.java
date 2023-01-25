package com.pypisan.kinani.play;

import static com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_ALWAYS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.viewpager.widget.ViewPager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.pypisan.kinani.R;
import com.pypisan.kinani.api.RequestModule;
import com.pypisan.kinani.api.WatchRequest;
import com.pypisan.kinani.model.EpisodeVideoModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class VideoPlayer extends AppCompatActivity implements SessionAvailabilityListener {

    TextView animeTitleView, summaryTextView;
    StyledPlayerView playerView;
    String videoLink;
    boolean isFullScreen = false;
    ExoPlayer player;
    String jTitle;
    String episode_num;
    String server_name = "server2";
    Uri hlsUri;
    CastContext mCastContext;
    MediaRouteButton mMediaRouteButton;
    ImageButton fullscreen, nextButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

//        for getting video summary params
        Intent videoIntent = getIntent();

        String title = videoIntent.getStringExtra("title");
        String summary = videoIntent.getStringExtra("summary");
        episode_num = videoIntent.getStringExtra("episode_num");
        jTitle = videoIntent.getStringExtra("jTitle");


        animeTitleView = findViewById(R.id.animeTitle);
        summaryTextView = findViewById(R.id.summaryText);
        playerView = findViewById(R.id.video_view);
        fullscreen = findViewById(R.id.fullScreen);


//        playerView.setShowNextButton(false);
//        playerView.setShowPreviousButton(false);
        playerView.setShowBuffering(SHOW_BUFFERING_ALWAYS);
//        playerView.setFullscreenButtonClickListener(new StyledPlayerView.FullscreenButtonClickListener() {
//            @Override
//            public void onFullscreenButtonClick(boolean isFullScreen) {
//                Toast.makeText(getApplicationContext(), "fullscreen clicked", Toast.LENGTH_SHORT).show();
//                changeOrientation(isFullScreen);
//            }
//        });
        fullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFullScreen = checkOrientation();
                Toast.makeText(getApplicationContext(), "fullscreen clicked", Toast.LENGTH_SHORT).show();
                changeOrientation(isFullScreen);
            }
        });
        animeTitleView.setText(title);
        summaryTextView.setText(summary);
        getEpisodeLink();

//        for casting video
        mCastContext = CastContext.getSharedInstance(this);
        mMediaRouteButton = (MediaRouteButton) findViewById(R.id.cast);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), mMediaRouteButton);

        if(mCastContext.getCastState() != CastState.NO_DEVICES_AVAILABLE)
            mMediaRouteButton.setVisibility(View.VISIBLE);

        mCastContext.addCastStateListener(new CastStateListener() {
            @Override
            public void onCastStateChanged(int state) {
                if (state == CastState.NO_DEVICES_AVAILABLE)
                    mMediaRouteButton.setVisibility(View.GONE);
                else {
                    if (mMediaRouteButton.getVisibility() == View.GONE)
                        mMediaRouteButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.stop();
        player.release();
        changeOrientation(false);
    }
    //    Check Orientation
//    returns true if in portrait mode

    public boolean checkOrientation() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation != Configuration.ORIENTATION_LANDSCAPE;
    }

//      Change Orientation

    public void changeOrientation(boolean shouldLandscape) {
        if (shouldLandscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void getEpisodeLink() {

        //      fetching data
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://anime.pypisan.com/v1/anime/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        RequestModule episodeLink = retrofit.create(RequestModule.class);
        Log.d("V1", "status is " + jTitle + episode_num);
        Call<EpisodeVideoModel> call = episodeLink.getEpisodeVideo(new WatchRequest(jTitle, episode_num, server_name));
        call.enqueue(new Callback<EpisodeVideoModel>() {
            @Override
            public void onResponse(Call<EpisodeVideoModel> call, Response<EpisodeVideoModel> response) {
                boolean flag = false;
                EpisodeVideoModel resource = response.body();
                if (response.code() == 200) {
                    boolean status = resource.getSuccess();
                    Log.d("V2", "status is " + status + resource.getValue());
                    flag = status;
                }
                if (flag) {
                    videoLink = resource.getValue();
                }
                playerInit();
            }

            @Override
            public void onFailure(Call<EpisodeVideoModel> call, Throwable t) {
//                Toast.makeText(getContext(), "Video Not found", Toast.LENGTH_SHORT).show();
                Log.d("V2", "status is " + t.getMessage());
                playerInit();
            }
        });

    }

    public void playerInit() {

        if (videoLink == null || videoLink.equals("")) {
            hlsUri = Uri.parse("https://s15.openstream.io/hls/qvsbdoqwnhblgwsztqk2ap4elgudihn2ropil7rbtdzkp3mqx4sk54po4anq/master.m3u8");
            Toast.makeText(getApplicationContext(), "Video Not found", Toast.LENGTH_LONG).show();
        } else {
            hlsUri = Uri.parse(videoLink);
            Toast.makeText(getApplicationContext(), "Video found", Toast.LENGTH_SHORT).show();
        }

        DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        // Create a HLS media source pointing to a playlist uri.
        HlsMediaSource hlsMediaSource =
                new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(hlsUri));

//        MediaItem hlsMediaSource = MediaItem.fromUri(hlsUri);

        // Create a player instance.
        player = new ExoPlayer.Builder(getApplicationContext()).build();
        // Set the media source to be played.
        player.setMediaSource(hlsMediaSource);
        // Prepare the player.
        player.prepare();
        playerView.setPlayer(player);

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) playerView.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            playerView.setLayoutParams(layoutParams);
        }else{
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            getWindow().clearFlags(View.KEEP_SCREEN_ON);

            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) playerView.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = (int) 255*3;
            playerView.setLayoutParams(layoutParams);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        changeOrientation(false);
        player.stop();
        player.release();

    }

    @Override
    public void onCastSessionAvailable() {
    }

    @Override
    public void onCastSessionUnavailable() {
    }
}
