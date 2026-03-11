package com.openclaw.assistant.chat

import org.junit.Test

class ChatMarkdownPreprocessorBenchmarkTest {
    @Test
    fun benchmarkPreprocess() {
        val rawText = """
            Conversation info (untrusted metadata):
            ```json
            {"id":"1234"}
            ```

            [Mon 2026-02-23 21:54 GMT+9]
            Here is the actual message content.

            With some newlines.

            And more content.
        """.trimIndent()

        // Warmup
        for (i in 1..100) {
            ChatMarkdownPreprocessor.preprocess(rawText)
        }

        val start = System.nanoTime()
        for (i in 1..1000) {
            ChatMarkdownPreprocessor.preprocess(rawText)
        }
        val end = System.nanoTime()

        println("Benchmark ChatMarkdownPreprocessor.preprocess 1000 iterations: ${(end - start) / 1000000.0} ms")
    }
}
