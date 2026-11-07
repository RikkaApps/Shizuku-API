// مجسّم الكويت ثلاثي الأبعاد: الأرض مرسومة من حدود Natural Earth، وكل مهمة دبوس في موقعها.
// ارتفاع الدبوس يمثّل الميزانية، ولونه يمثّل حالة المهمة. يُستخدم في الصفحة الرئيسية وصفحة المهام.
import * as THREE from '../vendor/three/three.module.min.js';
import { OrbitControls } from '../vendor/three/OrbitControls.js';

const CENTER = { lng: 47.49, lat: 29.31 };   // منتصف حدود الكويت
const K = 60;                                   // وحدة لكل درجة
const COS = Math.cos(CENTER.lat * Math.PI / 180);
const proj = (lng, lat) => [(lng - CENTER.lng) * K * COS, (lat - CENTER.lat) * K];
const LAND_H = 2.2;
// الكويت 100×94 وحدة وقطرها 137؛ نحسب الإطار على القطر (لأن الخريطة تدور) مع هامش للحافة القريبة
const FIT_W = 150, FIT_D = 128;

const token = (name, fb) => getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fb;

function landGeometry(geo) {
  const polys = geo.geometry.type === 'Polygon' ? [geo.geometry.coordinates] : geo.geometry.coordinates;
  const shapes = polys.map(rings => {
    const [outer, ...holes] = rings.map(r => r.map(([lng, lat]) => new THREE.Vector2(...proj(lng, lat))));
    const s = new THREE.Shape(outer);
    holes.forEach(h => s.holes.push(new THREE.Path(h)));
    return s;
  });
  const g = new THREE.ExtrudeGeometry(shapes, { depth: LAND_H, bevelEnabled: true, bevelThickness: .5, bevelSize: .4, bevelSegments: 2 });
  g.rotateX(-Math.PI / 2);                      // الشمال إلى عمق المشهد، والسماكة إلى الأعلى
  return g;
}

// دبوس: ساق رفيعة ورأس كروي. الساق بطول الميزانية.
const STALK = new THREE.CylinderGeometry(.32, .32, 1, 8).translate(0, .5, 0);
const HEAD = new THREE.SphereGeometry(1.6, 20, 14);
const RING = new THREE.RingGeometry(.9, 1.5, 24).rotateX(-Math.PI / 2);

