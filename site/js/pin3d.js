// مجسم دبوس الموقع (شعار صنعة) بثلاثة أبعاد.
// الوحدات بالمليمتر. المقاس النهائي مع الحواف المشطوفة: 31 × 44.6 × 10 مم، وقطر الثقب 7 مم.
import * as THREE from '../vendor/three/three.module.min.js';

export { THREE };

export const DIMENSIONS = { R: 14, tip: 27, hole: 5, depth: 6, bevel: 2 };
export const BRAND = '#ffc21a';

// شكل الشعار ثنائي الأبعاد: دائرة يتصل بها رأس مدبب من الأسفل، وفي وسطها ثقب.
function pinShape({ R, tip, hole }) {
  const phi = Math.acos(R / tip);             // زاوية نقطة التماس بين الخط المستقيم والدائرة
  const s = new THREE.Shape();
  // نبدأ من نقطة التماس اليمنى، والقوس يبدأ منها تماماً فلا تتكرر النقطة.
  // ثم نصل نهاية القوس (التماس اليسرى) بالرأس، ويُغلق الشكل تلقائياً بخط عودة إلى البداية.
  // ملاحظة: لا نكتب moveTo(الرأس) ثم absarc، لأن Three.js يُسقط نقطة moveTo قبل أول منحنى.
  s.moveTo(R * Math.cos(-Math.PI / 2 + phi), R * Math.sin(-Math.PI / 2 + phi));
  s.absarc(0, 0, R, -Math.PI / 2 + phi, 3 * Math.PI / 2 - phi, false);
  s.lineTo(0, -tip);
  const h = new THREE.Path();
  h.absarc(0, 0, hole, 0, Math.PI * 2, true);
  s.holes.push(h);
  return s;
}

export function buildPin(color = BRAND) {
  const d = DIMENSIONS;
  const geo = new THREE.ExtrudeGeometry(pinShape(d), {
    depth: d.depth, bevelEnabled: true, bevelThickness: d.bevel, bevelSize: 1.5,
    bevelSegments: 5, curveSegments: 64
  });
  geo.center();
  geo.computeVertexNormals();
  const mat = new THREE.MeshStandardMaterial({ color, metalness: 0.25, roughness: 0.35 });
  const mesh = new THREE.Mesh(geo, mat);
  mesh.name = 'MahammPin';
  return mesh;
}

export function addLights(scene) {
  scene.add(new THREE.HemisphereLight(0xffffff, 0x334155, 1.6));
  const key = new THREE.DirectionalLight(0xffffff, 2.2);
  key.position.set(40, 60, 80);
  scene.add(key);
  const rim = new THREE.DirectionalLight(0xbcd2ff, 1.2);
  rim.position.set(-60, 20, -40);
  scene.add(rim);
}
