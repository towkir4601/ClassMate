import json
import os
import shutil
import zipfile

def fix_lottie():
    src_path = 'brand_exports/Loading_Screen.json'
    with open(src_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # 1. Update asset reference to bg_1080x1920.webp
    data['assets'] = [
        {
            "id": "bg_image",
            "w": 1080,
            "h": 1920,
            "u": "",
            "p": "bg_1080x1920.webp",
            "e": 0
        }
    ]

    # Find each layer by name
    layers_by_name = {l['nm']: l for l in data['layers']}

    # Fix boolean issues in all layers
    # Specifically 'c' in shape paths: Moshi parser requires boolean true/false, not integer 1/0
    for l in data['layers']:
        for s in l.get('shapes', []):
            for it in s.get('it', []):
                if 'ks' in it and isinstance(it['ks'], dict):
                    k = it['ks'].get('k')
                    if isinstance(k, dict) and 'c' in k:
                        k['c'] = bool(k['c'])
                    elif isinstance(k, list):
                        for kf in k:
                            if isinstance(kf, dict):
                                s_val = kf.get('s')
                                if isinstance(s_val, list) and len(s_val) > 0 and isinstance(s_val[0], dict) and 'c' in s_val[0]:
                                    s_val[0]['c'] = bool(s_val[0]['c'])
                                elif isinstance(s_val, dict) and 'c' in s_val:
                                    s_val['c'] = bool(s_val['c'])

    # Desired visual stacking order from Back to Front:
    # Background image (BACKMOST)
    # Sun Shimmer
    # Flying sparrow flock (Bird50 to Bird57)
    # Loading... (Loading Text)
    # Loading dots (Dot1, Dot2, Dot3)
    # Loading ring / other foreground elements (Loading Ring, FRONTMOST)
    #
    # In Lottie compositing (Lottie-Android, Lottie-iOS, Bodymovin):
    # layers are rendered in reverse order (from index len-1 down to 0).
    # Therefore:
    # layers[len - 1] is drawn FIRST  -> BACKMOST layer
    # layers[0]       is drawn LAST   -> FRONTMOST layer

    ordered_layer_names = [
        # Frontmost (index 0..4)
        'Loading Ring',
        'Dot3',
        'Dot2',
        'Dot1',
        'Loading Text',
        # Sparrows flock (index 5..12)
        'Bird57',
        'Bird56',
        'Bird55',
        'Bird54',
        'Bird53',
        'Bird52',
        'Bird51',
        'Bird50',
        # Ambient sun shimmer (index 13)
        'Sun Shimmer',
        # Backmost (index 14)
        'Background'
    ]

    new_layers = []
    for idx, nm in enumerate(ordered_layer_names):
        l = layers_by_name[nm]
        # Assign 1-based index according to After Effects standard (1 = top/frontmost)
        l['ind'] = idx + 1
        l['bm'] = 0  # Normal blend mode
        l['ip'] = 0
        l['op'] = 120
        l['st'] = 0
        # If no parent / matte / masks, omit them completely (do not write null)
        l.pop('parent', None)
        l.pop('tt', None)
        l.pop('td', None)
        l.pop('masksProperties', None)
        new_layers.append(l)

    data['layers'] = new_layers

    # Verify composition properties
    data['w'] = 1080
    data['h'] = 1920
    data['fr'] = 30
    data['ip'] = 0
    data['op'] = 120

    # Verify no None/null values exist
    def check_no_nulls(obj, path=''):
        if isinstance(obj, dict):
            for k, v in list(obj.items()):
                if v is None:
                    raise ValueError(f"Found None value at {path}.{k}")
                check_no_nulls(v, f"{path}.{k}")
        elif isinstance(obj, list):
            for i, v in enumerate(obj):
                if v is None:
                    raise ValueError(f"Found None value at {path}[{i}]")
                check_no_nulls(v, f"{path}[{i}]")

    check_no_nulls(data)
    print("Null check passed: 0 nulls in data")

    # Write out files
    out_paths = [
        'Loading_Screen_fixed.json',
        'app/src/main/assets/Loading_Screen_fixed.json',
        'app/src/main/assets/loading_screen.json',
        'app/src/main/res/raw/loading_screen.json'
    ]

    for p in out_paths:
        os.makedirs(os.path.dirname(p) if os.path.dirname(p) else '.', exist_ok=True)
        with open(p, 'w', encoding='utf-8') as f:
            json.dump(data, f, separators=(',', ':'))
        print(f"Wrote {p} ({os.path.getsize(p)} bytes)")

    # Ensure bg_1080x1920.webp is in root beside Loading_Screen_fixed.json
    shutil.copyfile('app/src/main/assets/bg_1080x1920.webp', 'bg_1080x1920.webp')
    print("Copied bg_1080x1920.webp to root")

    # Create loading_screen_fixed.zip with both files
    zip_path = 'loading_screen_fixed.zip'
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        zf.write('Loading_Screen_fixed.json', 'Loading_Screen_fixed.json')
        zf.write('bg_1080x1920.webp', 'bg_1080x1920.webp')
    print(f"Created {zip_path} ({os.path.getsize(zip_path)} bytes)")

if __name__ == '__main__':
    fix_lottie()
