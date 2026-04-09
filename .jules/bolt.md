## 2025-02-12 - Optimizing joinToString in Hot Paths
**Learning:** In Kotlin codebases processing potentially large lists (like Android's `PackageManager.getInstalledApplications()`), chaining `.filter { ... }.joinToString(",") { "..." }` creates intermediate `ArrayList` and string instances for every item. This is particularly problematic for JSON serialization.
**Action:** Replace `joinToString` with a manual `StringBuilder` and loop.
