#!/usr/bin/env python3
"""
コンフリクト解消スクリプト: HEADとorigin/mainの両方の変更を保持する
"""
import re
import os

def resolve_conflict_file(filepath):
    """コンフリクトマーカーを除去して両方の変更を保持する"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if '<<<<<<< HEAD' not in content:
        print(f"  コンフリクトなし: {filepath}")
        return False
    
    def resolve_block(match):
        head_content = match.group(1)
        main_content = match.group(2)
        # 両方の内容を保持（HEADの内容を先に、mainの内容を後に）
        return head_content + main_content
    
    # コンフリクトマーカーを処理
    pattern = r'<<<<<<< HEAD\n(.*?)=======\n(.*?)>>>>>>> origin/main\n'
    resolved = re.sub(pattern, resolve_block, content, flags=re.DOTALL)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(resolved)
    
    print(f"  解消完了: {filepath}")
    return True

conflict_files = [
    'app/src/main/res/values/strings.xml',
    'app/src/main/res/values-de/strings.xml',
    'app/src/main/res/values-es/strings.xml',
    'app/src/main/res/values-fr/strings.xml',
    'app/src/main/res/values-hi/strings.xml',
    'app/src/main/res/values-ru/strings.xml',
    'app/src/main/res/values-zh-rCN/strings.xml',
    'app/src/main/res/values-zh-rTW/strings.xml',
]

base_dir = '/Users/yu-ga/Documents/GitHub/OpenClawAssistant'
print("コンフリクト解消を開始...")
for f in conflict_files:
    filepath = os.path.join(base_dir, f)
    resolve_conflict_file(filepath)

print("完了!")
