import json

def check_obj(obj, path=''):
    if isinstance(obj, dict):
        for k, v in obj.items():
            curr_path = f"{path}.{k}" if path else k
            if k == 'c' and not isinstance(v, bool):
                print(f"NON-BOOL 'c': {curr_path} = {repr(v)} (type {type(v).__name__})")
            elif k == 'hd' and not isinstance(v, bool):
                print(f"NON-BOOL 'hd': {curr_path} = {repr(v)} (type {type(v).__name__})")
            check_obj(v, curr_path)
    elif isinstance(obj, list):
        for i, item in enumerate(obj):
            check_obj(item, f"{path}[{i}]")

for p in ['brand_exports/Loading_Screen.json', 'app/src/main/assets/Loading_Screen_fixed.json']:
    print('Checking:', p)
    with open(p) as f:
        d = json.load(f)
    check_obj(d)
