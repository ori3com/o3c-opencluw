with open(".jules/bolt.md", "r") as f:
    content = f.read()

import re

# Remove conflict markers and just merge the contents
content = content.replace("<<<<<<< HEAD\n", "")
content = content.replace("=======\n", "")
content = content.replace(">>>>>>> 9a6b952 (Optimize AppHandler string serialization)\n", "")

with open(".jules/bolt.md", "w") as f:
    f.write(content)
