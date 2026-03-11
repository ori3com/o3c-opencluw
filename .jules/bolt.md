
## 2026-03-11 - Hoisting Regex instances in Kotlin
**Learning:** Instantiating `Regex` objects inline within frequently called methods (like `TTSUtils.stripMarkdownForSpeech` which formats chat messages) incurs a significant performance overhead due to repeated regex compilation.
**Action:** Always hoist `Regex` instances to `private val` properties at the file or object level when they use static patterns so they are compiled only once.
