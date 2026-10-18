import json

with open('brand_exports/Loading_Screen.json', 'r', encoding='utf-8') as f:
    d = json.load(f)

print(f"Canvas: {d.get('w')}x{d.get('h')}, fr={d.get('fr')}, ip={d.get('ip')}, op={d.get('op')}")
print("Assets:")
for a in d.get('assets', []):
    print(" ", a)

print("\nLayers in brand_exports/Loading_Screen.json:")
for idx, l in enumerate(d.get('layers', [])):
    nm = l.get('nm')
    ind = l.get('ind')
    ty = l.get('ty')
    ip = l.get('ip')
    op = l.get('op')
    st = l.get('st')
    bm = l.get('bm')
    parent = l.get('parent')
    tt = l.get('tt')
    td = l.get('td')
    shapes = [s.get('nm') for s in l.get('shapes', [])]
    print(f"[{idx:2d}] ind={ind:3d} ty={ty} nm={nm:15s} ip={ip:3d} op={op:3d} st={st:3d} bm={bm} parent={parent} tt={tt} td={td} shapes={shapes}")
