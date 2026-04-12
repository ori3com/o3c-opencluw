package com.openclaw.assistant.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openclaw.assistant.BuildConfig

/**
 * Records this throwable to Firebase Crashlytics when [BuildConfig.FIREBASE_ENABLED] is true.
 * No-op in fork PR builds where FIREBASE_ENABLED=false and no real API key is present.
 */
fun Throwable.recordToCrashlytics() {
    if (BuildConfig.FIREBASE_ENABLED) {
        FirebaseCrashlytics.getInstance().recordException(this)
    }
}
