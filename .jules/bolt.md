## 2024-05-24 - SpeechRecognizer Race Conditions
**Learning:** Reusing the `SpeechRecognizer` instance to avoid the ~300-500ms binding latency introduces a severe race condition during rapid stop/start sequences. If `cancel()` is dispatched asynchronously, it can kill a newly started session. The Android `SpeechRecognizer` API is notoriously finicky across OEMs, and recreating the instance is a required workaround to prevent `ERROR_CLIENT` (5) or `ERROR_RECOGNIZER_BUSY` (8) crashes.
**Action:** Do NOT attempt to optimize `SpeechRecognizer` by caching the instance. Always destroy and recreate it between sessions to ensure a clean state, as documented in the codebase.

## 2024-05-24 - O(1) checks in Jetpack Compose
**Learning:** O(N) list traversals (like `.count { ... }`) inside frequent `LaunchedEffect` blocks (e.g., during AI chat streaming) can cause unnecessary CPU overhead on the main thread during heavy UI updates.
**Action:** Replace full list iterations with O(1) checks where possible, such as comparing the unique ID of the `.lastOrNull()` item when detecting new messages appended to the end of a list.
