package com.openclaw.assistant.speech

import org.junit.Test

class TTSUtilsBenchmarkTest {
    @Test
    fun benchmarkStripMarkdown() {
        val markdownText = """
            # Heading 1
            Here is some **bold** text and some *italic* text.
            Also __bold__ and _italic_.

            ```kotlin
            val x = 1
            ```

            - bullet 1
            - bullet 2

            [Link](http://example.com) and ![Image](http://example.com/image.png)

            > blockquote

            ---

            Some `inline code`.

            Lots of newlines



            here.
        """.trimIndent()

        // Warmup
        for (i in 1..100) {
            TTSUtils.stripMarkdownForSpeech(markdownText)
        }

        val start = System.nanoTime()
        for (i in 1..1000) {
            TTSUtils.stripMarkdownForSpeech(markdownText)
        }
        val end = System.nanoTime()

        println("Benchmark TTSUtils.stripMarkdownForSpeech 1000 iterations: ${(end - start) / 1000000.0} ms")
    }
}
