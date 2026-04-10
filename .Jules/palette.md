## 2024-05-14 - Semantic Roles on Clickable Elements
**Learning:** Using Jetpack Compose's `.clickable()` modifier implicitly provides interaction states, but it lacks semantic roles needed by screen readers. When an icon or generic container functions as a button or dropdown, TalkBack won't announce it correctly unless `role = Role.Button` or `Role.DropdownList` is set.
**Action:** Always provide an explicit `onClickLabel` and `role` when building custom interactive elements with `Modifier.clickable()` to guarantee accurate screen reader announcements.
