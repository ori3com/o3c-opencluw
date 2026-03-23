## 2025-02-13 - Dynamic Content Descriptions for Multi-state Buttons
**Learning:** Hardcoded accessibility descriptions (`contentDescription`) on multi-purpose Compose elements (like a FAB that acts as "Send", "Stop", or "Mic" depending on state) will cause screen readers to announce misleading actions.
**Action:** When evaluating or creating multi-state interactive icons or buttons in Compose, always ensure the `contentDescription` string is computed dynamically using the same state rules as the `imageVector` or `onClick` handler.

## 2024-05-19 - Standardize contentDescription for File Attachments
**Learning:** Hardcoded accessibility descriptions in Jetpack Compose (e.g., `contentDescription = "Attach file"`) prevent localization for screen readers, diminishing the experience for non-English users.
**Action:** Always extract `contentDescription` strings to `strings.xml` and use `stringResource(R.string.key)` to ensure accessibility labels are fully localizable.

## 2024-11-20 - Expand/Collapse Accessibility Pattern
**Learning:** Adding a `contentDescription` to an expand/collapse `Icon` inside a clickable row that already has adjacent descriptive text causes duplicate/confusing screen reader announcements.
**Action:** Always set the `onClickLabel` of the `Modifier.clickable` parent `Row` to describe the action (e.g. "Expand" or "Collapse") dynamically based on state, assign a semantic `role = Role.Button`, and set the child `Icon`'s `contentDescription = null` to ensure a single, clear semantic interaction.

## $(date +%Y-%m-%d) - Expand/Collapse State Accessibility in Jetpack Compose
**Learning:** In Jetpack Compose, when building custom expandable components (like error or warning cards), default Material `Card` components with an `onClick` parameter do not automatically announce their state (expanded vs collapsed) to screen readers (TalkBack). Furthermore, purely decorative state icons (like chevrons) are often left with `contentDescription = null`, causing screen reader users to completely miss the interaction paradigm.
**Action:** When creating custom expandable interactive containers, always apply an `onClickLabel` (e.g., via `Modifier.clickable(onClickLabel = ...)`) that dynamically switches between "Expand" and "Collapse" string resources based on the current state, and ensure accompanying state icons either share this description or are explicitly marked `null` if the container's label already covers the interaction semantics adequately.
