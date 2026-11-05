// مجسم دبوس الموقع (شعار صنعة) بثلاثة أبعاد.
// الوحدات بالمليمتر. المقاس النهائي مع الحواف المشطوفة: 31 × 44.6 × 10 مم، وقطر الثقب 7 مم.
import * as THREE from '../vendor/three/three.module.min.js';

export { THREE };

export const DIMENSIONS = { R: 14, tip: 27, hole: 5, depth: 6, bevel: 2 };
export const BRAND = '#0f766e';

// شكل الشعار ثنائي الأبعاد: دائرة يتصل بها رأس مدبب من الأسفل، وفي وسطها ثقب.
function pinShape({ R, tip, hole }) {
  const phi = Math.acos(R / tip);             // زاوية نقطة التماس بين الخط المستقيم والدائرة
  const s = new THREE.Shape();
  // نبدأ من الرأس، والقوس يتصل به تلقائياً بخط مستقيم، والشكل يُغلق نفسه.
  // تكرار النقاط هنا يُحدث فتحات في المجسم تمنع طباعته.
  s.moveTo(0, -tip);
  s.absarc(0, 0, R, -Math.PI / 2 + phi, 3 * Math.PI / 2 - phi, false);
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
  const rim = new THREE.DirectionalLight(0x99f6e4, 1.2);
  rim.position.set(-60, 20, -40);
  scene.add(rim);
}

// دبوس صغير يطفو ويتمايل في الصفحة الرئيسية. يتوقف عند مغادرة الصفحة،
// ويبقى ثابتاً لمن فعّل «تقليل الحركة» في جهازه.
export function mountHero(el) {
  if (!el) return;
  const size = el.clientWidth || 140;
  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
  renderer.setSize(size, size);
  el.appendChild(renderer.domElement);

  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(35, 1, 1, 500);
  camera.position.set(0, 0, 105);
  addLights(scene);
  const pin = buildPin();
  scene.add(pin);

  const still = matchMedia('(prefers-reduced-motion: reduce)').matches;
  let tx = 0, ty = 0;
  el.addEventListener('pointermove', e => {
    const r = el.getBoundingClientRect();
    tx = ((e.clientX - r.left) / r.width - 0.5) * 0.8;
    ty = ((e.clientY - r.top) / r.height - 0.5) * 0.5;
  });
  el.addEventListener('pointerleave', () => { tx = ty = 0; });

  const t0 = performance.now();
  const frame = now => {
    if (!el.isConnected) { renderer.dispose(); pin.geometry.dispose(); pin.material.dispose(); return; }
    const t = (now - t0) / 1000;
    pin.rotation.y = (still ? 0.5 : Math.sin(t * 0.9) * 0.7) + tx;
    pin.rotation.x = ty;
    pin.position.y = still ? 0 : Math.sin(t * 1.6) * 2;
    renderer.render(scene, camera);
    if (!still) requestAnimationFrame(frame);
  };
  requestAnimationFrame(frame);
  if (still) el.addEventListener('pointermove', () => requestAnimationFrame(frame));
}
