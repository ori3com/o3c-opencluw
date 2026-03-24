💡 What: Replaced `listOf(a, b, c).joinToString(separator)` with direct string interpolation (`"$a$separator$b$separator$c"`) inside the `messageIdentityKey` function in `ChatController.kt`.

🎯 Why: Generating a content fingerprint runs frequently as it's used to identify uniqueness of messages. The previous implementation constructed intermediate `List` instances (and iterators) dynamically just to join the elements using a separator.

📊 Impact: Eliminates transient list allocations and garbage collection pressure in a frequently called UI key-generation function, reducing unnecessary overhead and improving general reactivity.

🔬 Measurement: Observe memory allocations and GC logs during long chat sessions with lots of message reconciliations. The allocations tied to `java.util.Arrays.asList` and iterator objects should decrease significantly.
