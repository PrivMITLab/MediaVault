package com.example.playback

import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@UnstableApi
class PlayerManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var exoPlayer: ExoPlayer? = null
    private var equalizer: Equalizer? = null

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerSecondsLeft = MutableStateFlow<Int?>(null)
    val sleepTimerSecondsLeft: StateFlow<Int?> = _sleepTimerSecondsLeft.asStateFlow()

    private var progressTrackingJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()
        val initialPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        attachPlayer(initialPlayer)
    }

    fun ensureServiceStarted() {
        val intent = Intent(context, MediaPlaybackService::class.java)
        try {
            context.startService(intent)
        } catch (_: Exception) {
            // Handled if already running or restricted
        }
    }

    fun attachPlayer(player: ExoPlayer) {
        this.exoPlayer = player
        setupEqualizer(player.audioSessionId)

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    stopProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: Media3Item?, reason: Int) {
                val index = player.currentMediaItemIndex
                _currentIndex.value = index
                if (index in _queue.value.indices) {
                    _currentMediaItem.value = _queue.value[index]
                }
            }
        })
    }

    fun detachPlayer() {
        equalizer?.release()
        equalizer = null
        exoPlayer = null
        stopProgressTracker()
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    private fun setupEqualizer(audioSessionId: Int) {
        runCatching {
            equalizer?.release()
            if (audioSessionId != 0) {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = true
                }
            }
        }
    }

    fun playTrack(item: MediaItem, fullQueue: List<MediaItem>) {
        ensureServiceStarted()
        val player = exoPlayer ?: return
        val queueItems = if (fullQueue.isEmpty()) listOf(item) else fullQueue
        _queue.value = queueItems

        val itemIndex = queueItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        _currentIndex.value = itemIndex
        _currentMediaItem.value = item

        player.clearMediaItems()
        val media3Items = queueItems.map { media ->
            val metadata = MediaMetadata.Builder()
                .setTitle(media.displayTitle)
                .setArtist(media.artist ?: "Private Vault")
                .setAlbumTitle(media.album ?: "")
                .build()

            Media3Item.Builder()
                .setUri(Uri.fromFile(File(media.filePath)))
                .setMediaId(media.id.toString())
                .setMediaMetadata(metadata)
                .build()
        }

        player.setMediaItems(media3Items, itemIndex, 0L)
        player.prepare()
        player.play()
        _isPlaying.value = true
        startProgressTracker()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0, 0L)
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun skipNext() {
        val player = exoPlayer ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        val player = exoPlayer ?: return
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun toggleRepeat() {
        val player = exoPlayer ?: return
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    fun toggleShuffle() {
        val player = exoPlayer ?: return
        val newShuffle = !player.shuffleModeEnabled
        player.shuffleModeEnabled = newShuffle
        _shuffleEnabled.value = newShuffle
    }

    fun setSpeed(speed: Float) {
        val player = exoPlayer ?: return
        player.playbackParameters = PlaybackParameters(speed)
        _playbackSpeed.value = speed
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        _sleepTimerSecondsLeft.value = minutes * 60

        sleepTimerJob = scope.launch {
            var seconds = minutes * 60
            while (seconds > 0 && isActive) {
                delay(1000)
                seconds--
                _sleepTimerSecondsLeft.value = seconds
            }
            // Timer expired: pause player
            exoPlayer?.pause()
            _sleepTimerMinutes.value = null
            _sleepTimerSecondsLeft.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = null
        _sleepTimerSecondsLeft.value = null
    }

    // Equalizer controls
    fun getEqualizerPresets(): List<String> {
        val eq = equalizer ?: return emptyList()
        val numPresets = eq.numberOfPresets.toInt()
        return (0 until numPresets).map { i ->
            runCatching { eq.getPresetName(i.toShort()) }.getOrDefault("Preset $i")
        }
    }

    fun setEqualizerPreset(presetIndex: Short) {
        runCatching {
            equalizer?.usePreset(presetIndex)
        }
    }

    fun getEqualizerNumberOfBands(): Short {
        return equalizer?.numberOfBands ?: 0
    }

    fun getBandCenterFreq(band: Short): Int {
        return equalizer?.getCenterFreq(band) ?: 0
    }

    fun getBandLevel(band: Short): Short {
        return equalizer?.getBandLevel(band) ?: 0
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        val range = equalizer?.bandLevelRange ?: return Pair(-1500, 1500)
        return Pair(range[0], range[1])
    }

    fun setBandLevel(band: Short, level: Short) {
        runCatching {
            equalizer?.setBandLevel(band, level)
        }
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { p ->
                    _currentPositionMs.value = p.currentPosition.coerceAtLeast(0L)
                    _durationMs.value = p.duration.coerceAtLeast(0L)
                }
                delay(400)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackingJob?.cancel()
    }

    companion object {
        @Volatile
        private var INSTANCE: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
