#!/usr/bin/env python3
"""重複した文字列定義を除去するスクリプト"""
import os, re

BASE = '/Users/yu-ga/Documents/GitHub/OpenClawAssistant/app/src/main/res'
files = []
for d in os.listdir(BASE):
    path = os.path.join(BASE, d, 'strings.xml')
    if os.path.exists(path):
        files.append(path)

for filepath in files:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # name="xxx" の文字列定義を1つだけ残す（最後のものを優先）
    seen = {}
    lines = content.splitlines(keepends=True)
    result = []
    i = 0
    while i < len(lines):
        line = lines[i]
        m = re.match(r'\s*<string name="([^"]+)"', line)
        if m:
            key = m.group(1)
            if key in seen:
                # 重複: スキップ（インラインか複数行か判定）
                if '</string>' not in line:
                    # 複数行 → 閉じタグまでスキップ
                    while i < len(lines) and '</string>' not in lines[i]:
                        i += 1
                    i += 1
                else:
                    i += 1
                print(f'  重複除去: {key} in {os.path.basename(os.path.dirname(filepath))}/strings.xml')
                continue
            else:
                seen[key] = True
        result.append(line)
        i += 1

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(''.join(result))

print('完了!')
