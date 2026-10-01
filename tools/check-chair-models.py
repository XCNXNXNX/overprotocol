"""Audit datagen chair surfaces against the pre-fix solid shape, without running Minecraft.

Checks the complete surface, including partial face overlaps and enclosed/internal faces;
an unchanged bounding box alone would not detect the reported z-fighting or missing panels.
"""
from collections import Counter
from itertools import product
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / 'src/generated/resources/assets/overprotocol/models/block'
STATE_ROOT = ROOT / 'src/generated/resources/assets/overprotocol/blockstates'
REFERENCE = ROOT / 'art/qa/chair-alpha23-reference.json'
FACE_AXIS = {'west': (0, -1), 'east': (0, 1), 'down': (1, -1),
             'up': (1, 1), 'north': (2, -1), 'south': (2, 1)}


def coords(element, key):
    return [round(float(value), 6) for value in element[key]]


def old_overlaps(elements):
    volumes = same_faces = 0
    for i, a in enumerate(elements):
        for b in elements[i + 1:]:
            intersections = [min(a['to'][k], b['to'][k]) - max(a['from'][k], b['from'][k]) for k in range(3)]
            volumes += all(value > 1e-6 for value in intersections)
            for face, (axis, sign) in FACE_AXIS.items():
                edge = 'to' if sign > 0 else 'from'
                same_faces += (face in a['faces'] and face in b['faces']
                               and abs(a[edge][axis] - b[edge][axis]) < 1e-6
                               and all(intersections[k] > 1e-6 for k in range(3) if k != axis))
    return {'overlapping_volumes': volumes, 'same_facing_coplanar_overlaps': same_faces}


def check(model, reference):
    elements = model['elements']
    assert all('rotation' not in element for element in elements), 'Expected axis-aligned chair surface'
    grid = [sorted({coords(element, edge)[axis] for element in reference + elements for edge in ['from', 'to']})
            for axis in range(3)]
    index = [{value: i for i, value in enumerate(edges)} for edges in grid]
    solid = set()
    for cell in product(*(range(len(edges) - 1) for edges in grid)):
        point = [(grid[axis][cell[axis]] + grid[axis][cell[axis] + 1]) / 2 for axis in range(3)]
        if any(all(box['from'][axis] < point[axis] < box['to'][axis] for axis in range(3)) for box in reference):
            solid.add(cell)
    expected = set()
    for cell in solid:
        for axis, sign in FACE_AXIS.values():
            neighbor = list(cell)
            neighbor[axis] += sign
            if tuple(neighbor) not in solid:
                u, v = (axis + 1) % 3, (axis + 2) % 3
                plane = grid[axis][cell[axis] + (1 if sign > 0 else 0)]
                expected.add((axis, sign, plane, cell[u], cell[v]))
    actual = Counter()
    materials = Counter()
    for element in elements:
        assert len(element['faces']) == 1, 'Every exported element must own a single exterior face'
        low, high = coords(element, 'from'), coords(element, 'to')
        for face, spec in element['faces'].items():
            axis, sign = FACE_AXIS[face]
            u, v = (axis + 1) % 3, (axis + 2) % 3
            assert low[axis] == high[axis] and low[u] < high[u] and low[v] < high[v], 'Degenerate or misplaced face'
            assert all(0 <= value <= 16 for value in low + high), 'UV inference can leave the atlas sprite'
            assert ('tintindex' in spec) == (spec['texture'] == '#cloth'), 'Only cloth may be tinted'
            assert spec.get('tintindex', 0) == 0
            materials[spec['texture']] += 1
            for ui in range(index[u][low[u]], index[u][high[u]]):
                for vi in range(index[v][low[v]], index[v][high[v]]):
                    actual[(axis, sign, low[axis], ui, vi)] += 1
    missing, extra = expected - actual.keys(), actual.keys() - expected
    duplicate_cells = sum(value > 1 for value in actual.values())
    assert not missing, f'Missing exterior patches: {len(missing)}'
    assert not extra, f'Internal or protruding patches: {len(extra)}'
    assert not duplicate_cells, f'Overlapping patches: {duplicate_cells}'
    assert set(materials) == {'#wood', '#gold', '#cloth'}, 'All three materials must remain'
    return {'faces': len(elements), 'overlapping_patches': duplicate_cells,
            'missing_patches': len(missing), 'internal_or_extra_patches': len(extra),
            'original_solid_boundary_preserved': True, 'materials': dict(materials)}


def main():
    reference = json.loads(REFERENCE.read_text(encoding='utf-8'))['elements']
    files = sorted(MODEL_ROOT.glob('*_ceremonial_chair.json'))
    assert len(files) == 16, 'Expected every wool colour'
    report = {'reference': str(REFERENCE.relative_to(ROOT)), 'before': old_overlaps(reference), 'models': {}}
    for file in files:
        report['models'][file.stem] = check(json.loads(file.read_text(encoding='utf-8')), reference)
        states = json.loads((STATE_ROOT / file.name).read_text(encoding='utf-8'))['variants']
        assert len(states) == 4 and {spec.get('y', 0) for spec in states.values()} == {0, 90, 180, 270}
        assert all(spec['model'] == 'overprotocol:block/' + file.stem for spec in states.values())
    output = ROOT / 'art/qa/chair-surface-check.json'
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'PASS: {len(files)} colours, 4 orientations; no overlapping, missing or internal surfaces; original silhouette preserved.')
    print('Before:', report['before'], 'After:', next(iter(report['models'].values())))
    print('Report:', output)


if __name__ == '__main__':
    main()
