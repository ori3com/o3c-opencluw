package com.openclaw.assistant.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class TTSUtilsTest {
    @Test
    fun testStripMarkdown() {
        val input = """
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

        // This relies on whatever the current output is for assertion
        val actual = TTSUtils.stripMarkdownForSpeech(input)
        println("Actual Output:")
        println(actual)
    }
}
