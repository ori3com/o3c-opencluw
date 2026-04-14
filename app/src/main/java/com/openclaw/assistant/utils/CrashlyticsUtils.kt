package com.openclaw.assistant.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openclaw.assistant.BuildConfig

/**
 * Records this exception to Firebase Crashlytics when Firebase is enabled.
 * No-op in debug builds where FIREBASE_ENABLED=false (e.g. fork PRs without a real API key).
 */
fun Throwable.recordToCrashlytics() {
    if (BuildConfig.FIREBASE_ENABLED) {
        FirebaseCrashlytics.getInstance().recordException(this)
    }
}
