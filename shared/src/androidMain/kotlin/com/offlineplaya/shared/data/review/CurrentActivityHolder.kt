package com.offlineplaya.shared.data.review

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the currently-resumed Activity so [PlayReviewPrompter] has something to
 * launch Google Play's in-app review flow against (it can only run from a
 * resumed Activity). Registered once via
 * [Application.registerActivityLifecycleCallbacks] in `OfflinePlayaApp`.
 *
 * Holds the Activity weakly and clears it on pause so a finished Activity can't
 * be leaked or handed to the store after it's gone.
 */
class CurrentActivityHolder : Application.ActivityLifecycleCallbacks {

    @Volatile
    private var ref: WeakReference<Activity>? = null

    /** The resumed Activity, or `null` when the app is backgrounded. */
    val current: Activity?
        get() = ref?.get()

    override fun onActivityResumed(activity: Activity) {
        ref = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        // Only clear if we're still pointing at this Activity — a fast
        // A→B transition can resume B before A pauses.
        if (ref?.get() === activity) ref = null
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (ref?.get() === activity) ref = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
