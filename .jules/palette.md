## 2025-02-13 - Dynamic Content Descriptions for Multi-state Buttons
**Learning:** Hardcoded accessibility descriptions (`contentDescription`) on multi-purpose Compose elements (like a FAB that acts as "Send", "Stop", or "Mic" depending on state) will cause screen readers to announce misleading actions.
**Action:** When evaluating or creating multi-state interactive icons or buttons in Compose, always ensure the `contentDescription` string is computed dynamically using the same state rules as the `imageVector` or `onClick` handler.

## 2025-02-13 - Compose NavigationBarItem Accessibility Labels
**Learning:** `NavigationBarItem` (bottom navigation) icons typically should use their label as their `contentDescription` rather than `null`. Although the text label is present, screen readers sometimes benefit from or expect the explicit description on the icon when the item is focused as a single clickable element.
**Action:** When configuring bottom navigation items in Jetpack Compose, ensure the `Icon`'s `contentDescription` utilizes the item's label string instead of `null` to ensure consistent screen reader announcements.

## 2025-02-13 - Extracted Hardcoded Content Descriptions
**Learning:** Found hardcoded `contentDescription` strings (e.g. "Refresh") on `IconButton` components in `MainActivity.kt`. Hardcoding accessibility strings breaks localization and creates an inconsistent experience for screen reader users across different languages.
**Action:** Always extract hardcoded accessibility descriptions into `strings.xml` and reference them using `stringResource()`.
