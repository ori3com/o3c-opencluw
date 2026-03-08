package com.openclaw.assistant.node

import android.content.Context
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.every
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import kotlinx.coroutines.runBlocking

class SystemHandlerTest {
    private val context = mockk<Context>()
    private val handler = SystemHandler(context)

    @Test
    fun `handleNotify returns INVALID_REQUEST when params are null`() = runBlocking {
        val result = handler.handleNotify(null)

        assertEquals(false, result.ok)
        assertEquals("INVALID_REQUEST", result.error?.code)
    }

    @Test
    fun `handleNotify returns INVALID_REQUEST when message is empty`() = runBlocking {
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED

        val result = handler.handleNotify("""{"title":"test"}""")

        // message field is absent -> INVALID_REQUEST regardless of SDK version
        assertEquals(false, result.ok)
        assertEquals("INVALID_REQUEST", result.error?.code)
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `handleNotify returns NOT_AUTHORIZED on Android 13+ when POST_NOTIFICATIONS denied`() = runBlocking {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Permission check is only enforced on API 33+; skip on lower SDK environments
            return@runBlocking
        }
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_DENIED

        val result = handler.handleNotify("""{"message":"test"}""")

        assertEquals(false, result.ok)
        assertEquals("NOT_AUTHORIZED", result.error?.code)
        unmockkStatic(ContextCompat::class)
    }
}
