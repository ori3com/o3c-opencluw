#!/usr/bin/env python3
"""コンフリクトマーカーを完全除去するスクリプト（両方保持）"""
import os, re

BASE = '/Users/yu-ga/Documents/GitHub/OpenClawAssistant/app/src/main/res'
files = []
for d in os.listdir(BASE):
    path = os.path.join(BASE, d, 'strings.xml')
    if os.path.exists(path):
        files.append(path)
files.append(os.path.join(BASE, 'values', 'strings.xml'))

for filepath in files:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if '<<<<<<<' not in content:
        continue
    
    # コンフリクトブロックを両方保持して解消
    def resolve(m):
        ours   = m.group(1)
        theirs = m.group(2)
        # 両方を結合（重複行を除去）
        ours_lines   = ours.strip().splitlines()
        theirs_lines = theirs.strip().splitlines()
        combined = []
        seen = set()
        for line in ours_lines + theirs_lines:
            stripped = line.strip()
            if stripped and stripped not in seen:
                seen.add(stripped)
                combined.append(line)
        return '\n'.join(combined) + '\n'
    
    content = re.sub(
        r'<<<<<<< .*?\n(.*?)=======\n(.*?)>>>>>>> .*?\n',
        resolve,
        content,
        flags=re.DOTALL
    )
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'  解消: {os.path.basename(os.path.dirname(filepath))}/strings.xml')

print('完了!')
