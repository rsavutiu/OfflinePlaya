package com.offlineplaya.shared.domain.stream

/**
 * Where the desktop receiver and its setup instructions live. Shared by the
 * Play-to-PC page and the feature-discovery nudge so there is one URL to keep
 * current.
 *
 * Two forms on purpose: the receiver has to be installed *on the computer*, so
 * the link usually has to travel off the phone. [GUIDE] is what a tap opens (it
 * carries the README anchor straight to the Stream-to-PC section); [GUIDE_SHORT]
 * is the one we print on screen, because an anchored URL is unreadable and
 * nobody can retype it at a keyboard.
 *
 * [GUIDE] deliberately points at the README section, NOT `/releases/latest`:
 * the Android app's own GitHub releases share that page and would steal
 * "latest" from the `desktop-v*` receiver release.
 */
object PlayToPcLinks {
    const val GUIDE = "https://github.com/rsavutiu/OfflinePlaya#stream-to-pc-play-to-pc"
    const val GUIDE_SHORT = "github.com/rsavutiu/OfflinePlaya"
}
