@file:Suppress("SpellCheckingInspection", "NewerVersionAvailable", "AndroidGradlePluginVersion")
package com.example.mymagic

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File

@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@OptIn(UnstableApi::class)
@Composable
fun VideoBackground(
    videoRawId: Int,
    localFileName: String = "",
    zoomLevel: Int = 1,
    spellId: String = "",
    isWallLoop: Boolean = false,
    isMutedByDefault: Boolean = false,
    refreshTrigger: Int = 0,
    onVideoEnd: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentOnVideoEnd by rememberUpdatedState(onVideoEnd)

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var playbackTrigger by remember { mutableStateOf("") }
    var isAppInForeground by remember { mutableStateOf(true) }

    var layoutView by remember { mutableStateOf<AspectRatioFrameLayout?>(null) }
    var isVideoReadyToDisplay by remember(spellId) { mutableStateOf(false) }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVideoReadyToDisplay) 1f else 0f,
        animationSpec = tween(durationMillis = 100), // Ускорили появление для бесшовности
        label = "VideoFadeIn"
    )

    LaunchedEffect(spellId) {
        playbackTrigger = spellId
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    isAppInForeground = false
                    exoPlayer.playWhenReady = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    isAppInForeground = true
                    exoPlayer.playWhenReady = true
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    LaunchedEffect(isMutedByDefault, spellId) {
        val prefs = MagicPrefsFactory.create(context)
        exoPlayer.volume = if (isMutedByDefault || (spellId.isNotEmpty() && prefs.getBoolean("mute_sound_$spellId", false))) 0f else 1f
    }

    LaunchedEffect(videoRawId, localFileName, spellId, isWallLoop, refreshTrigger) {
        try {
            val prefs = MagicPrefsFactory.create(context)
            val mediaDir = File(context.filesDir, "magic_media")
            var videoUri: Uri? = null
            var soundUri: Uri? = null
            val currentSpell = magicSpellRegistry.firstOrNull { it.id == spellId }

            if (localFileName.isNotEmpty()) {
                val localFile = File(mediaDir, localFileName)
                if (localFile.exists()) videoUri = Uri.fromFile(localFile)
            }

            if (videoUri == null && currentSpell != null && currentSpell.videoFileName.isNotEmpty()) {
                val rawVideoName = currentSpell.videoFileName.substringBeforeLast(".")
                val defVideoId = context.resources.getIdentifier(rawVideoName, "raw", context.packageName)
                if (defVideoId != 0) videoUri = "android.resource://${context.packageName}/$defVideoId".toUri()
            }

            if (videoUri == null && videoRawId != 0) videoUri = "android.resource://${context.packageName}/$videoRawId".toUri()

            val targetSoundSpellId = if (spellId == "wall_loop") "wall_break" else spellId
            if (targetSoundSpellId.isNotEmpty() && !prefs.getBoolean("mute_sound_$targetSoundSpellId", false)) {
                val customSoundName = prefs.getString("custom_sound_file_$targetSoundSpellId", "") ?: ""
                if (customSoundName != "none") {
                    if (customSoundName.isNotEmpty()) {
                        val localSoundFile = File(mediaDir, customSoundName)
                        if (localSoundFile.exists()) soundUri = Uri.fromFile(localSoundFile)
                    }
                    if (soundUri == null) {
                        val breakSpell = magicSpellRegistry.firstOrNull { it.id == "wall_break" }
                        val rawSoundName = currentSpell?.soundFileName?.substringBeforeLast(".") ?: breakSpell?.soundFileName?.substringBeforeLast(".") ?: ""
                        if (rawSoundName.isNotEmpty()) {
                            val defSoundId = context.resources.getIdentifier("${rawSoundName}_sound", "raw", context.packageName)
                            if (defSoundId != 0) soundUri = "android.resource://${context.packageName}/$defSoundId".toUri()
                        }
                    }
                }
            }
            val dataSourceFactory = DefaultDataSource.Factory(context)

            val finalMediaSource = if (videoUri != null && soundUri != null) {
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(videoUri))
                val soundSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(soundUri))

                MergingMediaSource(true, videoSource, soundSource)
            } else if (videoUri != null) {
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri))
            } else if (soundUri != null) {
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(soundUri))
            } else {
                null
            }

            exoPlayer.clearMediaItems()
            exoPlayer.repeatMode = if (isWallLoop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF

            if (finalMediaSource != null) {
                exoPlayer.setMediaSource(finalMediaSource)
                exoPlayer.prepare()
                if (isAppInForeground) {
                    exoPlayer.playWhenReady = true
                }
            } else {
                currentOnVideoEnd()
            }

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED && !isWallLoop) {
                        currentOnVideoEnd()
                    }
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        val ratio = videoSize.width.toFloat() / videoSize.height.toFloat()
                        layoutView?.setAspectRatio(ratio)
                        isVideoReadyToDisplay = true
                    }
                }
            })

        } catch (_: Exception) {
            currentOnVideoEnd()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Сделали фон прозрачным, чтобы видеть нижний слой
            .scale(if (zoomLevel == 2) 1.25f else 1.0f)
            .alpha(if (zoomLevel == 3) 0f else animatedAlpha)
    ) {
        AndroidView(
            factory = { ctx ->
                AspectRatioFrameLayout(ctx).apply {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    val playerView = PlayerView(ctx).apply {
                        useController = false
                        player = exoPlayer
                        // ЗАПРЕЩАЕМ ВСПЫШКУ: делаем шторку плеера полностью прозрачной
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                    addView(playerView)
                    layoutView = this
                }
            },
            update = { view ->
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.size(1.dp).alpha(0f).background(if (playbackTrigger.isNotEmpty()) Color.Transparent else Color.Transparent))
    }
}
