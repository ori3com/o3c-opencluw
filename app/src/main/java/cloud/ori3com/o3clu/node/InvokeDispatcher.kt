package cloud.ori3com.o3clu.node

import cloud.ori3com.o3clu.gateway.GatewaySession
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
import cloud.ori3com.o3clu.protocol.OpenClawWifiCommand
import cloud.ori3com.o3clu.protocol.OpenClawAppCommand
import cloud.ori3com.o3clu.protocol.OpenClawClipboardCommand
import cloud.ori3com.o3clu.protocol.OpenClawBridgeCommand
import cloud.ori3com.o3clu.protocol.OpenClawVoiceWakeCommand

class InvokeDispatcher(
  private val canvas: CanvasController,
  private val cameraHandler: CameraHandler,
  private val locationHandler: LocationHandler,
  private val screenHandler: ScreenHandler,
  private val smsHandler: SmsHandler,
  private val notificationsHandler: NotificationsHandler,
  private val systemHandler: SystemHandler,
  private val photosHandler: PhotosHandler,
  private val contactsHandler: ContactsHandler,
  private val calendarHandler: CalendarHandler,
  private val motionHandler: MotionHandler,
  private val wifiHandler: WifiHandler,
  private val clipboardHandler: ClipboardHandler,
  private val appHandler: AppHandler,
  private val voiceWakeHandler: VoiceWakeHandler,
  private val a2uiHandler: A2UIHandler,
  private val debugHandler: DebugHandler,
  private val appUpdateHandler: AppUpdateHandler,
  private val deviceHandler: DeviceHandler,
  private val mobileBridgeHandler: MobileBridgeHandler,
  private val isForeground: () -> Boolean,
  private val cameraEnabled: () -> Boolean,
  private val locationEnabled: () -> Boolean,
) {
  suspend fun handleInvoke(command: String, paramsJson: String?): GatewaySession.InvokeResult {
    // Check foreground requirement for canvas/camera/screen commands
    if (
      command.startsWith(OpenClawCanvasCommand.NamespacePrefix) ||
        command.startsWith(OpenClawCanvasA2UICommand.NamespacePrefix) ||
        command.startsWith(OpenClawCameraCommand.NamespacePrefix) ||
        command.startsWith(OpenClawScreenCommand.NamespacePrefix)
    ) {
      if (!isForeground()) {
        return GatewaySession.InvokeResult.error(
          code = "NODE_BACKGROUND_UNAVAILABLE",
          message = "NODE_BACKGROUND_UNAVAILABLE: canvas/camera/screen commands require foreground",
        )
      }
    }

    // Check camera enabled
    if (command.startsWith(OpenClawCameraCommand.NamespacePrefix) && !cameraEnabled()) {
      return GatewaySession.InvokeResult.error(
        code = "CAMERA_DISABLED",
        message = "CAMERA_DISABLED: enable Camera in Settings",
      )
    }

    // Check location enabled
    if (command.startsWith(OpenClawLocationCommand.NamespacePrefix) && !locationEnabled()) {
      return GatewaySession.InvokeResult.error(
        code = "LOCATION_DISABLED",
        message = "LOCATION_DISABLED: enable Location in Settings",
      )
    }

    return when (command) {
      // Canvas commands
      OpenClawCanvasCommand.Present.rawValue -> {
        val url = CanvasController.parseNavigateUrl(paramsJson)
        canvas.navigate(url)
        GatewaySession.InvokeResult.ok(null)
      }
      OpenClawCanvasCommand.Hide.rawValue -> GatewaySession.InvokeResult.ok(null)
      OpenClawCanvasCommand.Navigate.rawValue -> {
        val url = CanvasController.parseNavigateUrl(paramsJson)
        canvas.navigate(url)
        GatewaySession.InvokeResult.ok(null)
      }
      OpenClawCanvasCommand.Eval.rawValue -> {
        val js =
          CanvasController.parseEvalJs(paramsJson)
            ?: return GatewaySession.InvokeResult.error(
              code = "INVALID_REQUEST",
              message = "INVALID_REQUEST: javaScript required",
            )
        val result =
          try {
            canvas.eval(js)
          } catch (err: Throwable) {
            return GatewaySession.InvokeResult.error(
              code = "NODE_BACKGROUND_UNAVAILABLE",
              message = "NODE_BACKGROUND_UNAVAILABLE: canvas unavailable",
            )
          }
        GatewaySession.InvokeResult.ok("""{"result":${result.toJsonString()}}""")
      }
      OpenClawCanvasCommand.Snapshot.rawValue -> {
        val snapshotParams = CanvasController.parseSnapshotParams(paramsJson)
        val base64 =
          try {
            canvas.snapshotBase64(
              format = snapshotParams.format,
              quality = snapshotParams.quality,
              maxWidth = snapshotParams.maxWidth,
            )
          } catch (err: Throwable) {
            return GatewaySession.InvokeResult.error(
              code = "NODE_BACKGROUND_UNAVAILABLE",
              message = "NODE_BACKGROUND_UNAVAILABLE: canvas unavailable",
            )
          }
        GatewaySession.InvokeResult.ok("""{"format":"${snapshotParams.format.rawValue}","base64":"$base64"}""")
      }

      // A2UI commands
      OpenClawCanvasA2UICommand.Reset.rawValue -> {
        val a2uiUrl = a2uiHandler.resolveA2uiHostUrl()
          ?: return GatewaySession.InvokeResult.error(
            code = "A2UI_HOST_NOT_CONFIGURED",
            message = "A2UI_HOST_NOT_CONFIGURED: gateway did not advertise canvas host",
          )
        val ready = a2uiHandler.ensureA2uiReady(a2uiUrl)
        if (!ready) {
          return GatewaySession.InvokeResult.error(
            code = "A2UI_HOST_UNAVAILABLE",
            message = "A2UI host not reachable",
          )
        }
        val res = canvas.eval(A2UIHandler.a2uiResetJS)
        GatewaySession.InvokeResult.ok(res)
      }
      OpenClawCanvasA2UICommand.Push.rawValue, OpenClawCanvasA2UICommand.PushJSONL.rawValue -> {
        val messages =
          try {
            a2uiHandler.decodeA2uiMessages(command, paramsJson)
          } catch (err: Throwable) {
            return GatewaySession.InvokeResult.error(
              code = "INVALID_REQUEST",
              message = err.message ?: "invalid A2UI payload"
            )
          }
        val a2uiUrl = a2uiHandler.resolveA2uiHostUrl()
          ?: return GatewaySession.InvokeResult.error(
            code = "A2UI_HOST_NOT_CONFIGURED",
            message = "A2UI_HOST_NOT_CONFIGURED: gateway did not advertise canvas host",
          )
        val ready = a2uiHandler.ensureA2uiReady(a2uiUrl)
        if (!ready) {
          return GatewaySession.InvokeResult.error(
            code = "A2UI_HOST_UNAVAILABLE",
            message = "A2UI host not reachable",
          )
        }
        val js = A2UIHandler.a2uiApplyMessagesJS(messages)
        val res = canvas.eval(js)
        GatewaySession.InvokeResult.ok(res)
      }

      // Camera commands
      OpenClawCameraCommand.Snap.rawValue -> cameraHandler.handleSnap(paramsJson)
      OpenClawCameraCommand.Clip.rawValue -> cameraHandler.handleClip(paramsJson)
      OpenClawCameraCommand.List.rawValue -> cameraHandler.handleList(paramsJson)

      // Location command
      OpenClawLocationCommand.Get.rawValue -> locationHandler.handleLocationGet(paramsJson)
      OpenClawLocationCommand.History.rawValue -> locationHandler.handleLocationHistory(paramsJson)
      OpenClawLocationCommand.LastKnown.rawValue -> locationHandler.handleLocationLastKnown(paramsJson)
      OpenClawLocationCommand.SetTracking.rawValue -> locationHandler.handleLocationSetTracking(paramsJson)

      // Screen command
      OpenClawScreenCommand.Record.rawValue -> screenHandler.handleScreenRecord(paramsJson)

      // SMS command
      OpenClawSmsCommand.Send.rawValue -> smsHandler.handleSmsSend(paramsJson)
      OpenClawSmsCommand.ReadLatest.rawValue -> smsHandler.handleSmsReadLatest()
      OpenClawSmsCommand.ReadUnread.rawValue -> smsHandler.handleSmsReadUnread()

      // Notifications commands
      OpenClawNotificationsCommand.List.rawValue -> notificationsHandler.handleList()
      OpenClawNotificationsCommand.Actions.rawValue -> notificationsHandler.handleActions(paramsJson)

      // System command
      OpenClawSystemCommand.Notify.rawValue -> systemHandler.handleNotify(paramsJson)
      OpenClawSystemCommand.Volume.rawValue -> systemHandler.handleVolume(paramsJson)
      OpenClawSystemCommand.Brightness.rawValue -> systemHandler.handleBrightness(paramsJson)

      // Photos command
      OpenClawPhotosCommand.Latest.rawValue -> photosHandler.handleLatest()

      // Contacts commands
      OpenClawContactsCommand.Search.rawValue -> contactsHandler.handleSearch(paramsJson)
      OpenClawContactsCommand.Add.rawValue -> contactsHandler.handleAdd(paramsJson)
      OpenClawContactsCommand.Update.rawValue -> contactsHandler.handleUpdate(paramsJson)
      OpenClawContactsCommand.Delete.rawValue -> contactsHandler.handleDelete(paramsJson)

      // Calendar commands
      OpenClawCalendarCommand.Events.rawValue -> calendarHandler.handleEvents(paramsJson)
      OpenClawCalendarCommand.Add.rawValue -> calendarHandler.handleAdd(paramsJson)
      OpenClawCalendarCommand.Update.rawValue -> calendarHandler.handleUpdate(paramsJson)
      OpenClawCalendarCommand.Delete.rawValue -> calendarHandler.handleDelete(paramsJson)

      // Motion commands
      OpenClawMotionCommand.Activity.rawValue -> motionHandler.handleActivity()
      OpenClawMotionCommand.Pedometer.rawValue -> motionHandler.handlePedometer()

      // Wifi commands
      OpenClawWifiCommand.List.rawValue -> wifiHandler.handleWifiList()
      OpenClawWifiCommand.Status.rawValue -> wifiHandler.handleWifiStatus()
      OpenClawWifiCommand.Connect.rawValue -> wifiHandler.handleWifiConnect(paramsJson)

      // App commands
      OpenClawAppCommand.List.rawValue -> appHandler.handleAppList()
      OpenClawAppCommand.Launch.rawValue -> appHandler.handleAppLaunch(paramsJson)

      // Clipboard commands
      OpenClawClipboardCommand.Read.rawValue -> clipboardHandler.handleClipboardRead()
      OpenClawClipboardCommand.Write.rawValue -> clipboardHandler.handleClipboardWrite(paramsJson)

      // VoiceWake commands
      OpenClawVoiceWakeCommand.GetMode.rawValue -> voiceWakeHandler.handleVoiceWakeGetMode()
      OpenClawVoiceWakeCommand.SetMode.rawValue -> voiceWakeHandler.handleVoiceWakeSetMode(paramsJson)
      OpenClawVoiceWakeCommand.Status.rawValue -> voiceWakeHandler.handleVoiceWakeStatus()

      // Device commands
      OpenClawDeviceCommand.Status.rawValue -> deviceHandler.handleStatus()
      OpenClawDeviceCommand.Info.rawValue -> deviceHandler.handleInfo()
      OpenClawDeviceCommand.Permissions.rawValue -> deviceHandler.handlePermissions()
      OpenClawDeviceCommand.Health.rawValue -> deviceHandler.handleHealth()

      // Mobile Bridge compatibility commands
      OpenClawBridgeCommand.Status.rawValue -> mobileBridgeHandler.handleStatus()
      OpenClawBridgeCommand.Manifest.rawValue -> mobileBridgeHandler.handleManifest()
      OpenClawBridgeCommand.Execute.rawValue -> mobileBridgeHandler.handleExecute(paramsJson)
      OpenClawBridgeCommand.Grants.rawValue -> mobileBridgeHandler.handleGrants()
      OpenClawBridgeCommand.Revoke.rawValue -> mobileBridgeHandler.handleRevoke(paramsJson)

      // Debug commands
      "debug.ed25519" -> debugHandler.handleEd25519()
      "debug.logs" -> debugHandler.handleLogs()

      // App update
      "app.update" -> appUpdateHandler.handleUpdate(paramsJson)

      else ->
        GatewaySession.InvokeResult.error(
          code = "INVALID_REQUEST",
          message = "INVALID_REQUEST: unknown command",
        )
    }
  }
}
