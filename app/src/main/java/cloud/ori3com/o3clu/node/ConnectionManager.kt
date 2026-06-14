package cloud.ori3com.o3clu.node

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import cloud.ori3com.o3clu.BuildConfig
import cloud.ori3com.o3clu.SecurePrefs
import cloud.ori3com.o3clu.gateway.GatewayClientInfo
import cloud.ori3com.o3clu.gateway.GatewayConnectOptions
import cloud.ori3com.o3clu.gateway.GatewayEndpoint
import cloud.ori3com.o3clu.gateway.GatewayTlsParams
import cloud.ori3com.o3clu.protocol.OpenClawCanvasA2UICommand
import cloud.ori3com.o3clu.protocol.OpenClawCanvasCommand
import cloud.ori3com.o3clu.protocol.OpenClawCameraCommand
import cloud.ori3com.o3clu.protocol.OpenClawDeviceCommand
import cloud.ori3com.o3clu.protocol.OpenClawLocationCommand
import cloud.ori3com.o3clu.protocol.OpenClawScreenCommand
import cloud.ori3com.o3clu.protocol.OpenClawSmsCommand
import cloud.ori3com.o3clu.protocol.OpenClawNotificationsCommand
import cloud.ori3com.o3clu.protocol.OpenClawSystemCommand
import cloud.ori3com.o3clu.protocol.OpenClawPhotosCommand
import cloud.ori3com.o3clu.protocol.OpenClawContactsCommand
import cloud.ori3com.o3clu.protocol.OpenClawCalendarCommand
import cloud.ori3com.o3clu.protocol.OpenClawMotionCommand
import cloud.ori3com.o3clu.protocol.OpenClawCapability
import cloud.ori3com.o3clu.protocol.OpenClawBridgeCommand
import cloud.ori3com.o3clu.LocationMode
import cloud.ori3com.o3clu.VoiceWakeMode
import android.provider.Settings

