import re
import glob

for filepath in glob.glob("app/src/main/res/values-*/strings.xml"):
    with open(filepath, "r") as f:
        content = f.read()
        
    if "<<<<<<< HEAD" in content:
        lines = content.split('\n')
        out_lines = []
        
        cleaned_lines = []
        for line in lines:
            if line.startswith("<<<<<<< HEAD") or line.startswith("=======") or line.startswith(">>>>>>>"):
                continue
            cleaned_lines.append(line)
        
        seen_names = set()
        final_lines_reversed = []
        for line in reversed(cleaned_lines):
            match = re.search(r'<string name="([^"]+)"', line)
            if match:
                name = match.group(1)
                if name in seen_names:
                    continue
                seen_names.add(name)
            final_lines_reversed.append(line)
            
        final_lines = list(reversed(final_lines_reversed))
        
        with open(filepath, "w") as f:
            f.write('\n'.join(final_lines))
        print("Resolved", filepath)

