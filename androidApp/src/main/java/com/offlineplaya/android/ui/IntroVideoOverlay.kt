package com.offlineplaya.android.ui

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.offlineplaya.android.R
import kotlinx.coroutines.delay

/**
 * The launch intro: the Walkman animation clip played full-screen over the
 * app while it boots underneath. Strictly a flourish, so every exit path is
 * defensive — clip end, tap-to-skip, playback error, and a hard timeout all
 * land in [onFinished], which the host uses to remove the overlay.
 *
 * Implementation notes:
 *  - Its own short-lived [ExoPlayer] (released on dispose), NOT the session
 *    player — the intro must never appear in media controls or disturb the
 *    restored queue.
 *  - Muted and without audio attributes, so it cannot take audio focus or
 *    duck anything already playing.
 *  - Renders into a [TextureView] (not SurfaceView): the host fades the
 *    overlay out, and SurfaceView contents ignore view alpha.
 *  - The clip is square; it renders centered at full width on the clip's own
 *    background color ([IntroBackdrop]) so the letterboxing is seamless.
 */
@Composable
fun IntroVideoOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Tooling preview can't run a video decoder — show the backdrop only.
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.fillMaxSize().background(IntroBackdrop))
        return
    }

    val context = LocalContext.current
    var finished by remember { mutableStateOf(false) }
    val finish = {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(
                MediaItem.fromUri(
                    "android.resource://${context.packageName}/${R.raw.walkman_intro}"
                )
            )
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) finish()
            }

            override fun onPlayerError(error: PlaybackException) {
                finish()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Belt-and-braces: if neither ENDED nor an error ever arrives (broken
    // decoder, stuck buffering on some OEM build), the app must not stay
    // hidden behind the intro.
    LaunchedEffect(Unit) {
        delay(INTRO_TIMEOUT_MS)
        finish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IntroBackdrop)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = stringResource(R.string.intro_skip),
            ) { finish() },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also(player::setVideoTextureView)
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
    }
}

/** The clip's own background (sampled from its corner) — letterboxing blends into it. */
private val IntroBackdrop = Color(0xFF1E1F24)

/** Clip runs ~5.2 s; anything past this is a stall, not a slow start. */
private const val INTRO_TIMEOUT_MS = 9_000L

@Preview(showBackground = true)
@Composable
private fun IntroVideoOverlayPreview() {
    IntroVideoOverlay(onFinished = {})
}
