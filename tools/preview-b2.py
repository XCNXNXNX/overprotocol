"""Render the exported game mesh, without starting/controlling Minecraft.

Run tools/dev.ps1 exportB2Model first, then python tools/preview-b2.py.
Studio lighting is for geometry inspection; it is not a Minecraft screenshot.
"""
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
def load(path):
    global vertices, normals, faces
    vertices, normals, faces = [], [], []
    color, group = (80, 88, 98), ''
    for line in path.read_text(encoding="utf-8").splitlines():
        fields = line.split()
        if not fields:
            continue
        if fields[0] == 'v':
            vertices.append(tuple(map(float, fields[1:])))
        elif fields[0] == 'vn':
            normals.append(tuple(map(float, fields[1:])))
        elif fields[0] == 'g':
            group = fields[1]
        elif fields[0] == 'usemtl':
            packed = int(fields[1].split('_')[1], 16)
            color = ((packed >> 16) & 255, (packed >> 8) & 255, packed & 255)
        elif fields[0] == 'f':
            indices = [tuple(map(int, p.split('//'))) for p in fields[1:]]
            faces.append((indices, color, group))
    vertices, normals = np.array(vertices), np.array(normals)
    assert np.isfinite(vertices).all() and np.isfinite(normals).all()


def unit(vector):
    value = np.array(vector, dtype=float)
    return value / np.linalg.norm(value)


def render(width, height, view, scale, centre, gear=True):
    view = unit(view)
    right = unit(np.cross([0, 1, 0], view)) if abs(view[1]) < .999 else np.array([-1, 0, 0])
    up = np.cross(view, right)
    q = vertices - centre
    projected = np.stack([width / 2 + q @ right * scale,
                          height / 2 - q @ up * scale, q @ view], axis=1)
    pixels = np.full((height, width, 3), (229, 232, 235), dtype=np.uint8)
    depth = np.full((height, width), -1.e9)
    light = unit([-.48, 1 if view[1] >= 0 else -1, -.55])
    halfway = unit(view + light)
    for indices, color, group in faces:
        if not gear and group not in ('airframe', 'lights'):
            continue
        vi = np.array([i - 1 for i, _ in indices])
        ni = np.array([i - 1 for _, i in indices])
        p, ns = projected[vi], normals[ni]
        if ns.mean(axis=0) @ view < -.08:
            continue
        for ids in ((0, 1, 2), (0, 2, 3)):
            a, b, c = p[list(ids)]
            lo = np.maximum(np.floor(np.min([a[:2], b[:2], c[:2]], axis=0)).astype(int), [0, 0])
            hi = np.minimum(np.ceil(np.max([a[:2], b[:2], c[:2]], axis=0)).astype(int), [width-1, height-1])
            det = (b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
            if abs(det) < 1.e-8 or np.any(lo > hi):
                continue
            xx, yy = np.meshgrid(np.arange(lo[0], hi[0]+1)+.5, np.arange(lo[1], hi[1]+1)+.5)
            wa = ((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/det
            wb = ((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/det
            wc = 1-wa-wb
            zz = wa*a[2]+wb*b[2]+wc*c[2]
            local = depth[lo[1]:hi[1]+1, lo[0]:hi[0]+1]
            mask = (wa >= -1.e-5) & (wb >= -1.e-5) & (wc >= -1.e-5) & (zz > local)
            if not mask.any():
                continue
            n = wa[..., None]*ns[ids[0]]+wb[..., None]*ns[ids[1]]+wc[..., None]*ns[ids[2]]
            n /= np.maximum(np.linalg.norm(n, axis=2, keepdims=True), 1.e-8)
            diffuse = np.maximum(0, n @ light)
            gloss = np.maximum(0, n @ halfway)**28
            rgb = np.clip(np.array(color)*(0.52+0.48*diffuse[..., None])+gloss[..., None]*12, 0, 255).astype(np.uint8)
            pixels[lo[1]:hi[1]+1, lo[0]:hi[0]+1][mask] = rgb[mask]
            local[mask] = zz[mask]
    return Image.fromarray(pixels)


if __name__ == '__main__':
    import sys
    if '--gear' in sys.argv:
        canvas = Image.new('RGB', (1800, 1080), (229, 232, 235))
        cases = [('b2-spirit.obj', 'EXTENDED / fixed hinges and connected struts'),
                 ('b2-gear-folding.obj', 'FOLDING / doors fully open'),
                 ('b2-gear-doors.obj', 'DOORS CLOSING / wheels already inside'),
                 ('b2-gear-stowed.obj', 'STOWED / flush belly doors')]
        for i, (name, label) in enumerate(cases):
            load(ROOT/'art'/name)
            pos = ((i % 2)*900, (i // 2)*540)
            panel = render(900, 510, [.18, -.65, -1], 48, [0, 1.6, -2.8])
            canvas.paste(panel, (pos[0], pos[1]+30))
            ImageDraw.Draw(canvas).text((pos[0]+18, pos[1]+8), label, fill=(40,52,65),
                                       font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',18))
        canvas.save(ROOT/'art/b2-gear-preview.png')
        print('Saved art/b2-gear-preview.png')
    else:
        load(ROOT/'art/b2-spirit.obj')
        dimensions = np.ptp(vertices, axis=0)
        assert abs(dimensions[0]-52.12)<.02 and abs(dimensions[2]-20.9)<.02
        canvas = Image.new('RGB', (1800, 1340), (229, 232, 235))
        canvas.paste(render(1800, 650, [0, 1, 0], 30, [0, 2.5, 0], False), (0, 25))
        canvas.paste(render(1150, 590, [.70, .85, -1.10], 20, [0, 2.7, 0], True), (0, 725))
        canvas.paste(render(650, 260, [1, .045, 0], 25, [0, 2.7, 0], True), (1150, 780))
        canvas.paste(render(650, 220, [0, .055, -1], 11, [0, 2.7, 0], True), (1150, 1090))
        draw = ImageDraw.Draw(canvas)
        try:
            font = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 21)
        except OSError:
            font = ImageFont.load_default()
        for pos, label in [((30, 18), 'TOP / actual exported game mesh'), ((30, 720), 'FRONT QUARTER / gear extended'),
                           ((1170, 755), 'SIDE'), ((1170, 1060), 'FRONT')]:
            draw.text(pos, label, fill=(40, 52, 65), font=font)
        output = ROOT / 'art/b2-model-preview.png'
        canvas.save(output)
        report = {'dimensions_xyz': dimensions.tolist(), 'vertices': len(vertices), 'faces': len(faces),
                  'finite': True, 'preview': str(output)}
        (ROOT/'art/b2-model-check.json').write_text(json.dumps(report, indent=2))
        print(json.dumps(report))
