## 2024-04-17 - Missing Button Role on Clickable Surface
**Learning:** In Jetpack Compose, while `Modifier.clickable` accepts a `role` parameter directly, `Surface(onClick = ...)` does not. This causes interactive surfaces to be missing semantic roles for screen readers like TalkBack, making them inaccessible.
**Action:** Always explicitly append `.semantics { role = Role.Button }` to the `Modifier` of an interactive `Surface` to ensure screen readers correctly announce the element's interactability.
