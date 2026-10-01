"""Copy image_gen originals into the project and resize the selected game textures to 64px."""
from pathlib import Path
from PIL import Image
import json, shutil, sys

root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'art/materials-alpha21.json').read_text(encoding='utf-8'))
for asset in manifest['assets']:
    original=root/asset['project_source']
    original.parent.mkdir(parents=True,exist_ok=True)
    if not original.exists():
        shutil.copy2(asset['generated_source'],original)
    image=Image.open(original)
    assert image.width==image.height and image.mode in ('RGB','RGBA')
    destination=root/asset['runtime']
    image.resize(tuple(manifest['runtime_size']),Image.Resampling.LANCZOS).save(destination)
    print(destination.relative_to(root))
