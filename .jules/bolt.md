## 2025-02-28 - Pre-compiled Regex patterns
**Learning:** Frequent initialization of `Regex("...")` objects inside hot loops or parsing functions (like text-to-speech text normalization or markdown parsing) introduces significant, measurable performance overhead because the regular expression pattern has to be compiled on every invocation. In this Kotlin Android codebase, extracting them to `private val` properties avoids repeated compilation, especially in methods like `stripMarkdownForSpeech` which parses an entire text block multiple times per message string.
**Action:** When working on text parsing features (e.g. ChatMarkdown blocks, TTS filters, Tool output formatting), always ensure `Regex` objects are pre-compiled as top-level file constants, or inside a `companion object` of a class, or as a property within a singleton `object`.

## 2024-05-24 - Hoist Static Collection Allocations in Loop Parsers
**Learning:** Instantiating static data structures like `listOf(...)` inside frequently called parsing methods (such as text chunking) causes redundant memory allocations and garbage collection pressure, leading to hidden CPU overhead.
**Action:** Always hoist static parsing collections (like sentence or comma enders) to `private val` properties at the file or object level to ensure they are created exactly once.

## 2025-03-05 - Avoid listOf(...).joinToString() overhead
**Learning:** Constructing strings using `listOf(a, b, c).joinToString(separator)` on a hot path (like Jetpack Compose list reconciliation or text streaming) introduces measurable memory overhead from intermediate List instances, iterator allocations, and multiple string builds.
**Action:** Always replace `listOf(...).joinToString()` with direct string interpolation (e.g., `"$a$separator$b$separator$c"`) when constructing short, known-length strings in frequently executed loops or UI render cycles.

## 2025-03-05 - Avoid ViewModel Localization Caching
**Learning:** Attempting to micro-optimize string list allocations by caching `context.getString(...)` inside a `lazy` ViewModel or Application property is an Android anti-pattern. ViewModels survive configuration changes, meaning localized strings will be permanently stuck in the old language if the user changes the system device language while the app process is alive. Additionally, optimizing allocations triggered by discrete user events (like button clicks) provides no measurable impact compared to the bugs introduced.
**Action:** Do not hoist localized string lookups to `lazy` properties to "save allocations". Always fetch localized strings dynamically when needed, and focus optimization efforts strictly on hot loops and rendering paths.

## 2025-03-05 - Avoid listOf(...).joinToString() overhead
**Learning:** Constructing strings using `listOf(a, b, c).joinToString(separator)` on a hot path (like Jetpack Compose list reconciliation or text streaming) introduces measurable memory overhead from intermediate List instances, iterator allocations, and multiple string builds.
**Action:** Always replace `listOf(...).joinToString()` with direct string interpolation (e.g., `"$a$separator$b$separator$c"`) when constructing short, known-length strings in frequently executed loops or UI render cycles.

## 2025-03-05 - Avoid ViewModel Localization Caching
**Learning:** Attempting to micro-optimize string list allocations by caching `context.getString(...)` inside a `lazy` ViewModel or Application property is an Android anti-pattern. ViewModels survive configuration changes, meaning localized strings will be permanently stuck in the old language if the user changes the system device language while the app process is alive. Additionally, optimizing allocations triggered by discrete user events (like button clicks) provides no measurable impact compared to the bugs introduced.
**Action:** Do not hoist localized string lookups to `lazy` properties to "save allocations". Always fetch localized strings dynamically when needed, and focus optimization efforts strictly on hot loops and rendering paths.
