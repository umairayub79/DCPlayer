package codes.umair.dcvideoplayer.activities


import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import codes.umair.dcvideoplayer.OnSwipeTouchListener
import codes.umair.dcvideoplayer.R
import codes.umair.dcvideoplayer.TrackSelector.TrackSelectionDialog
import codes.umair.dcvideoplayer.Utils.Utils
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.RandomTrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.exo_player_control_view.*
import spencerstudios.com.jetdblib.JetDB
import umairayub.madialog.MaDialog


class PlayerActivity : AppCompatActivity(), OnClickListener {

    private lateinit var mPlayer: SimpleExoPlayer
    private lateinit var mPlayerView: PlayerView
    private lateinit var mUiImmersiveOptions: Any

    private var mLastPlaybackName: String? = null
    private var mPlaybackPosition = 0L
    private var mCurrentWindow = 0
    private val KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters"

    private val STATE_RESUME_WINDOW = "resumeWindow"
    private val STATE_RESUME_POSITION = "resumePosition"
    private val STATE_PLAYER_FULLSCREEN = "playerFullscreen"

    private var isShowingTrackSelectionDialog = false
    private var trackSelector: DefaultTrackSelector? = null
    private var trackSelectorParameters: DefaultTrackSelector.Parameters? = null
    private var lastSeenTrackGroupArray: TrackGroupArray? = null
    private var mControlsState = Utils.ControlsMode.FULLCONTORLS
    private var device_height = 0
    private var device_width = 0
    private var mAudioManager: AudioManager? = null
    private var mGestureType = Utils.GestureType.NoGesture
    private var mWasPlaying = false
    private var mCurrentBrightness = 50
    private var isLandscape: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        mUiImmersiveOptions = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        window.decorView.systemUiVisibility = mUiImmersiveOptions as Int

        mAudioManager = getSystemService(AUDIO_SERVICE) as AudioManager?


        val mDisplayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(mDisplayMetrics)
        device_height = mDisplayMetrics.heightPixels
        device_width = mDisplayMetrics.widthPixels

        val layout = window?.attributes
        layout?.screenBrightness = mCurrentBrightness.toFloat() / 100
        window?.attributes = layout

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        isLandscape = false

        mPlayerView = SimpleExoPlayerView(this)
        mPlayerView = findViewById<PlayerView>(R.id.mplayer)
        mPlayerView.requestFocus()


        mPlayerView.setOnTouchListener(clickFrameSwipeListener)
        mGestureType = Utils.GestureType.SwipeGesture

        mLastPlaybackName = JetDB.getString(this,"lastPlaybackName", null)
        btn_back.setOnClickListener(this)
        btn_unlock.setOnClickListener(this)
        btn_lock.setOnClickListener(this)
        btn_rotate.setOnClickListener(this)
        btn_audioTrack.setOnClickListener(this)
        btn_subtitleTrack.setOnClickListener(this)

        mPlayerView.setControllerVisibilityListener {
            if (mControlsState == Utils.ControlsMode.FULLCONTORLS) {
                root.visibility = it
            }

        }