export function mountMap3D(el, tasks, opts = {}) {
  if (!el || !window.KUWAIT_GEO) throw new Error('no-container');
  const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const C = {
    night: token('--night', '#071b20'), night2: token('--night-2', '#0d2a31'), land: token('--land', '#c9b48a'),
    line: token('--night-line', '#1d4049'), gold: token('--gold', '#c98a12'), pri: token('--pri', '#0b7a74'),
    mute: token('--on-night-mute', '#94aeb2'), bad: token('--bad', '#b4232a')
  };
  const STATUS_COLOR = { open: C.gold, assigned: C.pri, delivered: C.pri, disputed: C.bad, completed: C.mute };

  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
  renderer.domElement.setAttribute('aria-hidden', 'true');
  el.prepend(renderer.domElement);

  const scene = new THREE.Scene();
  scene.fog = new THREE.Fog(C.night, 170, 330);
  const camera = new THREE.PerspectiveCamera(36, 1, 1, 1000);
  camera.position.set(0, 118, 128);

  scene.add(new THREE.HemisphereLight(0xdff6f3, 0x0b1f24, 1.5));
  const sun = new THREE.DirectionalLight(0xfff2d6, 2.4);
  sun.position.set(-60, 120, 50);
  scene.add(sun);

  // البحر: شبكة خطوط طول وعرض خافتة، كخرائط الملاحة
  const grid = new THREE.GridHelper(320, 32, C.line, C.line);
  grid.position.y = -.05;
  grid.material.transparent = true; grid.material.opacity = .55;
  scene.add(grid);

  const land = new THREE.Mesh(landGeometry(window.KUWAIT_GEO), new THREE.MeshStandardMaterial({ color: C.land, roughness: .85, metalness: .05 }));
  scene.add(land);
  const edge = new THREE.LineSegments(new THREE.EdgesGeometry(land.geometry, 30), new THREE.LineBasicMaterial({ color: C.night2, transparent: true, opacity: .35 }));
  scene.add(edge);

  const controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.enablePan = false;
  controls.minDistance = 80; controls.maxDistance = 260;
  controls.minPolarAngle = .35; controls.maxPolarAngle = 1.2;
  controls.autoRotate = !reduce; controls.autoRotateSpeed = .55;
  controls.target.set(0, 0, 0);
  let idle;
  controls.addEventListener('start', () => { controls.autoRotate = false; clearTimeout(idle); });
  controls.addEventListener('end', () => { if (!reduce) idle = setTimeout(() => { controls.autoRotate = true; }, 6000); });

  // أسماء المحافظات وبطاقة المهمة: عناصر HTML تتبع نقاطاً في المشهد
  const overlay = document.createElement('div');
  overlay.className = 'm3d-overlay';
  el.append(overlay);
  const labels = Object.entries(opts.areas || {}).map(([name, [lat, lng]]) => {
    const d = document.createElement('span');
    d.className = 'm3d-label'; d.textContent = name;
    overlay.append(d);
    const [x, y] = proj(lng, lat);
    return { d, v: new THREE.Vector3(x, LAND_H + 1.5, -y), w: 0, h: 0 };
  });
  const tip = document.createElement('div');
  tip.className = 'm3d-tip'; tip.hidden = true;
  overlay.append(tip);

  let pins = new THREE.Group(), heads = [], born = performance.now();
  scene.add(pins);
  function setTasks(list) {
    scene.remove(pins);
    pins.traverse(o => { if (o.material) o.material.dispose(); });
    pins = new THREE.Group(); heads = [];
    const max = Math.max(1, ...list.map(t => +t.budget || 0));
    list.forEach((t, i) => {
      const [x, y] = proj(t.lng, t.lat);
      const h = 5 + 20 * Math.sqrt((+t.budget || 0) / max);
      const color = new THREE.Color(STATUS_COLOR[t.status] || C.gold);
      const mat = new THREE.MeshStandardMaterial({ color, roughness: .35, metalness: .2, emissive: color, emissiveIntensity: .18 });
      const g = new THREE.Group();
      g.position.set(x, LAND_H, -y);
      const stalk = new THREE.Mesh(STALK, mat); stalk.scale.y = h;
      const head = new THREE.Mesh(HEAD, mat); head.position.y = h;
      const ring = new THREE.Mesh(RING, new THREE.MeshBasicMaterial({ color, transparent: true, opacity: .45, side: THREE.DoubleSide }));
      ring.position.y = .15;
      head.userData = { task: t, h, i };
      g.add(stalk, head, ring);
      g.userData = { i };
      pins.add(g); heads.push(head);
    });
    scene.add(pins);
    born = performance.now();
  }
  setTasks(tasks);

  // التحويم يُظهر البطاقة؛ النقر بالفأرة يفتح المهمة؛ اللمس يُظهر البطاقة وفيها رابط الفتح.
  const ray = new THREE.Raycaster(), ptr = new THREE.Vector2();
  let hovered = null, pinned = false, down = null;
  const hit = e => {
    const r = renderer.domElement.getBoundingClientRect();
    ptr.set(((e.clientX - r.left) / r.width) * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1);
    ray.setFromCamera(ptr, camera);
    return ray.intersectObjects(heads, false)[0]?.object || null;
  };
  const showTip = head => {
    hovered = head;
    if (!head) { tip.hidden = true; return; }
    const t = head.userData.task;
    tip.innerHTML = '';
    const b = document.createElement('b'); b.textContent = t.title;
    const m = document.createElement('span'); m.textContent = `${t.budget} د.ك · ${t.area}`;
    const a = document.createElement('a'); a.href = `#task-${t.id}`; a.textContent = 'افتح المهمة';
    tip.append(b, m, a); tip.hidden = false;
  };
  renderer.domElement.addEventListener('pointermove', e => {
    if (e.pointerType !== 'mouse' || pinned) return;
    const h = hit(e);
    if (h !== hovered) showTip(h);
    renderer.domElement.style.cursor = h ? 'pointer' : 'grab';
  });
  renderer.domElement.addEventListener('pointerleave', () => { if (!pinned) showTip(null); });
  renderer.domElement.addEventListener('pointerdown', e => { down = [e.clientX, e.clientY]; });
  renderer.domElement.addEventListener('pointerup', e => {
    if (!down || Math.hypot(e.clientX - down[0], e.clientY - down[1]) > 6) return;
    const h = hit(e);
    if (h && e.pointerType === 'mouse') { location.hash = `#task-${h.userData.task.id}`; return; }
    pinned = !!h; showTip(h);
  });

  const size = () => {
    const w = el.clientWidth, h = el.clientHeight;
    if (!w || !h) return;
    renderer.setSize(w, h, false);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
    // نبعد الكاميرا بقدر يُظهر الكويت كاملة في عرض اللوحة وارتفاعها، مع إبقاء زاوية الدوران الحالية
    const t = Math.tan(THREE.MathUtils.degToRad(camera.fov) / 2);
    const elev = Math.asin(Math.min(1, (camera.position.y - controls.target.y) / camera.position.distanceTo(controls.target)));
    const dist = Math.max((FIT_W / 2) / (t * camera.aspect), (FIT_D * Math.sin(elev) / 2 + 12) / t) * 1.08;
    const dir = camera.position.clone().sub(controls.target).normalize();
    camera.position.copy(controls.target).addScaledVector(dir, dist);
    controls.minDistance = dist * .45; controls.maxDistance = dist * 1.6;
  };
  const ro = new ResizeObserver(size); ro.observe(el); size();

  let visible = true;
  const io = new IntersectionObserver(([e]) => { visible = e.isIntersecting; });
  io.observe(el);

  const v = new THREE.Vector3();
  const toScreen = vec => { v.copy(vec).project(camera); return [(v.x * .5 + .5) * el.clientWidth, (-v.y * .5 + .5) * el.clientHeight, v.z > 1]; };
  const place = (node, vec, dy = 0) => {
    const [x, y, behind] = toScreen(vec);
    node.style.transform = `translate(${x}px, ${y + dy}px) translate(-50%, -100%)`;
    node.style.visibility = behind ? 'hidden' : '';
  };
  // أسماء المحافظات المتقاربة تتراكب مع الدوران؛ نزيح كل اسم يصطدم بما قبله إلى الأسفل
  const placeLabels = () => {
    const done = [];
    labels.forEach(l => {
      if (!l.w) { l.w = l.d.offsetWidth; l.h = l.d.offsetHeight; }
      let [x, y, behind] = toScreen(l.v);
      for (let k = 0; k < 4; k++) {
        const clash = done.find(o => Math.abs(o.x - x) < (o.w + l.w) / 2 + 4 && Math.abs(o.y - y) < l.h);
        if (!clash) break;
        y = clash.y + l.h;
      }
      done.push({ x, y, w: l.w });
      l.d.style.transform = `translate(${x}px, ${y}px) translate(-50%, -100%)`;
      l.d.style.visibility = behind ? 'hidden' : '';
    });
  };
  const tipAt = new THREE.Vector3();
  let disposed = false;
  function frame(now) {
    if (disposed) return;
    if (!el.isConnected) return dispose();
    requestAnimationFrame(frame);
    if (!visible) return;
    controls.update();
    const age = (now - born) / 1000;
    pins.children.forEach(g => {
      // الدبابيس ترتفع واحداً تلو الآخر عند الظهور
      const k = reduce ? 1 : Math.min(1, Math.max(0, (age - g.userData.i * .08) / .6));
      const e = 1 - Math.pow(1 - k, 3);
      g.scale.set(1, Math.max(.001, e), 1);
    });
    if (hovered) { hovered.getWorldPosition(tipAt); place(tip, tipAt, -14); }
    placeLabels();
    renderer.render(scene, camera);
  }
  requestAnimationFrame(frame);

  function dispose() {
    if (disposed) return;
    disposed = true;
    ro.disconnect(); io.disconnect(); controls.dispose(); clearTimeout(idle);
    scene.traverse(o => { if (o.geometry && ![STALK, HEAD, RING].includes(o.geometry)) o.geometry.dispose(); if (o.material) o.material.dispose(); });
    renderer.dispose();
  }
  return { setTasks, dispose };
}
