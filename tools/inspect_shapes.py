import json

with open('brand_exports/Loading_Screen.json', 'r', encoding='utf-8') as f:
    d = json.load(f)

for l_idx, l in enumerate(d['layers']):
    print(f"Layer {l_idx}: {l.get('nm')}")
    for s_idx, s in enumerate(l.get('shapes', [])):
        print(f"  Shape {s_idx}: {s.get('nm')} (ty={s.get('ty')})")
        for it_idx, it in enumerate(s.get('it', [])):
            print(f"    it {it_idx}: {it.get('nm')} (ty={it.get('ty')})")
            if 'ks' in it:
                k = it['ks'].get('k')
                if isinstance(k, dict):
                    print(f"      ks.k: c={k.get('c')} (type={type(k.get('c')).__name__})")
                elif isinstance(k, list):
                    print(f"      ks.k (keyframes, len={len(k)}):")
                    for kf_idx, kf in enumerate(k[:2]):
                        s_val = kf.get('s', [{}])[0] if isinstance(kf.get('s'), list) else kf.get('s')
                        if isinstance(s_val, dict):
                            print(f"        kf {kf_idx}: c={s_val.get('c')} (type={type(s_val.get('c')).__name__})")
