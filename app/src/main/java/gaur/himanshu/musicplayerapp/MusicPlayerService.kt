package gaur.himanshu.musicplayerapp

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val PREV = "play"
const val NEXT = "next"
const val PLAY_PAUSE = "play_pause"

class MusicPlayerService : Service() {

    private val binder = MusicBinder()

    private var musicList = listOf<Track>()

    private var mediaPlayer: MediaPlayer = MediaPlayer()

    private var currentSong = MutableStateFlow<Track>(Track())
    private var maxDuration = MutableStateFlow<Float>(0f)
    private val currentDuration = MutableStateFlow<Float>(0f)
    private val isPlaying = MutableStateFlow<Boolean>(false)

    private val scope = CoroutineScope(Dispatchers.Main)
    private var job : Job?=null

    inner class MusicBinder : Binder() {
        fun getService() = this@MusicPlayerService

        fun setMusicList(list: List<Track>) {
            this@MusicPlayerService.musicList = list
        }

        fun getCurrentSong() = currentSong

        fun getMaxDuration() = maxDuration

        fun getCurrentDuration() = currentDuration

        fun isPlaying() = isPlaying

    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (intent.action) {
                PREV -> {
                    playPrevSong()
                }

                PLAY_PAUSE -> {
                    playPause()
                }

                NEXT -> {
                    playNextSong()
                }

                else -> {
                    currentSong.update { songs.get(0) }
                    play(currentSong.value)
                }
            }
        }
        return START_STICKY
    }

    fun playPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        } else {
            mediaPlayer.start()
        }
        sendNotification(currentSong.value)
    }

    private fun sendNotification(currentSong: Track) {
        isPlaying.update { mediaPlayer.isPlaying }
        val mediaSession = MediaSessionCompat(this, "music")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(currentSong.name)
            .setContentText(currentSong.desc)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.big_image))
            .addAction(R.drawable.ic_prev, "prev", createPrevPendingIntent())
            .addAction(
                getPlayPauseIcon(),
                "play",
                createPlayPausePendingIntent()
            )
            .addAction(R.drawable.ic_next, "next", createNextPendingIntent())
            .build()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startForeground(1, notification)
            }
        } else {
            startForeground(1, notification)
        }
    }

    private fun getRawUri(id: Int) = Uri.parse("android.resource://${packageName}/${id}")

    private fun updateDurations(player: MediaPlayer) {
       job =  scope.launch {
            try {
                if (player.isPlaying.not()) return@launch
                maxDuration.update { player.duration.toFloat() }
                while (true) {
                    currentDuration.update { player.currentPosition.toFloat() }
                    delay(1000)
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }

        }
    }

    private fun getPlayPauseIcon() =
        if (mediaPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play

    fun playNextSong() {
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        val currentSongIndex = musicList.indexOf(currentSong.value)
        val next = currentSongIndex.plus(1).mod(musicList.size)
        currentSong.update { musicList[next] }
        mediaPlayer.setOnCompletionListener { playNextSong() }
        play(currentSong.value)

    }

    fun playPrevSong() {
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()

        val index = musicList.indexOf(currentSong.value)
        val prev = if (index.minus(1) < 0) musicList.size.minus(1) else index.minus(1)
        currentSong.update { musicList[prev] }

        mediaPlayer.setOnCompletionListener { playNextSong() }
        play(currentSong.value)
    }

    private fun play(track: Track) {
        job?.cancel()

        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(this, getRawUri(track.id))
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            sendNotification(track)
            updateDurations(mediaPlayer)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    private fun createPrevPendingIntent(): PendingIntent? {
        val prevIntent = Intent(this, MusicPlayerService::class.java).apply {
            action = PREV
        }
        val prevPendingIntent =
            PendingIntent.getService(
                this,
                1,
                prevIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        return prevPendingIntent
    }

    private fun createNextPendingIntent(): PendingIntent? {
        val intent = Intent(this, MusicPlayerService::class.java).apply {
            action = NEXT
        }
        val pendingIntent =
            PendingIntent.getService(
                this,
                3,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        return pendingIntent
    }

    private fun createPlayPausePendingIntent(): PendingIntent? {
        val intent = Intent(this, MusicPlayerService::class.java).apply {
            action = PLAY_PAUSE
        }
        val pendingIntent =
            PendingIntent.getService(
                this,
                2,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        return pendingIntent
    }

}



