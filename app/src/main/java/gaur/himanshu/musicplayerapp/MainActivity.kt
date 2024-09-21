package gaur.himanshu.musicplayerapp

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import gaur.himanshu.musicplayerapp.ui.theme.MusicPlayerAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {


    var isBound: MutableState<Boolean> = mutableStateOf(false)
    var service: MusicPlayerService? = null

    var currentSong = MutableStateFlow<Track>(Track())

    private var currentDuration = MutableStateFlow<Float>(0f)
    private var maxDuration = MutableStateFlow<Float>(0f)
    private val isPlaying = MutableStateFlow<Boolean>(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, binder: IBinder) {
            isBound.value = true
            service = (binder as MusicPlayerService.MusicBinder).getService()

            binder.setMusicList(songs)

            lifecycleScope.launch {
                binder.getCurrentDuration().collectLatest {
                    currentDuration.value = it
                }
            }
            lifecycleScope.launch {
                binder.getMaxDuration().collectLatest {
                    maxDuration.value = it
                }
            }
            lifecycleScope.launch {
                binder.getCurrentSong().collectLatest {
                    currentSong.value = it
                }
            }
            lifecycleScope.launch {
                binder.isPlaying().collectLatest {
                    isPlaying.value = it
                }
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound.value = false
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerAppTheme {
                Scaffold(modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black),
                    topBar = {
                        TopAppBar(title = { Text(text = "Music Player App") }, actions = {
                            IconButton(onClick = {
                                val intent =
                                    Intent(this@MainActivity, MusicPlayerService::class.java)
                                startService(intent)
                                bindService(intent, connection, BIND_AUTO_CREATE)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                            }

                            IconButton(onClick = {
                                val intent =
                                    Intent(this@MainActivity, MusicPlayerService::class.java)
                                stopService(intent)
                                unbindService(connection)
                            }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            }
                        })
                    }) { innerPadding ->

                    val slider by currentDuration.collectAsState()
                    val maximum by maxDuration.collectAsState()
                    val currentSong by currentSong.collectAsState()
                    val isPlaying by isPlaying.collectAsState()

                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Image(
                            painter = painterResource(id = currentSong.image),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(horizontal = 32.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(32.dp)
                                ), contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            text = currentSong.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = Bold
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = slider.div(1000f).toInt().toString())
                            Spacer(modifier = Modifier.width(12.dp))
                            Slider(
                                value = slider, onValueChange = {

                                }, modifier = Modifier.weight(1f),
                                valueRange = 0f..maximum
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Text(text = maximum.div(1000f).toInt().toString())
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(
                                modifier = Modifier.size(60.dp),
                                onClick = { service?.playPrevSong() }) {
                                Icon(
                                    modifier = Modifier.size(60.dp),
                                    painter = painterResource(id = R.drawable.ic_prev),
                                    contentDescription = null
                                )
                            }
                            IconButton(modifier = Modifier.size(60.dp),
                                onClick = {
                                    service?.playPause()
                                }) {
                                Icon(
                                    modifier = Modifier.size(60.dp),
                                    painter = if (isPlaying)
                                        painterResource(id = R.drawable.ic_pause) else painterResource(
                                        id = R.drawable.ic_play
                                    ), contentDescription = null
                                )
                            }
                            IconButton(modifier = Modifier.size(60.dp),
                                onClick = { service?.playNextSong() }) {
                                Icon(
                                    modifier = Modifier.size(80.dp),
                                    painter = painterResource(id = R.drawable.ic_next),
                                    contentDescription = null
                                )
                            }
                        }

                    }
                }
            }
        }
    }

}