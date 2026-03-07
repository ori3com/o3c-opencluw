#!/usr/bin/env python3
"""全言語ファイルに新規文字列を一括追加するスクリプト"""
import os

BASE = '/Users/yu-ga/Documents/GitHub/OpenClawAssistant/app/src/main/res'

# 追加する文字列（言語コード -> (qr_scan_title, qr_scan_cmd_label, code_input_title)）
translations = {
    'values-de': (
        'Option 1: QR-Code scannen',
        'Führen Sie dies auf dem Gateway-Host aus, um einen QR-Code anzuzeigen:',
        'Option 2: Setup-Code eingeben',
    ),
    'values-es': (
        'Opción 1: Escanear código QR',
        'Ejecuta esto en el host del gateway para mostrar un código QR:',
        'Opción 2: Introducir código de configuración',
    ),
    'values-fr': (
        'Option 1 : Scanner le QR code',
        'Exécutez ceci sur l\'hôte de la passerelle pour afficher un QR code :',
        'Option 2 : Saisir le code de configuration',
    ),
    'values-hi': (
        'विकल्प 1: QR कोड स्कैन करें',
        'QR कोड दिखाने के लिए गेटवे होस्ट पर यह चलाएं:',
        'विकल्प 2: सेटअप कोड दर्ज करें',
    ),
    'values-ja': (
        '方法1：QRコードをスキャン',
        'QRコードを表示するには、ゲートウェイホストでこれを実行してください:',
        '方法2：セットアップコードを入力',
    ),
    'values-ru': (
        'Вариант 1: Сканировать QR-код',
        'Запустите это на хосте шлюза для отображения QR-кода:',
        'Вариант 2: Введите код настройки',
    ),
    'values-zh-rCN': (
        '方式 1：扫描 QR 码',
        '在网关主机上运行以下命令以显示 QR 码：',
        '方式 2：输入设置码',
    ),
    'values-zh-rTW': (
        '方式 1：掃描 QR 碼',
        '在閘道主機上執行以下命令以顯示 QR 碼：',
        '方式 2：輸入設定碼',
    ),
}

# すでにQRスキャナーセクションが存在するかチェックし、新規文字列を追加
new_strings_template = '''    <string name="setup_guide_qr_scan_title">{qr_scan_title}</string>
    <string name="setup_guide_qr_scan_cmd_label">{qr_scan_cmd_label}</string>
    <string name="setup_guide_code_input_title">{code_input_title}</string>'''

for lang_dir, (qr_scan_title, qr_scan_cmd_label, code_input_title) in translations.items():
    filepath = os.path.join(BASE, lang_dir, 'strings.xml')
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # すでに追加済みなら skip
    if 'setup_guide_qr_scan_title' in content:
        print(f'  スキップ（既存）: {lang_dir}')
        continue

    # qr_scan_prompt の後か、</resources> の前に追加
    new_strings = new_strings_template.format(
        qr_scan_title=qr_scan_title,
        qr_scan_cmd_label=qr_scan_cmd_label,
        code_input_title=code_input_title
    )

    if '<string name="qr_scan_desc">' in content:
        # qr_scan_desc の行の後に挿入
        content = content.replace(
            '    <string name="qr_scan_desc">',
            new_strings + '\n    <string name="qr_scan_desc">'
        )
    else:
        # </resources> の前に挿入
        content = content.replace('</resources>', new_strings + '\n</resources>')

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'  追加完了: {lang_dir}')

print('完了!')