        trackSelectorParameters = if(savedInstanceState != null){
            savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
        }else{
            ParametersBuilder().build()

        }

    }


    override fun onClick(v: View?) {
        when (v?.id) {
            btn_back.id -> {
                killPlayer()
                finish()
            }
            btn_lock.id -> {
                mControlsState = Utils.ControlsMode.LOCK
                root.visibility = View.GONE
                btn_unlock.visibility = View.VISIBLE
                btn_lock.visibility = View.GONE
                btn_rotate.visibility = View.GONE


            }
            btn_unlock.id -> {
                mControlsState = Utils.ControlsMode.FULLCONTORLS
                root.visibility = View.VISIBLE
                btn_lock.visibility = View.VISIBLE
                btn_unlock.visibility = View.GONE
                btn_rotate.visibility = View.VISIBLE

            }
            btn_rotate.id -> {
                if (!isLandscape){
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    isLandscape = true
                }else{
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    isLandscape = false
                }

            }
            btn_audioTrack.id -> {
                if(!isShowingTrackSelectionDialog && TrackSelectionDialog.willHaveContent(trackSelector)) {
                    isShowingTrackSelectionDialog = true
                    mPlayer.playWhenReady = false
                    val trackSelectionDialog = TrackSelectionDialog.createForTrackSelector(trackSelector,R.string.track_selection_title, C.TRACK_TYPE_AUDIO, DialogInterface.OnDismissListener {
                        isShowingTrackSelectionDialog = false
                        mPlayer.playWhenReady = true
                    })

                    trackSelectionDialog.show(supportFragmentManager,null)
                }
            }
            btn_subtitleTrack.id -> {
                if(!isShowingTrackSelectionDialog && TrackSelectionDialog.willHaveContent(trackSelector)) {
                    isShowingTrackSelectionDialog = true
                    mPlayer.playWhenReady = false
                    val trackSelectionDialog = TrackSelectionDialog.createForTrackSelector(trackSelector,R.string.sub_track_selection_title, C.TRACK_TYPE_TEXT, DialogInterface.OnDismissListener {
                        isShowingTrackSelectionDialog = false
                        mPlayer.playWhenReady = true
                    })

                    trackSelectionDialog.show(supportFragmentManager,null)
                }

            }


        }

    }

    private fun initExo(uri: Uri) {
        var trackSelectionFactory = RandomTrackSelection.Factory()
        val loadControl = DefaultLoadControl()
        trackSelector = DefaultTrackSelector(trackSelectionFactory)
        val userAgent = Util.getUserAgent(this, "DCVideoPlayer")
        val mediaSource = ExtractorMediaSource.Factory(DefaultDataSourceFactory(this, userAgent))
            .setExtractorsFactory(DefaultExtractorsFactory()).createMediaSource(
                uri
            )

        trackSelector?.parameters = trackSelectorParameters!!
        lastSeenTrackGroupArray = null;

        val rf = DefaultRenderersFactory(this,null, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)


        mPlayer = ExoPlayerFactory.newSimpleInstance(this, rf, trackSelector!!, loadControl)
        mPlayerView.player = mPlayer


        mPlayer.prepare(mediaSource, true, false)
        mPlayer.playWhenReady = true
        mPlayer.seekTo(mCurrentWindow, mPlaybackPosition)



        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MOVIE)
            .build()

        mPlayer.setAudioAttributes(audioAttributes, true)

        mPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if(playbackState == Player.STATE_ENDED){
                    killPlayer()
                    finish()
                }
            }
            override  fun onPlayerError(error: ExoPlaybackException) {
                MaDialog.Builder(this@PlayerActivity)
                    .setMessageTextColor(Color.WHITE)
                    .setButtonTextColor(Color.WHITE)
                    .setCustomFont(R.font.muli)
                    .setTitle("ERROR!")
                    .setMessage("Unable to play this video")
                    .setPositiveButtonText("OK")
                    .setPositiveButtonListener {
                        finish()
                    }
                    .build()
            }
            override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
            ) {
                toggleControls()
                if (trackGroups !== lastSeenTrackGroupArray) {
                    val mappedTrackInfo: MappingTrackSelector.MappedTrackInfo? =
                        trackSelector!!.currentMappedTrackInfo
                    if (mappedTrackInfo != null) {
                        if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            === MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS
                        ) {
                           MaDialog.Builder(this@PlayerActivity)
                               .setMessageTextColor(Color.WHITE)
                               .setButtonTextColor(Color.WHITE)
                               .setCustomFont(R.font.muli)
                               .setTitle("ERROR!")
                               .setMessage("Unsupported Video Format")
                               .setPositiveButtonText("OK")
                               .setPositiveButtonListener {  }
                               .build()

                        }
                        if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                            === MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS
                        ) {
                            MaDialog.Builder(this@PlayerActivity)
                                .setMessageTextColor(Color.WHITE)
                                .setButtonTextColor(Color.WHITE)
                                .setCustomFont(R.font.muli)
                                .setTitle("ERROR!")
                                .setMessage("Unsupported Audio Format")
                                .setPositiveButtonText("OK")
                                .setPositiveButtonListener {  }
                                .build()
                        }
                    }
                    lastSeenTrackGroupArray = trackGroups
                }
            }
        })
    }

    private fun updateTrackSelectorParameters() {
        if (trackSelector != null) {
            trackSelectorParameters = trackSelector!!.parameters
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateTrackSelectorParameters()
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters);
        outState.putInt(STATE_RESUME_WINDOW, mCurrentWindow)
        outState.putLong(STATE_RESUME_POSITION, mPlaybackPosition)
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, isLandscape)
        super.onSaveInstanceState(outState)
    }

    private fun toggleControls() {
        if (mControlsState === Utils.ControlsMode.FULLCONTORLS) {
            if (root.visibility === View.VISIBLE) {
                root.visibility = View.GONE
                unlock_panel.visibility = View.GONE
                mPlayerView.hideController()
                mUiImmersiveOptions = (View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

            }else if (root.visibility === View.GONE) {
                mUiImmersiveOptions = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                mPlayerView.showController()
                root.visibility = View.VISIBLE
                unlock_panel.visibility = View.VISIBLE
            }
        }

        if (mControlsState === Utils.ControlsMode.LOCK) {
            if (unlock_panel.visibility === View.VISIBLE) {
                unlock_panel.visibility = View.GONE
                mPlayerView.hideController()
                mUiImmersiveOptions = (View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

            }else if (unlock_panel.visibility === View.GONE) {
                unlock_panel.visibility = View.VISIBLE
                mPlayerView.showController()
                mUiImmersiveOptions = (View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            }
        }
        window.decorView.systemUiVisibility = mUiImmersiveOptions as Int
    }

    private fun isPlaying(): Boolean {
        return mPlayer.isPlaying
    }

    private fun killPlayer() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlaybackPosition = mPlayer.currentPosition;
            mCurrentWindow = mPlayer.currentWindowIndex;
            mPlayerView.player = null;
            mPlayer.release();
        }
    }


    override fun onBackPressed() {
        if (mControlsState === Utils.ControlsMode.LOCK) {

        }else{
            super.onBackPressed()
            killPlayer()
        }

    }

    override fun onStart() {
        super.onStart()
        if (intent?.type?.startsWith("video/") == true){
            val data = intent.data!!
            txt_title.text = Utils().getFileName(this,data)
            if (mLastPlaybackName != null && mLastPlaybackName == txt_title.text.toString()){
                mPlaybackPosition = JetDB.getLong(this,"lastPlaybackPosition",0).toLong()
            }
            initExo(data)
        }else{
            val mVideoPath = intent.getStringExtra("videoPath")
            txt_title.text = intent.getStringExtra("videoName")
            if (mLastPlaybackName != null && mLastPlaybackName == txt_title.text.toString()){
                mPlaybackPosition = JetDB.getLong(this,"lastPlaybackPosition",0).toLong()
            }
            initExo(Uri.parse(mVideoPath))
        }


    }

    override fun onResume() {
        super.onResume()
        if (mPlaybackPosition != 0L && mPlayer != null) {
            mPlayer.seekTo(mCurrentWindow, mPlaybackPosition)
        }
    }

    override fun onStop() {
        super.onStop()
        if (Utils().getDurationString(mPlayer.currentPosition,false) != Utils().getDurationString(mPlayer.duration,false)){
            JetDB.putLong(this, mPlayer.currentPosition,"lastPlaybackPosition")
        }else{
            JetDB.putLong(this, 0L,"lastPlaybackPosition")
        }
        JetDB.putString(this, txt_title.text.toString(),"lastPlaybackName")

    }

    override fun onPause() {
        super.onPause()
        killPlayer()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        mPlayerView.showController()
        return super.dispatchKeyEvent(event) || mPlayerView.dispatchMediaKeyEvent(event)
    }


    private var clickFrameSwipeListener = object : OnSwipeTouchListener(true) {

        var diffTime = -1f
        var finalTime = -1f
        var startVolume: Int = 0
        var maxVolume: Int = 0
        var maxBrightness: Int = 0
        var finalBrightness: Int = 0


        override fun onMove(dir: Direction, diff: Float) {
            // If swipe is not enabled, move should not be evaluated.
            if (mGestureType != Utils.GestureType.SwipeGesture)
                return

            if (mControlsState === Utils.ControlsMode.FULLCONTORLS) {
                if (dir == Direction.LEFT || dir == Direction.RIGHT) {

                    mPlayer.let { it ->

                        diffTime = if (it.duration <= 60) {
                            it.duration.toFloat() * diff / device_width.toFloat()
                        } else {
                            60000.toFloat() * diff / device_width.toFloat()
                        }
                        if (dir == Direction.LEFT) {
                            diffTime *= -1f
                        }
                        finalTime = it.currentPosition + diffTime
                        if (finalTime < 0) {
                            finalTime = 0f
                        } else if (finalTime > it.duration) {
                            finalTime = it.duration.toFloat()
                        }
                        diffTime = finalTime - it.currentPosition

                        val progressText = Utils().getDurationString(finalTime.toLong(), false) +
                                " [" + (if (dir == Direction.LEFT) "-" else "+") +
                                Utils().getDurationString(Math.abs(diffTime).toLong(), false) +
                                "]"

                        mPlayerView.showController()
                        root.visibility = View.VISIBLE
                        play_controls.visibility = View.GONE
                        unlock_panel.visibility = View.GONE
                        position_textview.text = progressText

                    }

                } else {

                    finalTime = -1f
                    if (initialX >= device_width / 2 || window == null) {

                        var diffVolume: Float
                        var finalVolume: Int

                        diffVolume = maxVolume.toFloat() * diff / (device_height.toFloat() / 2)
                        if (dir == Direction.DOWN) {
                            diffVolume = -diffVolume
                        }
                        finalVolume = startVolume + diffVolume.toInt()
                        if (finalVolume < 0)
                            finalVolume = 0
                        else if (finalVolume > maxVolume)
                            finalVolume = maxVolume

                        val progressText = "Volume: $finalVolume"
                        position_textview.text = progressText

                        mPlayerView.showController()
                        root.visibility = View.VISIBLE
                        play_controls.visibility = View.GONE
                        unlock_panel.visibility = View.GONE
                        mAudioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, finalVolume, 0)

                    } else if (initialX < device_width / 2) {

                        var diffBrightness: Float

                        diffBrightness =
                            maxBrightness.toFloat() * diff / (device_height.toFloat() / 2)
                        if (dir == Direction.DOWN) {
                            diffBrightness = -diffBrightness
                        }
                        finalBrightness = mCurrentBrightness + diffBrightness.toInt()
                        if (finalBrightness < 0)
                            finalBrightness = 0
                        else if (finalBrightness > maxBrightness)
                            finalBrightness = maxBrightness

                        val progressText = "Brightness: $finalBrightness"
                        position_textview.text = progressText

                        mPlayerView.showController()
                        root.visibility = View.VISIBLE
                        play_controls.visibility = View.GONE
                        unlock_panel.visibility = View.GONE

                        val layout = window?.attributes
                        layout?.screenBrightness = finalBrightness.toFloat() / 100
                        window?.attributes = layout


                    }
                }

            }
        }

        override fun onClick() {
            toggleControls()
        }

        override fun onDoubleTap(event: MotionEvent) {
            if (mControlsState === Utils.ControlsMode.FULLCONTORLS) {
                mPlayer.playWhenReady = !mPlayer.playWhenReady
            }
        }

        override fun onAfterMove() {
            if (mControlsState === Utils.ControlsMode.FULLCONTORLS) {
                if (finalTime >= 0 && mGestureType == Utils.GestureType.SwipeGesture) {
                    mPlayer.seekTo(finalTime.toLong())
                    if (mWasPlaying) mPlayer.playWhenReady = true
                }

                position_textview.visibility = View.GONE
                root.visibility = View.GONE
                play_controls.visibility = View.VISIBLE


            }
        }

        override fun onBeforeMove(dir: Direction) {
            if (mControlsState === Utils.ControlsMode.FULLCONTORLS) {
                if (mGestureType != Utils.GestureType.SwipeGesture)
                    return
                if (dir == Direction.LEFT || dir == Direction.RIGHT) {
                    mWasPlaying = isPlaying()
                    mPlayer?.playWhenReady = false

                    play_controls.visibility = View.GONE
                    unlock_panel.visibility = View.GONE
                    position_textview.visibility = View.VISIBLE
                } else {
                    maxBrightness = 100
                    if(finalBrightness > 0){
                        mCurrentBrightness = finalBrightness
                    }

                    maxVolume = mAudioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
                    startVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 100

                    play_controls.visibility = View.GONE
                    unlock_panel.visibility = View.GONE
                    position_textview.visibility = View.VISIBLE


                }
            }
        }

    }

}

