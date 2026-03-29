1. Modify `app/src/main/java/com/openclaw/assistant/MainActivity.kt` to import `androidx.compose.ui.semantics.clearAndSetSemantics`.
2. In `SystemStatusCard`, wrap `StatusIndicator` and the two `Text` views inside a `Row` with `Modifier.weight(1f).semantics(mergeDescendants = true) {}`. This groups the "OpenClaw Assistant" and "Connected" texts into a single, clean announcement for TalkBack.
3. Wrap `StatusIndicator` itself in a `Box(modifier = Modifier.clearAndSetSemantics {})` to prevent its internal "Connected!" `contentDescription` from redundantly being read alongside the visible "Connected" text in the row.
4. Run native build/lint checks via Gradle.
5. Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.
6. Submit a PR titled `🎨 Polish: Improve connection status accessibility`.
