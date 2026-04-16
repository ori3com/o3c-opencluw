package com.openclaw.assistant.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openclaw.assistant.BuildConfig

/**
 * Records this throwable to Crashlytics when Firebase is enabled.
 * No-op in builds where FIREBASE_ENABLED is false (e.g. fork PRs without a real API key).
 */
fun Throwable.recordToCrashlytics() {
    if (BuildConfig.FIREBASE_ENABLED) {
        FirebaseCrashlytics.getInstance().recordException(this)
    }
}
