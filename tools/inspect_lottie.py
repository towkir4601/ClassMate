import json
import os

files = [
    'brand_exports/Loading_Screen.json',
    'app/src/main/assets/Loading_Screen_fixed.json',
    'app/src/main/res/raw/loading_screen.json',
    'app/src/main/res/raw/sunset_loading_screen.json'
]

for f in files:
    print('='*60)
    print(f)
    if not os.path.exists(f):
        print("DOES NOT EXIST")
        continue
    try:
        with open(f, 'r', encoding='utf-8') as fp:
            d = json.load(fp)
        print(f"w={d.get('w')}, h={d.get('h')}, fr={d.get('fr')}, ip={d.get('ip')}, op={d.get('op')}")
        print("Assets:")
        for a in d.get('assets', []):
            print(f"  id={a.get('id')} w={a.get('w')} h={a.get('h')} u={a.get('u')} p={a.get('p')} layers={len(a.get('layers', [])) if 'layers' in a else None}")
        print(f"Layers count: {len(d.get('layers', []))}")
        for idx, l in enumerate(d.get('layers', [])):
            nm = l.get('nm')
            ind = l.get('ind')
            ty = l.get('ty')
            ip = l.get('ip')
            op = l.get('op')
            st = l.get('st')
            bm = l.get('bm')
            parent = l.get('parent')
            has_masks = bool(l.get('masksProperties'))
            tt = l.get('tt')
            td = l.get('td')
            refId = l.get('refId')
            # Check opacity
            opacity = None
            if 'ks' in l and 'o' in l['ks']:
                opacity = l['ks']['o'].get('k')
            print(f"  [{idx}] ind={ind} ty={ty} nm=\"{nm}\" refId={refId} ip={ip} op={op} st={st} bm={bm} opac={opacity} parent={parent} masks={has_masks} tt={tt} td={td}")
    except Exception as e:
        print('Error:', e)
