// Renders the generated Overprotocol block models to PNG so they can be eyeballed without
// launching the game.  Usage:  node tools/preview-guard.mjs [pose ...]
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { inflateSync, deflateSync } from 'node:zlib';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = join(dirname(fileURLToPath(import.meta.url)), '..');
const genRoot = join(projectRoot, 'src', 'generated', 'resources', 'assets', 'overprotocol');
const mainRoot = join(projectRoot, 'src', 'main', 'resources', 'assets', 'overprotocol');

// ---------------------------------------------------------------- PNG codec
const crcTable = (() => {
  const table = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c;
  }
  return table;
})();
const crc32 = (buf) => {
  let c = -1;
  for (const byte of buf) c = crcTable[(c ^ byte) & 0xff] ^ (c >>> 8);
  return (c ^ -1) >>> 0;
};
const chunk = (type, data) => {
  const out = Buffer.alloc(data.length + 12);
  out.writeUInt32BE(data.length, 0);
  out.write(type, 4, 'ascii');
  data.copy(out, 8);
  out.writeUInt32BE(crc32(Buffer.concat([Buffer.from(type, 'ascii'), data])), data.length + 8);
  return out;
};
function encodePng(width, height, rgba) {
  const raw = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y++) {
    raw[y * (width * 4 + 1)] = 0;
    rgba.copy(raw, y * (width * 4 + 1) + 1, y * width * 4, (y + 1) * width * 4);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0); ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk('IHDR', ihdr), chunk('IDAT', deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}