class ConnectionManager(
  private val prefs: SecurePrefs,
  private val appContext: Context,
  private val cameraEnabled: () -> Boolean,
  private val locationMode: () -> LocationMode,
  private val voiceWakeMode: () -> VoiceWakeMode,
  private val smsAvailable: () -> Boolean,
  private val hasRecordAudioPermission: () -> Boolean,
  private val manualTls: () -> Boolean,
  private val deviceId: () -> String?,
) {
  companion object {
    internal fun resolveTlsParamsForEndpoint(
      endpoint: GatewayEndpoint,
      storedFingerprint: String?,
      manualTlsEnabled: Boolean,
    ): GatewayTlsParams? {
      val stableId = endpoint.stableId
      val stored = storedFingerprint?.trim().takeIf { !it.isNullOrEmpty() }
      val isManual = stableId.startsWith("manual|")

      if (isManual) {
        if (!manualTlsEnabled) return null
        if (!stored.isNullOrBlank()) {
          return GatewayTlsParams(
            required = true,
            expectedFingerprint = stored,
            allowTOFU = false,
            stableId = stableId,
          )
        }
        return GatewayTlsParams(
          required = true,
          expectedFingerprint = null,
          allowTOFU = false,
          stableId = stableId,
        )
      }

      // Prefer stored pins. Never let discovery-provided TXT override a stored fingerprint.
      if (!stored.isNullOrBlank()) {
        return GatewayTlsParams(
          required = true,
          expectedFingerprint = stored,
          allowTOFU = false,
          stableId = stableId,
        )
      }

      val hinted = endpoint.tlsEnabled || !endpoint.tlsFingerprintSha256.isNullOrBlank()
      if (hinted) {
        // TXT is unauthenticated. Do not treat the advertised fingerprint as authoritative.
        return GatewayTlsParams(
          required = true,
          expectedFingerprint = null,
          allowTOFU = false,
          stableId = stableId,
        )
      }

      return null
    }
  }

  private fun hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
  }

  private fun isNotificationListenerEnabled(): Boolean {
    val enabledPackages = Settings.Secure.getString(appContext.contentResolver, "enabled_notification_listeners")
    return enabledPackages?.contains(appContext.packageName) == true
  }

  fun buildInvokeCommands(): List<String> =
    buildList {
      add(OpenClawCanvasCommand.Present.rawValue)
      add(OpenClawCanvasCommand.Hide.rawValue)
      add(OpenClawCanvasCommand.Navigate.rawValue)
      add(OpenClawCanvasCommand.Eval.rawValue)
      add(OpenClawCanvasCommand.Snapshot.rawValue)
      add(OpenClawCanvasA2UICommand.Push.rawValue)
      add(OpenClawCanvasA2UICommand.PushJSONL.rawValue)
      add(OpenClawCanvasA2UICommand.Reset.rawValue)
      OpenClawBridgeCommand.entries.forEach { add(it.rawValue) }
      add(OpenClawScreenCommand.Record.rawValue)
      OpenClawDeviceCommand.entries.forEach { add(it.rawValue) }
      if (cameraEnabled()) {
        add(OpenClawCameraCommand.List.rawValue)
        add(OpenClawCameraCommand.Snap.rawValue)
        add(OpenClawCameraCommand.Clip.rawValue)
      }
      if (locationMode() != LocationMode.Off) {
        add(OpenClawLocationCommand.Get.rawValue)
      }
      if (smsAvailable()) {
        add(OpenClawSmsCommand.Send.rawValue)
      }

      // Notifications
      if (isNotificationListenerEnabled()) {
        add(OpenClawNotificationsCommand.List.rawValue)
        add(OpenClawNotificationsCommand.Actions.rawValue)
      }

      // System
      add(OpenClawSystemCommand.Notify.rawValue)

      // Photos
      val photosPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
      } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
      }
      if (hasPermission(photosPermission)) {
        add(OpenClawPhotosCommand.Latest.rawValue)
      }

      // Contacts
      if (hasPermission(Manifest.permission.READ_CONTACTS)) {
        add(OpenClawContactsCommand.Search.rawValue)
      }
      if (hasPermission(Manifest.permission.WRITE_CONTACTS)) {
        add(OpenClawContactsCommand.Add.rawValue)
      }

      // Calendar
      if (hasPermission(Manifest.permission.READ_CALENDAR)) {
        add(OpenClawCalendarCommand.Events.rawValue)
      }
      if (hasPermission(Manifest.permission.WRITE_CALENDAR)) {
        add(OpenClawCalendarCommand.Add.rawValue)
      }

      // Motion
      if (hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
        add(OpenClawMotionCommand.Activity.rawValue)
        add(OpenClawMotionCommand.Pedometer.rawValue)
      }

      if (BuildConfig.DEBUG) {
        add("debug.logs")
        add("debug.ed25519")
      }
      add("app.update")
    }

  fun buildCapabilities(): List<String> =
    buildList {
      add(OpenClawCapability.Canvas.rawValue)
      add(OpenClawCapability.Screen.rawValue)
      add(OpenClawCapability.System.rawValue)
      add(OpenClawCapability.Bridge.rawValue)

      if (isNotificationListenerEnabled()) {
        add(OpenClawCapability.Notifications.rawValue)
      }

      val photosPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
      } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
      }
      if (hasPermission(photosPermission)) {
        add(OpenClawCapability.Photos.rawValue)
      }

      if (hasPermission(Manifest.permission.READ_CONTACTS) || hasPermission(Manifest.permission.WRITE_CONTACTS)) {
        add(OpenClawCapability.Contacts.rawValue)
      }

      if (hasPermission(Manifest.permission.READ_CALENDAR) || hasPermission(Manifest.permission.WRITE_CALENDAR)) {
        add(OpenClawCapability.Calendar.rawValue)
      }

      if (hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
        add(OpenClawCapability.Motion.rawValue)
      }

      if (cameraEnabled()) add(OpenClawCapability.Camera.rawValue)
      if (smsAvailable()) add(OpenClawCapability.Sms.rawValue)
      if (voiceWakeMode() != VoiceWakeMode.Off && hasRecordAudioPermission()) {
        add(OpenClawCapability.VoiceWake.rawValue)
      }
      if (locationMode() != LocationMode.Off) {
        add(OpenClawCapability.Location.rawValue)
      }
    }

  fun resolvedVersionName(): String {
    val versionName = BuildConfig.VERSION_NAME.trim().ifEmpty { "dev" }
    return if (BuildConfig.DEBUG && !versionName.contains("dev", ignoreCase = true)) {
      "$versionName-dev"
    } else {
      versionName
    }
  }

  fun resolveModelIdentifier(): String? {
    return listOfNotNull(Build.MANUFACTURER, Build.MODEL)
      .joinToString(" ")
      .trim()
      .ifEmpty { null }
  }

  fun buildUserAgent(): String {
    val version = resolvedVersionName()
    val release = Build.VERSION.RELEASE?.trim().orEmpty()
    val releaseLabel = if (release.isEmpty()) "unknown" else release
    return "OpenClawAndroid/$version (Android $releaseLabel; SDK ${Build.VERSION.SDK_INT})"
  }

  fun buildClientInfo(clientId: String, clientMode: String): GatewayClientInfo {
    return GatewayClientInfo(
      id = clientId,
      displayName = deviceId() ?: prefs.displayName.value,
      version = resolvedVersionName(),
      platform = "android",
      mode = clientMode,
      instanceId = prefs.instanceId.value,
      deviceFamily = "Android",
      modelIdentifier = resolveModelIdentifier(),
    )
  }

  fun buildNodeConnectOptions(): GatewayConnectOptions {
    val caps = buildCapabilities()
    // Derive requested scopes only from permission-gated capabilities.
    // Always-on capabilities (canvas, screen, system) are unconditionally supported and must not
    // require the approving operator to hold specific node.* scopes — doing so causes
    // `openclaw devices approve` to fail for CLI operators that lack those scopes.
    val alwaysOnCaps = setOf(
      OpenClawCapability.Canvas.rawValue,
      OpenClawCapability.Screen.rawValue,
      OpenClawCapability.System.rawValue,
      OpenClawCapability.Bridge.rawValue,
    )
    val requestedScopes = caps.filterNot { it in alwaysOnCaps }.map { "node.$it" }

    return GatewayConnectOptions(
      role = "node",
      scopes = requestedScopes,
      caps = caps,
      commands = buildInvokeCommands(),
      permissions = emptyMap(),
      client = buildClientInfo(clientId = "openclaw-android", clientMode = "node"),
      userAgent = buildUserAgent(),
    )
  }

  fun buildOperatorConnectOptions(): GatewayConnectOptions {
    return GatewayConnectOptions(
      role = "operator",
      scopes = listOf(
        "operator.read",
        "operator.write",
        "operator.admin",
        "operator.pairing",
        "operator.talk.secrets",
        "operator.approvals",
      ),
      caps = emptyList(),
      commands = emptyList(),
      permissions = emptyMap(),
      client = buildClientInfo(clientId = "openclaw-control-ui", clientMode = "ui"),
      userAgent = buildUserAgent(),
    )
  }

  fun buildPairingOperatorConnectOptions(): GatewayConnectOptions {
    return GatewayConnectOptions(
      role = "operator",
      scopes = listOf(
        "operator.read",
        "operator.write",
        "operator.admin",
        "operator.pairing",
        "operator.talk.secrets",
        "operator.approvals",
      ),
      caps = emptyList(),
      commands = emptyList(),
      permissions = emptyMap(),
      client = buildClientInfo(clientId = "cli", clientMode = "cli"),
      userAgent = buildUserAgent(),
      ignoreStoredDeviceToken = true,
      includeDeviceIdentity = false,
      persistIssuedDeviceToken = false,
    )
  }

  fun resolveTlsParams(endpoint: GatewayEndpoint): GatewayTlsParams? {
    val stored = prefs.loadGatewayTlsFingerprint(endpoint.stableId)
    return resolveTlsParamsForEndpoint(endpoint, storedFingerprint = stored, manualTlsEnabled = manualTls())
  }
}
