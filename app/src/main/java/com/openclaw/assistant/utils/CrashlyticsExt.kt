package com.openclaw.assistant.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openclaw.assistant.BuildConfig

/**
 * Records this throwable to Firebase Crashlytics when Firebase is enabled.
 * No-op when [BuildConfig.FIREBASE_ENABLED] is false (e.g. fork PRs or unit-test builds).
 */
fun Throwable.recordToCrashlytics() {
    if (BuildConfig.FIREBASE_ENABLED) {
        FirebaseCrashlytics.getInstance().recordException(this)
    }
}