function decodePng(file) {
  const buf = readFileSync(file);
  let pos = 8, width = 0, height = 0, colorType = 6, bitDepth = 8;
  const idat = [];
  while (pos < buf.length) {
    const len = buf.readUInt32BE(pos);
    const type = buf.toString('ascii', pos + 4, pos + 8);
    const data = buf.subarray(pos + 8, pos + 8 + len);
    if (type === 'IHDR') {
      width = data.readUInt32BE(0); height = data.readUInt32BE(4);
      bitDepth = data[8]; colorType = data[9];
    } else if (type === 'IDAT') idat.push(data);
    else if (type === 'IEND') break;
    pos += 12 + len;
  }
  if (bitDepth !== 8) throw new Error('only 8-bit PNGs are supported: ' + file);
  const bpp = colorType === 6 ? 4 : colorType === 2 ? 3 : colorType === 0 ? 1 : 4;
  const raw = inflateSync(Buffer.concat(idat));
  const stride = width * bpp;
  const out = Buffer.alloc(width * height * 4);
  let prev = Buffer.alloc(stride);
  for (let y = 0; y < height; y++) {
    const filter = raw[y * (stride + 1)];
    const line = Buffer.from(raw.subarray(y * (stride + 1) + 1, y * (stride + 1) + 1 + stride));
    for (let i = 0; i < stride; i++) {
      const a = i >= bpp ? line[i - bpp] : 0, b = prev[i], c = i >= bpp ? prev[i - bpp] : 0;
      if (filter === 1) line[i] = (line[i] + a) & 0xff;
      else if (filter === 2) line[i] = (line[i] + b) & 0xff;
      else if (filter === 3) line[i] = (line[i] + ((a + b) >> 1)) & 0xff;
      else if (filter === 4) {
        const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
        line[i] = (line[i] + (pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 0xff;
      }
    }
    for (let x = 0; x < width; x++) {
      const s = x * bpp, d = (y * width + x) * 4;
      if (colorType === 6) { out[d] = line[s]; out[d + 1] = line[s + 1]; out[d + 2] = line[s + 2]; out[d + 3] = line[s + 3]; }
      else if (colorType === 2) { out[d] = line[s]; out[d + 1] = line[s + 1]; out[d + 2] = line[s + 2]; out[d + 3] = 255; }
      else { out[d] = out[d + 1] = out[d + 2] = line[s]; out[d + 3] = 255; }
    }
    prev = line;
  }
  return { width, height, data: out };
}

// --------------------------------------------------------------- model data
const textureCache = new Map();
function loadTexture(reference) {
  if (textureCache.has(reference)) return textureCache.get(reference);
  const path = reference.replace(/^overprotocol:/, '');
  for (const root of [mainRoot, genRoot]) {
    try { const png = decodePng(join(root, 'textures', path + '.png')); textureCache.set(reference, png); return png; }
    catch { /* try the next root */ }
  }
  textureCache.set(reference, null);
  return null;
}

// Corner order matches FaceBakery: index 0..3 carry UV (u1,v1) (u2,v1) (u2,v2) (u1,v2).
// Side faces keep u horizontal and v vertical, exactly like an auto-UV'd cube.
const CORNERS = {
  down: (b) => [[b[0], b[1], b[2]], [b[3], b[1], b[2]], [b[3], b[1], b[5]], [b[0], b[1], b[5]]],
  up: (b) => [[b[0], b[4], b[2]], [b[3], b[4], b[2]], [b[3], b[4], b[5]], [b[0], b[4], b[5]]],
  north: (b) => [[b[3], b[4], b[2]], [b[0], b[4], b[2]], [b[0], b[1], b[2]], [b[3], b[1], b[2]]],
  south: (b) => [[b[0], b[4], b[5]], [b[3], b[4], b[5]], [b[3], b[1], b[5]], [b[0], b[1], b[5]]],
  west: (b) => [[b[0], b[4], b[2]], [b[0], b[4], b[5]], [b[0], b[1], b[5]], [b[0], b[1], b[2]]],
  east: (b) => [[b[3], b[4], b[5]], [b[3], b[4], b[2]], [b[3], b[1], b[2]], [b[3], b[1], b[5]]],
};
const NORMALS = { down: [0, -1, 0], up: [0, 1, 0], north: [0, 0, -1], south: [0, 0, 1], west: [-1, 0, 0], east: [1, 0, 0] };

function collectQuads(modelFile, yOffset, offset = [0, 0, 0]) {
  const model = JSON.parse(readFileSync(modelFile, 'utf8'));
  const textures = model.textures ?? {};
  const resolve = (ref) => (ref?.startsWith('#') ? resolve(textures[ref.slice(1)]) : ref);
  const quads = [];
  for (const element of model.elements ?? []) {
    const from = element.from, to = element.to;
    const bounds = [from[0] + offset[0], from[1] + yOffset + offset[1], from[2] + offset[2],
      to[0] + offset[0], to[1] + yOffset + offset[1], to[2] + offset[2]];
    for (const [face, data] of Object.entries(element.faces ?? {})) {
      const png = loadTexture(resolve(data.texture));
      const uv = data.uv ?? [0, 0, 16, 16];
      quads.push({ face, corners: CORNERS[face](bounds), normal: NORMALS[face], uv, png,
        shade: face === 'up' ? 1.0 : face === 'down' ? 0.5 : face === 'north' || face === 'south' ? 0.8 : 0.6 });
    }
  }
  return quads;
}

// --------------------------------------------------------------- rendering
const normalize = (v) => { const l = Math.hypot(...v); return v.map((c) => c / l); };
const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];

function render(quads, { width, height, scale, centre, view }) {
  const v = normalize(view);
  const r = normalize(cross([0, 1, 0], v));
  const u = cross(v, r);
  const px = Buffer.alloc(width * height * 4);
  for (let i = 0; i < width * height; i++) { px[i * 4 + 3] = 255; }
  const project = (p) => {
    const q = [p[0] - centre[0], p[1] - centre[1], p[2] - centre[2]];
    return [width / 2 + dot(q, r) * scale, height / 2 - dot(q, u) * scale, dot(q, v)];
  };
  const visible = quads
    .map((quad) => ({ quad, projected: quad.corners.map(project) }))
    .filter((entry) => dot(entry.quad.normal, v) > 0.001)
    .map((entry) => ({ ...entry, depth: entry.projected.reduce((sum, p) => sum + p[2], 0) / 4 }))
    .sort((a, b) => a.depth - b.depth);

  const SS = 3;
  for (const { quad, projected } of visible) {
    const [c0, c1, , c3] = projected;
    const ex = [c1[0] - c0[0], c1[1] - c0[1]];
    const ey = [c3[0] - c0[0], c3[1] - c0[1]];
    const det = ex[0] * ey[1] - ex[1] * ey[0];
    if (Math.abs(det) < 1e-6) continue;
    const xs = projected.map((p) => p[0]), ys = projected.map((p) => p[1]);
    const x0 = Math.max(0, Math.floor(Math.min(...xs))), x1 = Math.min(width - 1, Math.ceil(Math.max(...xs)));
    const y0 = Math.max(0, Math.floor(Math.min(...ys))), y1 = Math.min(height - 1, Math.ceil(Math.max(...ys)));
    const [u1, v1, u2, v2] = quad.uv;
    for (let y = y0; y <= y1; y++) {
      for (let x = x0; x <= x1; x++) {
        let hits = 0, rs = 0, gs = 0, bs = 0, as = 0;
        for (let sy = 0; sy < SS; sy++) {
          for (let sx = 0; sx < SS; sx++) {
            const dx = x + (sx + 0.5) / SS - c0[0], dy = y + (sy + 0.5) / SS - c0[1];
            const a = (dx * ey[1] - dy * ey[0]) / det;
            const b = (ex[0] * dy - ex[1] * dx) / det;
            if (a < 0 || a > 1 || b < 0 || b > 1 || !quad.png) continue;
            const tu = Math.min(quad.png.width - 1, Math.max(0, Math.floor(u1 + a * (u2 - u1))));
            const tv = Math.min(quad.png.height - 1, Math.max(0, Math.floor(v1 + b * (v2 - v1))));
            const s = (tv * quad.png.width + tu) * 4;
            rs += quad.png.data[s]; gs += quad.png.data[s + 1]; bs += quad.png.data[s + 2];
            as += quad.png.data[s + 3]; hits++;
          }
        }
        if (!hits || as / hits < 128) continue;
        const d = (y * width + x) * 4, shade = quad.shade;
        px[d] = Math.min(255, Math.round((rs / hits) * shade));
        px[d + 1] = Math.min(255, Math.round((gs / hits) * shade));
        px[d + 2] = Math.min(255, Math.round((bs / hits) * shade));
      }
    }
  }
  return px;
}

// -------------------------------------------------------------- player model
// Mirrors vanilla ModelPart.Cube box unwrapping for a w x h x d box at texture (u,v).
function boxUv(u, v, w, h, d) {
  return {
    minY: [u + d, v, u + d + w, v + d],
    maxY: [u + d + w, v, u + d + 2 * w, v + d],
    minX: [u, v + d, u + d, v + d + h],
    minZ: [u + d, v + d, u + d + w, v + d + h],
    maxX: [u + d + w, v + d, u + 2 * d + w, v + d + h],
    maxZ: [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h],
  };
}
// Corners in (u1,v1) (u2,v1) (u2,v2) (u1,v2) order; model +Y points down, so v grows with +Y.
const FACE_CORNERS = {
  minY: (b) => [[b[0], b[1], b[2]], [b[3], b[1], b[2]], [b[3], b[1], b[5]], [b[0], b[1], b[5]]],
  maxY: (b) => [[b[0], b[4], b[2]], [b[3], b[4], b[2]], [b[3], b[4], b[5]], [b[0], b[4], b[5]]],
  minX: (b) => [[b[0], b[1], b[5]], [b[0], b[1], b[2]], [b[0], b[4], b[2]], [b[0], b[4], b[5]]],
  maxX: (b) => [[b[3], b[1], b[2]], [b[3], b[1], b[5]], [b[3], b[4], b[5]], [b[3], b[4], b[2]]],
  minZ: (b) => [[b[0], b[1], b[2]], [b[3], b[1], b[2]], [b[3], b[4], b[2]], [b[0], b[4], b[2]]],
  maxZ: (b) => [[b[3], b[1], b[5]], [b[0], b[1], b[5]], [b[0], b[4], b[5]], [b[3], b[4], b[5]]],
};
const FACE_NORMALS = { minY: [0, -1, 0], maxY: [0, 1, 0], minX: [-1, 0, 0], maxX: [1, 0, 0], minZ: [0, 0, -1], maxZ: [0, 0, 1] };
const FACE_SHADE = { minY: 1.0, maxY: 0.5, minX: 0.6, maxX: 0.6, minZ: 0.8, maxZ: 0.8 };

function rotateModelPoint(pt, pivot, rotations) {
  let [x, y, z] = pt;
  for (const r of rotations) {
    const dx = x - pivot[0], dy = y - pivot[1], dz = z - pivot[2];
    const c = Math.cos(r.angle), s = Math.sin(r.angle);
    let nx, ny, nz;
    if (r.axis === 'x') { nx = dx; ny = dy * c - dz * s; nz = dy * s + dz * c; }
    else if (r.axis === 'y') { nx = dx * c + dz * s; ny = dy; nz = -dx * s + dz * c; }
    else { nx = dx * c - dy * s; ny = dx * s + dy * c; nz = dz; }
    x = nx + pivot[0]; y = ny + pivot[1]; z = nz + pivot[2];
  }
  return [x, y, z];
}
function rotateNormal(n, rotations) {
  return rotateModelPoint(n, [0, 0, 0], rotations);
}

/** Replicates translate(0.5,0,0.5) -> rotateY(180 - yRot) -> scale(-1,-1,1) -> translate(0,-1.501,0). */
function playerToWorld(pt) {
  const t = [pt[0] / 16, pt[1] / 16 - 1.501, pt[2] / 16];
  const s = [-t[0], -t[1], t[2]];
  const th = Math.PI;
  const c = Math.cos(th), sn = Math.sin(th);
  const ry = [c * s[0] + sn * s[2], s[1], -sn * s[0] + c * s[2]];
  return [(ry[0] + 0.5) * 16, ry[1] * 16, (ry[2] + 0.5) * 16];
}

function playerModelQuads(skin, pose) {
  const quads = [];
  const parts = [
    { name: 'head', box: [-4, -8, -4, 4, 0, 4], uv: [0, 0], rot: [] },
    // the hat overlay is grown by 0.5 but keeps the head's 8x8x8 texture unwrap
    { name: 'hat', box: [-4.5, -8.5, -4.5, 4.5, 0.5, 4.5], uv: [32, 0], uvSize: [8, 8, 8], rot: [] },
    { name: 'visor', box: [-4, -8.2, -5.4, 4, -6.6, -4], uv: [56, 16], uvSize: [8, 2, 2], rot: [] },
    { name: 'body', box: [-4, 0, -2, 4, 12, 2], uv: [16, 16], rot: [] },
    { name: 'rightArm', box: [-8, 0, -2, -4, 12, 2], uv: [40, 16], pivot: [-5, 2, 0], rot: pose.rightArm ?? [] },
    { name: 'leftArm', box: [4, 0, -2, 8, 12, 2], uv: [32, 48], pivot: [5, 2, 0], rot: pose.leftArm ?? [] },
    { name: 'rightLeg', box: [-3.9, 12, -2, 0.1, 24, 2], uv: [0, 16], rot: [] },
    { name: 'leftLeg', box: [-0.1, 12, -2, 3.9, 24, 2], uv: [16, 48], rot: [] },
  ];
  for (const extra of pose.extras ?? []) {
    parts.push({
      ...extra, pivot: extra.pivot ?? [-5, 2, 0], rot: extra.rot ?? pose.rightArm ?? [],
      tex: loadTexture('overprotocol:' + extra.tex),
    });
  }
  for (const part of parts) {
    const [x0, y0, z0, x1, y1, z1] = part.box;
    const w = x1 - x0, h = y1 - y0, d = z1 - z0;
    const [uw, uh, ud] = part.uvSize ?? [w, h, d];
    const rects = part.uvAll ? null : boxUv(part.uv[0], part.uv[1], uw, uh, ud);
    const pivot = part.pivot ?? [0, 0, 0];
    for (const face of Object.keys(FACE_CORNERS)) {
      const uv = part.uvAll ?? rects[face];
      const corners = FACE_CORNERS[face](part.box).map((pt) => playerToWorld(rotateModelPoint(pt, pivot, part.rot)));
      const normal = rotateNormal(FACE_NORMALS[face], part.rot);
      quads.push({ face, corners, normal, uv, png: part.tex ?? skin, shade: FACE_SHADE[face] });
    }
  }
  return quads;
}

// ------------------------------------------------------------------- driver
const args = process.argv.slice(2);
const CELL = 200, scale = 5.4;
const outDir = join(projectRoot, 'art');
mkdirSync(outDir, { recursive: true });

function writeSheet(cells, views, centre, outName, label) {
  const width = CELL * cells.length, height = CELL * views.length;
  const buf = Buffer.alloc(width * height * 4);
  for (let i = 0; i < width * height; i++) buf[i * 4 + 3] = 255;
  cells.forEach((quads, index) => {
    views.forEach((entry, row) => {
      const cell = render(quads, { width: CELL, height: CELL, scale, centre, view: entry.view });
      for (let y = 0; y < CELL; y++) {
        cell.copy(buf, ((row * CELL + y) * width + index * CELL) * 4, y * CELL * 4, (y + 1) * CELL * 4);
      }
    });
  });
  const file = join(outDir, outName);
  writeFileSync(file, encodePng(width, height, buf));
  console.log('wrote ' + file + ' (' + label + ')');
}

const THREE_QUARTER = { name: 'three-quarter', view: [1, 0.75, 1.35] };
const FRONT = { name: 'front', view: [0, 0.06, 1] };
const SIDE = { name: 'side', view: [1, 0.06, 0] };

if (args[0] === 'skin') {
  const skinPng = decodePng(join(mainRoot, 'textures', 'entity', 'honor_guard.png'));
  const ARM_DOWN = [];
  // mirrors HonorGuardRenderer.applyPose
  const poses = {
    attention: { rightArm: ARM_DOWN, leftArm: ARM_DOWN },
    salute: { rightArm: [{ axis: 'z', angle: 0.35 }, { axis: 'x', angle: -2.5 }], leftArm: ARM_DOWN },
    present: { rightArm: [{ axis: 'z', angle: 0.1 }, { axis: 'x', angle: -1.25 }], leftArm: ARM_DOWN },
    raise: { rightArm: [{ axis: 'z', angle: 0.15 }, { axis: 'x', angle: -2.95 }], leftArm: ARM_DOWN },
  };
  const names = args.length > 1 ? args.slice(1) : ['attention', 'salute', 'present', 'raise'];
  const cells = names.map((name) => playerModelQuads(skinPng, poses[name]));
  writeSheet(cells, [THREE_QUARTER, FRONT, SIDE], [0, 8, 0], 'guard-model-preview.png', names.join(', '));
} else if (args[0] === 'scene') {
  // a straight run of three barriers plus one guard, to check that neighbours line up
  const quads = [
    ...collectQuads(join(genRoot, 'models', 'block', 'ceremonial_rope_4.json'), 0, [0, 0, 0]),
    ...collectQuads(join(genRoot, 'models', 'block', 'ceremonial_rope_5.json'), 0, [0, 0, 16]),
    ...collectQuads(join(genRoot, 'models', 'block', 'ceremonial_rope_1.json'), 0, [0, 0, 32]),
    ...collectQuads(join(genRoot, 'models', 'block', 'ceremonial_rope_2.json'), 0, [16, 0, 16]),
  ];
  const W = 520, H = 300;
  for (const [name, view] of [['three-quarter', [1, 0.55, 1.1]], ['front', [0, 0.1, 1]], ['top', [0.2, 1.4, 0.35]]]) {
    const img = render(quads, { width: W, height: H, scale: 6.0, centre: [12, 8, 20], view });
    writeFileSync(join(outDir, 'rope-scene-' + name + '.png'), encodePng(W, H, img));
  }
  console.log('wrote rope-scene-*.png');
} else if (args[0] === 'rope') {
  const masks = args.length > 1 ? args.slice(1).map(Number) : [0, 1, 2, 5, 10, 15];
  const cells = masks.map((mask) => collectQuads(join(genRoot, 'models', 'block', 'ceremonial_rope_' + mask + '.json'), 0));
  writeSheet(cells, [THREE_QUARTER, FRONT], [8, 8, 8], 'ceremonial-rope-preview.png', 'rope masks ' + masks.join(', '));
} else {
  const poses = args.length ? args : ['attention', 'salute', 'rifle', 'flag'];
  const cells = poses.map((pose) => [
    ...collectQuads(join(genRoot, 'models', 'block', 'honor_guard_' + pose + '_lower.json'), 0),
    ...collectQuads(join(genRoot, 'models', 'block', 'honor_guard_' + pose + '_upper.json'), 16),
  ]);
  writeSheet(cells, [THREE_QUARTER, FRONT, SIDE], [8, 17, 8], 'honor-guard-preview.png', poses.join(', '));
}