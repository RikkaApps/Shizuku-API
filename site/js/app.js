'use strict';
/* صنعة · سوق المهام — نموذج تجريبي. كل الحالة في localStorage؛ الدفع/التوثيق/الإشعارات محاكاة. */

const KEY = 'mahamm_v1';
const FEE = 0.05;               // عمولة المنصة
const VERIFY_LIMIT = 50;        // عروض أعلى من هذا المبلغ تتطلب منفّذاً موثّقاً
const AREAS = {
  'العاصمة': [29.3759, 47.9774], 'حولي': [29.3328, 48.0286], 'الفروانية': [29.2775, 47.9585],
  'الأحمدي': [29.0769, 48.0838], 'الجهراء': [29.3375, 47.6581], 'مبارك الكبير': [29.2120, 48.0600]
};
const CATS = ['توصيل', 'تنظيف', 'صيانة وإصلاح', 'تصميم وبرمجة', 'ترجمة وكتابة', 'دروس وتعليم', 'تصوير', 'ذكاء اصطناعي', 'أخرى'];
const STATUS = {
  open: ['مفتوحة للعروض', ''], assigned: ['قيد التنفيذ — المبلغ محجوز', 'warn'],
  delivered: ['تم التسليم — بانتظار تأكيدك', 'warn'], completed: ['مكتملة', 'ok'],
  disputed: ['نزاع — المبلغ مجمّد', 'bad'], cancelled: ['ملغاة', 'bad']
};
// صنعة قسمان بهوية واحدة: «تعلّم» (صفحة صنعة) لتعلّم المهارة، و«سوق المهام» (هذا الموقع) للعمل بها.
const SAN3A = 'https://claude.ai/artifact/JAnYSWkHtB7QcVheH94fHH';
const san3a = (sec, text, cls = '') => `<a class="${cls}" href="${SAN3A}${sec ? '#' + sec : ''}" target="_blank" rel="noopener">${text} ↗</a>`;
const san3aCard = (title, body) => `<div class="card san3a"><b>🛠 ${title}</b><p class="mute">${body}</p>
  <div class="row fit">${san3a('paths', 'مسارات الدخل', 'btn')}${san3a('plan', 'خطتي لـ 4 أسابيع', 'btn sec')}${san3a('community', 'ركن المجتمع', 'btn sec')}</div></div>`;
const REPORT_REASONS = ['احتيال أو نصب', 'محتوى مسيء', 'طلب دفع خارج المنصة', 'انتحال هوية', 'مهمة مخالفة للقانون', 'أخرى'];

/* ---------- الحالة ---------- */
const now = () => Date.now();
const day = 864e5;
function seed() {
  const j = (a, b) => b + (Math.random() - .5) * a;
  const mk = (id, title, desc, cat, area, budget, owner, extra = {}) => ({
    id, title, desc, cat, area, budget, owner, lat: j(.05, AREAS[area][0]), lng: j(.05, AREAS[area][1]),
    status: 'open', created: now() - id * 3e6, bids: [], escrow: null, worker: null, ...extra
  });
  return {
    me: 'u1', seq: 10,
    users: {
      u1: { name: 'أنت (صاحب مهمة)', verified: false, skills: '' },
      u2: { name: 'فهد المطيري', verified: true, skills: 'تركيب وصيانة، كهرباء منزلية' },
      u3: { name: 'نورة العنزي', verified: true, skills: 'تصميم شعارات، محتوى بالذكاء الاصطناعي' },
      u4: { name: 'يوسف الرشيدي', verified: false, skills: 'توصيل وخدمات سريعة' }
    },
    tasks: [
      mk(1, 'تركيب مكيف سبليت', 'تركيب وحدة سبليت 24 ألف وحدة في غرفة المعيشة، المواد متوفرة.', 'صيانة وإصلاح', 'حولي', 35, 'u1', { bids: [{ by: 'u2', price: 30, date: '2026-11-10', note: 'خبرة 8 سنوات، أحضر العدة كاملة.' }, { by: 'u4', price: 25, date: '2026-11-09', note: 'أقدر أجي بكرة.' }] }),
      mk(2, 'تصميم شعار لمشروع منزلي', 'شعار لمتجر حلويات إنستغرام، ألوان دافئة، ملفات مفتوحة.', 'تصميم وبرمجة', 'العاصمة', 40, 'u1'),
      mk(3, 'كتابة 10 منشورات بمساعدة الذكاء الاصطناعي', 'منشورات تسويقية بالعربية لحساب مطعم، مراجعة بشرية للنص.', 'ذكاء اصطناعي', 'الفروانية', 20, 'u4'),
      mk(4, 'توصيل طرد من الجهراء إلى حولي', 'طرد صغير، خلال اليوم.', 'توصيل', 'الجهراء', 5, 'u3'),
      mk(5, 'تنظيف شقة بعد تسليم', 'شقة غرفتين وصالة، يشمل المطبخ والحمامات.', 'تنظيف', 'مبارك الكبير', 45, 'u3'),
      mk(6, 'تصوير منتجات متجر', '30 منتجاً بخلفية بيضاء مع تعديل بسيط.', 'تصوير', 'الأحمدي', 60, 'u4'),
      mk(7, 'ترجمة عقد إيجار', 'عربي إلى إنجليزي، صفحتان.', 'ترجمة وكتابة', 'العاصمة', 12, 'u1',
        { status: 'completed', worker: 'u3', bids: [{ by: 'u3', price: 10, date: '2026-10-20', note: '' }], escrow: { state: 'released', amount: 10, fee: .5 } })
    ],
    reviews: [{ task: 7, from: 'u1', to: 'u3', stars: 5, text: 'سريعة ودقيقة، أنصح بها.', at: now() - 5 * day }],
    reports: [], notifs: [], messages: [
      { id: 1, key: '1:u1-u2', task: 1, from: 'u2', to: 'u1', text: 'السلام عليكم، هل الوحدة الخارجية ستُركّب على السطح أم على الجدار؟', at: now() - 2 * 36e5, read: false, risky: false }
    ], prefs: { push: false, wa: '' }
  };
}
let S;
try { S = JSON.parse(localStorage.getItem(KEY)) || seed(); } catch { S = seed(); }
S.messages ||= [];
const save = () => { try { localStorage.setItem(KEY, JSON.stringify(S)); } catch { } };

/* ---------- أدوات ---------- */
const $ = (s, r = document) => r.querySelector(s);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const user = id => S.users[id] || { name: 'محذوف', verified: false };
const task = id => S.tasks.find(t => t.id == id);
const money = n => (+n).toFixed(n % 1 ? 2 : 0) + ' د.ك';
const ago = t => { const m = Math.round((now() - t) / 6e4); return m < 60 ? `قبل ${Math.max(m, 1)} د` : m < 1440 ? `قبل ${Math.round(m / 60)} س` : `قبل ${Math.round(m / 1440)} يوم`; };
const rating = id => { const r = S.reviews.filter(x => x.to === id); return { n: r.length, avg: r.length ? r.reduce((a, b) => a + b.stars, 0) / r.length : 0 }; };
const stars = n => '★'.repeat(Math.round(n)) + '☆'.repeat(5 - Math.round(n));
const badge = id => user(id).verified ? '<span class="chip ok">✔ موثّق</span>' : '<span class="chip">غير موثّق</span>';
const uname = id => `<a href="#profile-${id}">${esc(user(id).name)}</a> ${badge(id)}`;
const me = () => S.me;
let toastT;
function toast(m) { const t = $('#toast'); t.textContent = m; t.classList.add('on'); clearTimeout(toastT); toastT = setTimeout(() => t.classList.remove('on'), 3500); }

/* ---------- الإشعارات ---------- */
function notify(to, text, link) {
  S.notifs.unshift({ id: now() + Math.random(), to, text, link, at: now(), read: false });
  S.notifs = S.notifs.slice(0, 100); save();
  if (to === me()) {
    toast('🔔 ' + text);
    if (S.prefs.push && 'Notification' in window && Notification.permission === 'granted') {
      navigator.serviceWorker?.ready.then(r => r.showNotification('صنعة', { body: text, icon: 'icon.svg', data: location.pathname + link }))
        .catch(() => new Notification('صنعة', { body: text }));
    }
  }
  updateBadge();
}
function updateBadge() {
  const n = S.notifs.filter(x => x.to === me() && !x.read).length, b = $('#badge');
  b.hidden = !n; b.textContent = n;
  const m = S.messages.filter(x => x.to === me() && !x.read).length, mb = $('#mbadge');
  mb.hidden = !m; mb.textContent = m;
}

/* ---------- نافذة حوار ---------- */
function dialog(html, onOpen) { const d = $('#dlg'); d.innerHTML = html; d.showModal(); onOpen?.(d); return d; }
const closeDlg = () => $('#dlg').close();

/* ---------- العروض (مكوّنات) ---------- */
// تسمية قصيرة للحالة داخل البطاقات (النص الكامل في صفحة المهمة)
const STATUS_SHORT = { open: 'مفتوحة', assigned: 'قيد التنفيذ', delivered: 'بانتظار التأكيد', completed: 'مكتملة', disputed: 'نزاع', cancelled: 'ملغاة' };
function taskCard(t) {
  return `<article class="card task">
  <div class="task-top"><span class="cat">${esc(t.cat)}</span><span class="pill ${STATUS[t.status][1]}">${STATUS_SHORT[t.status]}</span></div>
  <h3><a class="t" href="#task-${t.id}">${esc(t.title)}</a></h3>
  <p>${esc(t.desc.slice(0, 90))}${t.desc.length > 90 ? '…' : ''}</p>
  <div class="task-foot"><span class="price">${+t.budget} <small>د.ك</small></span>
  <span class="meta">${esc(t.area)} · ${t.bids.length} عروض<br>${esc(user(t.owner).name)} · ${ago(t.created)}</span></div></article>`;
}
const optionList = (arr, sel) => arr.map(a => `<option ${a === sel ? 'selected' : ''}>${esc(a)}</option>`).join('');

/* ---------- الصفحات ---------- */
const views = {};

// الخريطة ثلاثية الأبعاد تُحمَّل عند الحاجة؛ إن تعذّرت (لا WebGL، أو فتح الملف بلا خادم) تظهر رسالة بديلة.
const LEGEND3D = `<p class="m3d-hint">اسحب لتدوير الخريطة · مرّر على دبوس لترى المهمة</p>
  <p class="legend"><span><i style="background:var(--gold)"></i>مفتوحة</span><span><i style="background:var(--pri)"></i>قيد التنفيذ</span><span><i style="background:var(--on-night-mute)"></i>مكتملة</span><span>ارتفاع الدبوس = الميزانية</span></p>`;
function mount3D(el, list) {
  return import('./map3d.js').then(m => m.mountMap3D(el, list, { areas: AREAS })).catch(() => {
    if (el.isConnected) el.innerHTML = '<div class="hero-fallback">تعذّر عرض الخريطة ثلاثية الأبعاد على هذا الجهاز. جرّب عرض «خريطة» أو «قائمة» في صفحة المهام.</div>';
    return null;
  });
}

// مقترحات الأسبوع: نفس الاختيار ونفس الترتيب في صفحة «صنعة · تعلّم» (المولّد العشوائي نفسه ورقم الأسبوع نفسه).
function weeklyTips(n = 6) {
  const T = window.SAN3A_TIPS || [];
  let seed = Math.floor(Date.now() / (7 * 24 * 60 * 60000)) * 2654435761 % 4294967296;
  const r = () => { seed |= 0; seed = seed + 0x6D2B79F5 | 0; let t = Math.imul(seed ^ seed >>> 15, 1 | seed); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; };
  const a = T.slice();
  for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; }
  return a.slice(0, n);
}

views.home = () => {
  const open = S.tasks.filter(t => t.status === 'open');
  const sum = open.reduce((a, t) => a + (+t.budget || 0), 0);
  const verified = Object.values(S.users).filter(u => u.verified).length;
  return `<section class="hero">
   <div class="hero-copy">
    <p class="eyebrow">صنعة · سوق المهام في الكويت</p>
    <h1>أنجز مهمتك، أو <em>اربح من مهارتك</em>.</h1>
    <p class="lead">عروض أسعار من منفّذين قريبين منك، ومبلغك محجوز بنظام الأمانة حتى تستلم العمل وتؤكده.</p>
    <div class="cta"><a class="btn gold" href="#new">انشر مهمة</a><a class="btn sec" href="#tasks">تصفّح المهام</a></div>
    <dl class="stats"><div><dt>مهمة مفتوحة</dt><dd>${open.length}</dd></div><div><dt>د.ك في المهام المفتوحة</dt><dd>${sum}</dd></div><div><dt>منفّذ موثّق</dt><dd>${verified}</dd></div></dl>
   </div>
   <div class="hero-3d" id="hero3d">${LEGEND3D}</div>
  </section>
  <div class="numbered">
  <h2>كيف تعمل الأمانة؟</h2>
  <ol class="flow">
   <li><b>عروض</b><p>المنفّذون يقدّمون سعرهم وموعدهم، وأنت تختار.</p></li>
   <li><b>أمانة</b><p>تدفع، فيُحجز المبلغ عند المنصة ولا يصل للمنفّذ بعد.</p></li>
   <li><b>تسليم وتأكيد</b><p>استلمت العمل؟ أكّد فيُحوَّل المبلغ. لم يعجبك؟ افتح نزاعاً.</p></li>
   <li><b>تقييم</b><p>كل طرف يقيّم الآخر، فتبني السمعة ثقة من يأتي بعدك.</p></li>
  </ol>
  ${san3aCard('ما عندك مهارة مطلوبة بعد؟ ابدأ مع «صنعة · تعلّم»', 'مسارات لكسب الدخل بالذكاء الاصطناعي، وأدوات مجانية، وخطة شخصية لأربعة أسابيع. تعلّم هناك، ثم عُد هنا وقدّم عروضك على المهام.')}
  <div class="sect-head"><h2>أحدث المهام</h2><a href="#tasks">كل المهام ←</a></div>
  <div class="grid">${open.slice(0, 6).map(taskCard).join('') || '<p class="mute">لا توجد مهام مفتوحة.</p>'}</div>
  <div class="sect-head"><h2>مقترحات هذا الأسبوع</h2>${san3a('pulse', 'كل المقترحات في صنعة · تعلّم')}</div>
  <p class="tips-note">ست أفكار من 100 مقترح للعمل والكسب، تتجدد كل خميس.</p>
  <div class="grid feed">${weeklyTips().map(p => `<article class="card" data-g="${esc(p.g)}"><span class="tag">${esc(p.g)}</span><h3>${esc(p.t)}</h3><p class="mute">${esc(p.b)}</p></article>`).join('')}</div>
  </div>`;
};
views.home.after = () => { mount3D($('#hero3d'), S.tasks.filter(t => t.status !== 'cancelled')); };

let tf = { q: '', area: '', cat: '', view: 'list', sort: 'new' };
views.tasks = () => `<div class="sect-head"><h1>المهام</h1><span class="mute" id="count"></span></div>
 <div class="card"><div class="filters">
  <div><label for="fq">بحث</label><input id="fq" value="${esc(tf.q)}" placeholder="مثال: تركيب"></div>
  <div><label for="fa">المنطقة</label><select id="fa"><option value="">كل الكويت</option>${optionList(Object.keys(AREAS), tf.area)}</select></div>
  <div><label for="fc">التصنيف</label><select id="fc"><option value="">الكل</option>${optionList(CATS, tf.cat)}</select></div>
  <div><label for="fs">الترتيب</label><select id="fs"><option value="new" ${tf.sort === 'new' ? 'selected' : ''}>الأحدث</option><option value="price" ${tf.sort === 'price' ? 'selected' : ''}>الأعلى ميزانية</option></select></div>
 </div></div>
 <div class="tabs" role="group" aria-label="طريقة العرض">${[['list', 'قائمة'], ['map', 'خريطة'], ['3d', 'مجسّم 3D']].map(([v, n]) => `<button data-v="${v}" class="${tf.view === v ? 'on' : ''}" aria-pressed="${tf.view === v}">${n}</button>`).join('')}</div>
 <div id="results"></div>`;
views.tasks.after = () => {
  let m3 = null; // المجسّم يبقى عند تغيير الفلاتر وتُحدَّث دبابيسه فقط
  const draw = () => {
    let l = S.tasks.filter(t => t.status !== 'cancelled' &&
      (!tf.area || t.area === tf.area) && (!tf.cat || t.cat === tf.cat) &&
      (!tf.q || (t.title + t.desc).includes(tf.q)));
    l.sort((a, b) => tf.sort === 'price' ? b.budget - a.budget : b.created - a.created);
    const box = $('#results');
    $('#count').textContent = `${l.length} مهمة`;
    if (tf.view === '3d') {
      if (m3 && m3.el.isConnected) { m3.p.then(i => i?.setTasks(l)); return; }
      box.innerHTML = `<div class="map3d" id="m3">${LEGEND3D}</div>`;
      m3 = { el: $('#m3'), p: mount3D($('#m3'), l) };
      return;
    }
    if (tf.view === 'list') { box.innerHTML = `<div class="grid">${l.map(taskCard).join('') || `<p class="mute">لا توجد نتائج. ${san3a('paths', 'اكتشف مجالات جديدة في «صنعة · تعلّم»')}</p>`}</div>`; return; }
    box.innerHTML = '<div id="map" role="application" aria-label="خريطة المهام"></div>';
    if (typeof L === 'undefined') { box.innerHTML = '<p class="card">تعذّر تحميل الخريطة (لا يوجد اتصال).</p>'; return; }
    const m = L.map('map').setView(tf.area ? AREAS[tf.area] : [29.3, 47.95], tf.area ? 11 : 9);
    const pri = getComputedStyle(document.documentElement).getPropertyValue('--pri').trim() || '#0f766e';
    // صور الخريطة من OpenStreetMap؛ إن حُجبت (مثل عارض claude.ai) تبقى حدود الكويت وأسماء المحافظات مرسومة.
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 18, attribution: '© OpenStreetMap' }).addTo(m);
    if (window.KUWAIT_GEO) {
      const kw = L.geoJSON(window.KUWAIT_GEO, { interactive: false, style: { color: pri, weight: 1.5, fillColor: pri, fillOpacity: .07 } }).addTo(m);
      if (!tf.area) m.fitBounds(kw.getBounds(), { padding: [10, 10] });
    }
    Object.entries(AREAS).forEach(([n, c]) => L.tooltip({ permanent: true, direction: 'center', className: 'area-label', interactive: false }).setLatLng([c[0] + .045, c[1]]).setContent(esc(n)).addTo(m));
    l.forEach(t => L.circleMarker([t.lat, t.lng], { radius: 9, color: '#fff', weight: 2, fillColor: pri, fillOpacity: .95 }).addTo(m).bindPopup(`<b>${esc(t.title)}</b><br>${money(t.budget)} · ${esc(t.area)}<br><a href="#task-${t.id}">فتح</a>`));
  };
  $('#fq').oninput = e => { tf.q = e.target.value; draw(); };
  $('#fa').onchange = e => { tf.area = e.target.value; draw(); };
  $('#fc').onchange = e => { tf.cat = e.target.value; draw(); };
  $('#fs').onchange = e => { tf.sort = e.target.value; draw(); };
  document.querySelectorAll('.tabs button').forEach(b => b.onclick = () => { tf.view = b.dataset.v; route(); });
  draw();
};

/* ---------- مساعد كتابة المهمة ----------
   في عارض claude.ai يكتبها Claude عبر قدرة «sample» على حساب الزائر وبعد إذنه.
   خارج العارض، أو إن رفض الزائر، يملأ قالب جاهز الحقول نفسها دون ذكاء اصطناعي. */
const samplePromise = window.claude?.use ? window.claude.use('sample').catch(() => null) : Promise.resolve(null);
let aiOff = false; // يصبح true إن رفض الزائر السماح أو لم يكن Claude متاحاً لحسابه
const CAT_HINTS = [
  ['توصيل', /توصيل|طرد|مشوار|أوصل|اوصل|شحن/], ['تنظيف', /تنظيف|نظاف|غسيل|ترتيب/],
  ['صيانة وإصلاح', /تركيب|صيانة|تصليح|إصلاح|اصلاح|مكيف|كهرب|سباك|نجار|صبغ|دهان/],
  ['تصميم وبرمجة', /تصميم|شعار|موقع|برمج|تطبيق|لوقو/], ['ترجمة وكتابة', /ترجم|كتابة|تدقيق|مقال|محتوى/],
  ['دروس وتعليم', /درس|دروس|تدريس|معلم|مدرس|تقوية/], ['تصوير', /تصوير|صور|فيديو|مونتاج/],
  ['ذكاء اصطناعي', /ذكاء|\bAI\b|شات|بوت/i]
];
function templateTask({ need, when, more, area }) {
  const cat = (CAT_HINTS.find(([, re]) => re.test(need + ' ' + more)) || ['أخرى'])[0];
  const title = need.replace(/\s+/g, ' ').trim().replace(/[.،!؟]+$/, '').slice(0, 60);
  const desc = [`المطلوب: ${need.trim()}`, `الموقع: ${area}`, `الموعد: ${when}`, more.trim() && `تفاصيل: ${more.trim()}`,
    'في عرضك اذكر خبرتك بأعمال مشابهة، وما يشمله السعر، وموعد التسليم.'].filter(Boolean).join('\n');
  return { title, desc, cat };
}
async function aiTask(sample, { need, when, more, area, budget }, signal) {
  const prompt = `اكتب إعلان مهمة لمنصة مهام في الكويت انطلاقاً من طلب صاحب المهمة أدناه.
اكتب بعربية واضحة قريبة من لهجة أهل الكويت دون مبالغة، ولا تخترع تفاصيل لم يذكرها؛ إن نقصت معلومة مهمة اكتب ما يحتاج المنفّذ أن يسأل عنه.
لا تضع أرقام هواتف أو حسابات أو روابط.
أجب بكائن JSON فقط بهذا الشكل:
{"title": "عنوان قصير محدد حتى 60 حرفاً", "desc": "وصف من 3 إلى 6 أسطر: المطلوب، المكان، الموعد، ما يجب أن يذكره المنفّذ في عرضه", "cat": "أحد التصنيفات بالضبط: ${CATS.join('، ')}", "budget_min": رقم بالدينار الكويتي, "budget_max": رقم, "budget_note": "جملة قصيرة تشرح ما يؤثر في السعر"}

الطلب: ${need}
المنطقة: ${area}
الموعد: ${when}
${more ? 'تفاصيل إضافية: ' + more : ''}
${budget ? 'ميزانية صاحب المهمة: ' + budget + ' د.ك' : 'لم يحدد ميزانية؛ اقترح نطاقاً معقولاً لسوق الكويت.'}`;
  const r = await sample.json(prompt, { signal, modelTier: 'quick' });
  const num = v => (Number.isFinite(+v) && +v > 0 ? Math.round(+v) : null);
  return {
    title: String(r?.title || '').slice(0, 80), desc: String(r?.desc || '').slice(0, 800),
    cat: CATS.includes(r?.cat) ? r.cat : null, min: num(r?.budget_min), max: num(r?.budget_max),
    note: String(r?.budget_note || '').slice(0, 160)
  };
}
function openAssistant() {
  let ctl = null;
  dialog(`<h3>✨ ساعدني في كتابة المهمة</h3>
  <label for="aiNeed">ماذا تحتاج؟</label><textarea id="aiNeed" rows="2" maxlength="300" placeholder="مثال: أبي أحد يركّب لي مكيف سبليت في الصالة"></textarea>
  <label for="aiWhen">متى تحتاجه؟</label><select id="aiWhen">${optionList(['اليوم أو غداً', 'خلال هذا الأسبوع', 'خلال شهر', 'الموعد مرن'])}</select>
  <label for="aiMore">تفاصيل تهمّك (اختياري)</label><input id="aiMore" maxlength="200" placeholder="مثال: المواد متوفرة، الدور الثاني بدون مصعد">
  <p class="mute" id="aiMode">…</p><p id="aiStatus" role="status"></p>
  <div class="row fit"><button id="aiGo">اكتب لي المهمة</button><button class="sec" id="aiStop" hidden>إيقاف</button><button class="sec" id="aiClose">إغلاق</button></div>`, d => {
    const st = $('#aiStatus', d), go = $('#aiGo', d), stop = $('#aiStop', d);
    samplePromise.then(s => { $('#aiMode', d).textContent = s && !aiOff ? 'يكتبها Claude من حسابك على claude.ai؛ في أول مرة يُطلب منك السماح.' : 'يعمل هنا بقالب جاهز دون ذكاء اصطناعي. في نسخة claude.ai يكتبها Claude.'; });
    $('#aiNeed', d).focus();
    $('#aiClose', d).onclick = () => { ctl?.abort(); closeDlg(); };
    stop.onclick = () => ctl?.abort();
    go.onclick = async () => {
      const input = { need: $('#aiNeed', d).value.trim(), when: $('#aiWhen', d).value, more: $('#aiMore', d).value.trim(), area: $('#na').value, budget: +$('#nb').value || 0 };
      if (input.need.length < 4) { st.textContent = 'اكتب في سطر ما الذي تحتاجه.'; $('#aiNeed', d).focus(); return; }
      const sample = aiOff ? null : await samplePromise;
      let out = null;
      if (sample) {
        ctl = new AbortController(); go.disabled = true; stop.hidden = false; st.textContent = 'جارٍ الكتابة…';
        try { out = await aiTask(sample, input, ctl.signal); }
        catch (e) {
          const msg = {
            cancelled: '', rate_limited: 'طلبات كثيرة الآن. انتظر قليلاً ثم حاول مرة أخرى.',
            invalid_json: 'لم يكتمل النص. اضغط «اكتب لي المهمة» مرة أخرى.', refused: 'لم يقبل Claude هذا الطلب. غيّر صياغته وحاول مرة أخرى.',
            session_expired: 'انتهت جلستك في claude.ai. سجّل الدخول ثم حاول مرة أخرى.'
          }[e?.code];
          if (['not_granted', 'sampling_disabled', 'not_declared', 'capability_disabled', 'capability_removed'].includes(e?.code)) {
            aiOff = true; $('#aiMode', d).textContent = 'Claude غير متاح هنا، فاستخدمنا القالب الجاهز.';
          } else { st.textContent = msg ?? 'تعذّر الاتصال. حاول مرة أخرى.'; return; }
        } finally { go.disabled = false; stop.hidden = true; ctl = null; }
      }
      if (!out || !out.title || !out.desc) out = { ...templateTask(input), min: null, max: null, note: '' };
      $('#nt').value = out.title; $('#nd').value = out.desc;
      if (out.cat) $('#nc').value = out.cat;
      const hint = $('#nbHint');
      if (out.min && out.max) {
        if (!$('#nb').value) $('#nb').value = Math.round((out.min + out.max) / 2);
        hint.textContent = `💡 ميزانية تقديرية: ${out.min}–${out.max} د.ك. ${out.note}`; hint.hidden = false;
      }
      closeDlg(); toast('راجع النص وعدّل ما تريد قبل النشر'); $('#nt').focus();
    };
  });
}

views.new = () => `<h1>انشر مهمة</h1>
 <div class="card ai"><div><b>✨ مساعد كتابة المهمة</b><p class="mute">جاوب على سؤالين ويكتب لك عنواناً ووصفاً واضحاً يجذب منفّذين جادّين. يمكنك تعديل كل شيء قبل النشر.</p></div>
 <button type="button" class="sec" id="aiOpen">ساعدني في الكتابة</button></div>
 <form class="card" id="nf">
 <label for="nt">العنوان</label><input id="nt" required maxlength="80">
 <label for="nd">الوصف</label><textarea id="nd" required rows="4" maxlength="800" placeholder="التفاصيل، الموعد المطلوب، أي شروط"></textarea>
 <div class="row"><div><label for="nc">التصنيف</label><select id="nc">${optionList(CATS)}</select></div>
 <div><label for="na">المنطقة</label><select id="na">${optionList(Object.keys(AREAS))}</select></div>
 <div><label for="nb">الميزانية (د.ك)</label><input id="nb" type="number" min="1" step="0.5" required></div></div>
 <p class="mute" id="nbHint" hidden></p>
 <p class="mute">لحمايتك: لا تشارك رقم بطاقتك أو كلمات المرور، ولا تدفع خارج المنصة.</p>
 <button>نشر المهمة</button></form>`;
views.new.after = () => {
  $('#aiOpen').onclick = openAssistant;
  $('#nf').onsubmit = e => {
    e.preventDefault();
    const area = $('#na').value, c = AREAS[area];
    const t = {
      id: ++S.seq, title: $('#nt').value.trim(), desc: $('#nd').value.trim(), cat: $('#nc').value, area, budget: +$('#nb').value,
      owner: me(), lat: c[0] + (Math.random() - .5) * .05, lng: c[1] + (Math.random() - .5) * .05, status: 'open', created: now(), bids: [], escrow: null, worker: null
    };
    S.tasks.unshift(t); save(); toast('تم نشر المهمة');
    // محاكاة: إشعار بوصول عرض جديد بعد ثوانٍ
    setTimeout(() => {
      const w = ['u2', 'u3', 'u4'].filter(x => x !== t.owner)[Math.floor(Math.random() * 2)];
      t.bids.push({ by: w, price: Math.max(1, Math.round(t.budget * .9)), date: new Date(now() + 2 * day).toISOString().slice(0, 10), note: 'جاهز للتنفيذ.' });
      save(); notify(t.owner, `عرض جديد من ${user(w).name} على «${t.title}»`, `#task-${t.id}`);
    }, 6000);
    location.hash = '#task-' + t.id;
  };
};

views.task = id => {
  const t = task(id); if (!t) return '<p class="card">المهمة غير موجودة.</p>';
  const isOwner = t.owner === me(), isWorker = t.worker === me();
  const reviews = S.reviews.filter(r => r.task === t.id);
  let h = `<a href="#tasks">← المهام</a><div class="card"><h1>${esc(t.title)}</h1>
  <div><span class="chip">${esc(t.cat)}</span><span class="chip">📍 ${esc(t.area)}</span><span class="chip ${STATUS[t.status][1]}">${STATUS[t.status][0]}</span></div>
  <p>${esc(t.desc).replace(/\n/g, '<br>')}</p>${t.cat === 'ذكاء اصطناعي' && !isOwner ? `<p class="mute">🤖 جديد على أدوات الذكاء الاصطناعي؟ ${san3a('tools', 'تعلّمها مجاناً في «صنعة · تعلّم»')}</p>` : ''}<p class="price">الميزانية: ${money(t.budget)}</p>
  <p class="mute">صاحب المهمة: ${uname(t.owner)}${t.worker ? ` · المنفّذ: ${uname(t.worker)}` : ''}</p>
  <div class="row fit">${!isOwner ? `<a class="btn sec" href="#chat-${t.id}-${t.owner}">💬 راسل صاحب المهمة</a>` : ''}
  <button class="danger" data-report="task:${t.id}">🚩 إبلاغ عن المهمة</button></div></div>`;

  if (t.escrow && t.escrow.state !== 'none') {
    const e = t.escrow, lbl = { held: 'محجوز عند المنصة 🔒', released: 'تم تحويله للمنفّذ ✔', refunded: 'أُعيد لصاحب المهمة ↩' }[e.state];
    h += `<div class="card escrow"><b>حساب الأمانة:</b> ${money(e.amount)} — ${lbl}<div class="mute">عمولة المنصة ${money(e.fee)} تُخصم من المنفّذ عند الصرف.</div></div>`;
  }

  // إجراءات حسب الحالة والدور
  let act = '';
  if (isWorker && t.status === 'assigned') act += `<button id="deliver">تسليم العمل</button>`;
  if (isOwner && t.status === 'delivered') act += `<button id="confirm">✔ استلمت العمل — حوّل المبلغ</button> <button class="danger" id="dispute">فتح نزاع</button>`;
  if (isOwner && t.status === 'open') act += `<button class="danger" id="cancel">إلغاء المهمة</button>`;
  if (t.status === 'disputed') act += `<p class="mute">⚙ تجريبي (دور الإدارة):</p><button class="sec" id="resRefund">استرجاع لصاحب المهمة</button> <button class="sec" id="resPay">صرف للمنفّذ</button>`;
  if (act) h += `<div class="card">${act}</div>`;

  if (t.status === 'completed' && (isOwner || isWorker)) {
    const other = isOwner ? t.worker : t.owner;
    if (!reviews.some(r => r.from === me()))
      h += `<form class="card" id="rv"><h3>قيّم ${esc(user(other).name)}</h3>
      <label for="rs">التقييم</label><select id="rs">${[5, 4, 3, 2, 1].map(n => `<option value="${n}">${stars(n)}</option>`).join('')}</select>
      <label for="rt">تعليقك</label><textarea id="rt" rows="3" maxlength="400"></textarea><button>إرسال التقييم</button></form>`;
  }
  if (reviews.length) h += `<h2>التقييمات</h2>` + reviews.map(r => `<div class="card"><span class="stars">${stars(r.stars)}</span> <b>${esc(user(r.from).name)}</b><p>${esc(r.text)}</p></div>`).join('');

  // العروض
  h += `<h2>العروض (${t.bids.length})</h2>`;
  if (t.status === 'open' && !isOwner) {
    const mine = t.bids.find(b => b.by === me());
    h += `<form class="card" id="bf"><h3>${mine ? 'عدّل عرضك' : 'قدّم عرضك'}</h3><div class="row">
    <div><label for="bp">السعر (د.ك)</label><input id="bp" type="number" min="1" step="0.5" required value="${mine?.price ?? ''}"></div>
    <div><label for="bd">موعد التسليم</label><input id="bd" type="date" required value="${mine?.date ?? ''}"></div></div>
    <label for="bn">ملاحظة</label><textarea id="bn" rows="2" maxlength="300">${esc(mine?.note ?? '')}</textarea>
    ${user(me()).verified ? '' : `<p class="mute">المنفّذ غير الموثّق لا يمكنه تقديم عرض فوق ${VERIFY_LIMIT} د.ك. <a href="#profile-me">وثّق هويتك</a></p>`}
    <button>إرسال العرض</button></form>`;
  }
  h += t.bids.length ? t.bids.map((b, i) => {
    const r = rating(b.by), chosen = t.worker === b.by;
    return `<div class="card ${chosen ? 'escrow' : ''}"><div class="row fit"><b class="price">${money(b.price)}</b><span class="mute">التسليم ${esc(b.date)}</span></div>
    <div>${uname(b.by)} ${r.n ? `<span class="stars">${stars(r.avg)}</span> <span class="mute">(${r.n})</span>` : '<span class="mute">لا تقييمات بعد</span>'}
    ${workCount(b.by) ? ` · <a href="#profile-${b.by}">📁 ${workCount(b.by)} أعمال</a>` : ''}</div>
    <p>${esc(b.note)}</p>
    <div class="row fit">${isOwner && t.status === 'open' ? `<button data-accept="${i}">اختيار العرض والدفع للأمانة</button>` : ''}
    ${isOwner ? `<a class="btn sec" href="#chat-${t.id}-${b.by}">💬 محادثة${unreadIn(t.id, b.by) ? ` (${unreadIn(t.id, b.by)})` : ''}</a>` : ''}
    <button class="danger" data-report="user:${b.by}">🚩 إبلاغ</button></div></div>`;
  }).join('') : '<p class="mute">لا توجد عروض بعد.</p>';
  return h;
};
views.task.after = id => {
  const t = task(id); if (!t) return;
  const set = (st, msgTo, msg) => { t.status = st; save(); if (msgTo) notify(msgTo, msg, `#task-${t.id}`); route(); };
  const bf = $('#bf');
  if (bf) bf.onsubmit = e => {
    e.preventDefault();
    const price = +$('#bp').value;
    if (price > VERIFY_LIMIT && !user(me()).verified) return toast(`العروض فوق ${VERIFY_LIMIT} د.ك تتطلب توثيق الهوية`);
    const b = { by: me(), price, date: $('#bd').value, note: $('#bn').value.trim() };
    const i = t.bids.findIndex(x => x.by === me()); i >= 0 ? t.bids[i] = b : t.bids.push(b);
    save(); notify(t.owner, `عرض جديد من ${user(me()).name}: ${money(price)} على «${t.title}»`, `#task-${t.id}`);
    toast('تم إرسال العرض'); route();
  };
  document.querySelectorAll('[data-accept]').forEach(btn => btn.onclick = () => {
    const b = t.bids[+btn.dataset.accept], fee = +(b.price * FEE).toFixed(2);
    dialog(`<h3>الدفع إلى الأمانة</h3><p>المبلغ: <b>${money(b.price)}</b> يُحجز عند المنصة ولا يصل إلى ${esc(user(b.by).name)} إلا بعد تأكيدك الاستلام.</p>
    <p class="mute">عمولة المنصة ${money(fee)} (${FEE * 100}%) تُخصم من المنفّذ. (الدفع هنا محاكاة؛ يتم ربطه لاحقاً ببوابة دفع مرخّصة مثل KNET.)</p>
    <div class="row fit"><button id="pay">ادفع واحجز المبلغ</button><button class="sec" id="no">إلغاء</button></div>`, d => {
      $('#no', d).onclick = closeDlg;
      $('#pay', d).onclick = () => {
        closeDlg(); t.worker = b.by; t.escrow = { state: 'held', amount: b.price, fee };
        set('assigned', b.by, `تم اختيار عرضك على «${t.title}» والمبلغ محجوز في الأمانة. ابدأ التنفيذ.`);
      };
    });
  });
  $('#deliver') && ($('#deliver').onclick = () => set('delivered', t.owner, `${user(t.worker).name} سلّم العمل على «${t.title}». أكّد الاستلام لتحويل المبلغ.`));
  $('#confirm') && ($('#confirm').onclick = () => { t.escrow.state = 'released'; set('completed', t.worker, `تم تحويل ${money(t.escrow.amount - t.escrow.fee)} إليك من «${t.title}».`); });
  $('#dispute') && ($('#dispute').onclick = () => set('disputed', t.worker, `فتح صاحب المهمة نزاعاً على «${t.title}»؛ المبلغ مجمّد حتى المراجعة.`));
  $('#resRefund') && ($('#resRefund').onclick = () => { t.escrow.state = 'refunded'; set('cancelled', t.owner, `تم استرجاع المبلغ لك عن «${t.title}».`); });
  $('#resPay') && ($('#resPay').onclick = () => { t.escrow.state = 'released'; set('completed', t.worker, `تم صرف المبلغ لك عن «${t.title}» بعد مراجعة النزاع.`); });
  $('#cancel') && ($('#cancel').onclick = () => set('cancelled'));
  const rv = $('#rv');
  if (rv) rv.onsubmit = e => {
    e.preventDefault();
    const to = t.owner === me() ? t.worker : t.owner;
    S.reviews.push({ task: t.id, from: me(), to, stars: +$('#rs').value, text: $('#rt').value.trim(), at: now() });
    save(); notify(to, `${user(me()).name} قيّمك على «${t.title}»`, `#profile-${to}`); toast('شكراً لتقييمك'); route();
  };
};

/* ---------- ملف الأعمال ----------
   نوعان: «منجزة على المنصة» تُؤخذ من المهام المكتملة وتقييماتها فلا يمكن تزييفها،
   و«أضافها المنفّذ» لأعمال سابقة خارج المنصة، مع صورة ورابط اختياريين. */
const MAX_WORKS = 8;
const CAT_ICON = { 'توصيل': '🚚', 'تنظيف': '🧽', 'صيانة وإصلاح': '🔧', 'تصميم وبرمجة': '🎨', 'ترجمة وكتابة': '✍️', 'دروس وتعليم': '📚', 'تصوير': '📷', 'ذكاء اصطناعي': '🤖', 'أخرى': '🧰' };
const SAFE_IMG = /^data:image\/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$/;
const SAFE_URL = /^https?:\/\/[^\s"'<>]+$/i;
const hostOf = u => { try { return SAFE_URL.test(u || '') ? new URL(u).hostname : ''; } catch { return ''; } };
const seedPortfolio = () => [
  { id: 'w1', by: 'u2', title: 'تركيب 3 وحدات سبليت لفيلا', desc: 'تركيب وتمديد نحاس وتصريف لثلاث غرف في مشرف، مع اختبار الضغط وتشغيل تجريبي.', cat: 'صيانة وإصلاح', month: '2026-06', img: '', link: '', at: now() - 90 * day },
  { id: 'w2', by: 'u2', title: 'صيانة لوحة كهرباء منزلية', desc: 'استبدال قواطع تالفة وترتيب الأسلاك وتوثيق اللوحة بالأسماء.', cat: 'صيانة وإصلاح', month: '2026-03', img: '', link: '', at: now() - 200 * day },
  { id: 'w3', by: 'u3', title: 'هوية بصرية لمقهى في السالمية', desc: 'شعار ولوحة ألوان وقوالب منشورات إنستغرام، مع دليل استخدام مختصر.', cat: 'تصميم وبرمجة', month: '2026-08', img: '', link: '', at: now() - 60 * day },
  { id: 'w4', by: 'u3', title: '20 منشوراً بمساعدة الذكاء الاصطناعي', desc: 'كتابة محتوى تسويقي لمتجر عطور ومراجعته يدوياً قبل النشر.', cat: 'ذكاء اصطناعي', month: '2026-09', img: '', link: '', at: now() - 40 * day }
];
S.portfolio ||= seedPortfolio();
const saveOk = () => { try { localStorage.setItem(KEY, JSON.stringify(S)); return true; } catch { return false; } };
const worksOf = id => S.portfolio.filter(w => w.by === id).sort((a, b) => (b.month || '').localeCompare(a.month || '') || b.at - a.at);
const doneWorks = id => S.tasks.filter(t => t.status === 'completed' && t.worker === id);
const workCount = id => worksOf(id).length + doneWorks(id).length;
const monthLabel = m => { if (!/^\d{4}-\d{2}$/.test(m || '')) return ''; const [y, mo] = m.split('-'); return new Date(+y, +mo - 1).toLocaleDateString('ar-KW', { month: 'long', year: 'numeric' }); };
const thumb = (img, cat) => SAFE_IMG.test(img || '') ? `<img class="wimg" src="${img}" alt="" loading="lazy">` : `<div class="wimg ph" aria-hidden="true">${CAT_ICON[cat] || '🧰'}</div>`;

function portfolioSection(id, mine) {
  const done = doneWorks(id), works = worksOf(id);
  const doneCards = done.map(t => {
    const rv = S.reviews.find(r => r.task === t.id && r.to === id);
    return `<a class="card work" href="#task-${t.id}">${thumb('', t.cat)}<div class="wb"><span class="chip ok">✔ منجزة على المنصة</span>
      <h3>${esc(t.title)}</h3><div class="mute">${esc(t.cat)} · ${esc(t.area)}</div>
      ${rv ? `<div><span class="stars">${stars(rv.stars)}</span> <span class="mute">«${esc(rv.text.slice(0, 60))}»</span></div>` : ''}</div></a>`;
  });
  const own = works.map(w => `<article class="card work">${SAFE_IMG.test(w.img || '') ? `<button class="wzoom" data-wzoom="${esc(w.id)}" aria-label="تكبير صورة ${esc(w.title)}">${thumb(w.img, w.cat)}</button>` : thumb('', w.cat)}
    <div class="wb"><span class="chip">أضافها المنفّذ</span><h3>${esc(w.title)}</h3>
    <div class="mute">${esc(w.cat)}${monthLabel(w.month) ? ' · ' + monthLabel(w.month) : ''}</div>
    ${w.desc ? `<p>${esc(w.desc)}</p>` : ''}
    ${hostOf(w.link) ? `<a href="${esc(w.link)}" target="_blank" rel="noopener nofollow ugc">🔗 ${esc(hostOf(w.link))} ↗</a>` : ''}
    <div class="row fit">${mine ? `<button class="sec" data-wedit="${esc(w.id)}">تعديل</button><button class="danger" data-wdel="${esc(w.id)}">حذف</button>`
      : `<button class="danger" data-report="work:${esc(w.id)}">🚩 إبلاغ</button>`}</div></div></article>`);
  const cards = [...doneCards, ...own];
  return `<h2>ملف الأعمال <span class="mute">(${cards.length})</span></h2>
  <p class="mute">«منجزة على المنصة» مأخوذة من مهام مكتملة وتقييمات أصحابها. «أضافها المنفّذ» أعمال سابقة يعرضها بنفسه ولم تتحقق منها المنصة.</p>
  ${mine ? `<div class="row fit" style="margin-bottom:10px"><button id="wAdd" ${works.length >= MAX_WORKS ? 'disabled' : ''}>＋ أضف عملاً</button>
    <span class="mute">${works.length}/${MAX_WORKS}</span></div>` : ''}
  <div class="grid">${cards.join('') || `<p class="mute">${mine ? 'أضف أول عمل لك: صورة ووصف قصير يكفيان ليثق بك أصحاب المهام.' : 'لم يُضف أعمالاً بعد.'}</p>`}</div>`;
}

// تصغير الصورة في المتصفح قبل حفظها حتى لا تمتلئ مساحة التخزين.
function shrinkImage(file, max = 900) {
  return new Promise((res, rej) => {
    if (!/^image\/(jpeg|png|webp|gif)$/.test(file.type)) return rej(new Error('type'));
    const url = URL.createObjectURL(file), img = new Image();
    img.onload = () => {
      const k = Math.min(1, max / Math.max(img.width, img.height)), c = document.createElement('canvas');
      c.width = Math.round(img.width * k); c.height = Math.round(img.height * k);
      c.getContext('2d').drawImage(img, 0, 0, c.width, c.height); URL.revokeObjectURL(url);
      let q = .8, out = c.toDataURL('image/jpeg', q);
      while (out.length > 220000 && q > .4) { q -= .1; out = c.toDataURL('image/jpeg', q); }
      res(out);
    };
    img.onerror = () => { URL.revokeObjectURL(url); rej(new Error('decode')); };
    img.src = url;
  });
}

function editWork(w) {
  const isNew = !w;
  w ||= { id: 'w' + now().toString(36), by: me(), title: '', desc: '', cat: CATS[0], month: '', img: '', link: '', at: now() };
  let img = w.img;
  dialog(`<h3>${isNew ? 'أضف عملاً' : 'تعديل العمل'}</h3><form id="wf">
  <label for="wt">العنوان</label><input id="wt" required maxlength="80" value="${esc(w.title)}" placeholder="مثال: تركيب مطبخ ألمنيوم في الجهراء">
  <label for="wd">ماذا عملت؟</label><textarea id="wd" rows="3" maxlength="300" placeholder="المطلوب، ما قمت به، والنتيجة">${esc(w.desc)}</textarea>
  <div class="row"><div><label for="wc">التصنيف</label><select id="wc">${optionList(CATS, w.cat)}</select></div>
  <div><label for="wm">التاريخ</label><input id="wm" type="month" value="${esc(w.month)}" max="${new Date().toISOString().slice(0, 7)}"></div></div>
  <label for="wi">صورة (اختياري)</label><input id="wi" type="file" accept="image/jpeg,image/png,image/webp">
  <div id="wprev">${SAFE_IMG.test(img) ? `<img class="wimg" src="${img}" alt="">` : ''}</div>
  <label for="wl">رابط (اختياري)</label><input id="wl" type="url" inputmode="url" maxlength="300" value="${esc(w.link)}" placeholder="https://…">
  <p class="mute">اعرض أعمالك أنت فقط. لا تضع أرقام هواتف أو صوراً فيها بيانات عملاء. العمل المنسوب لغيرك يُحذف عند الإبلاغ.</p>
  <p id="wst" role="status" class="mute"></p>
  <div class="row fit"><button>حفظ</button><button type="button" class="sec" id="wcancel">إلغاء</button></div></form>`, d => {
    $('#wcancel', d).onclick = closeDlg;
    $('#wi', d).onchange = async e => {
      const f = e.target.files[0]; if (!f) return;
      $('#wst', d).textContent = 'جارٍ تجهيز الصورة…';
      try { img = await shrinkImage(f); $('#wprev', d).innerHTML = `<img class="wimg" src="${img}" alt="">`; $('#wst', d).textContent = ''; }
      catch { e.target.value = ''; $('#wst', d).textContent = 'تعذّرت قراءة الصورة. اختر صورة JPG أو PNG أو WebP.'; }
    };
    $('#wf', d).onsubmit = e => {
      e.preventDefault();
      const link = $('#wl', d).value.trim();
      if (link && !hostOf(link)) { $('#wst', d).textContent = 'الرابط يجب أن يبدأ بـ https://'; return; }
      const before = { ...w };
      Object.assign(w, { title: $('#wt', d).value.trim(), desc: $('#wd', d).value.trim(), cat: $('#wc', d).value, month: $('#wm', d).value, img, link });
      if (isNew) S.portfolio.push(w);
      if (!saveOk()) {
        if (isNew) S.portfolio.pop(); else Object.assign(w, before);
        $('#wst', d).textContent = 'مساحة التخزين في متصفحك ممتلئة. احذف عملاً قديماً أو اختر صورة أصغر.'; return;
      }
      closeDlg(); toast(isNew ? 'أُضيف العمل إلى ملفك' : 'حُفظ التعديل'); route();
    };
  });
}

views.profile = id => {
  if (id === 'me') id = me();
  const u = S.users[id]; if (!u) return '<p class="card">المستخدم غير موجود.</p>';
  const r = rating(id), revs = S.reviews.filter(x => x.to === id), mine = id === me();
  const done = S.tasks.filter(t => t.status === 'completed' && (t.worker === id || t.owner === id)).length;
  let h = `<div class="card"><h1>${esc(u.name)} ${badge(id)}</h1>
  <p>${r.n ? `<span class="stars">${stars(r.avg)}</span> ${r.avg.toFixed(1)} (${r.n} تقييم)` : 'لا تقييمات بعد'} · ${done} مهمة مكتملة</p>
  ${u.skills ? `<p class="mute">المهارات: ${esc(u.skills)}</p>` : ''}
  ${mine ? '' : `<button class="danger" data-report="user:${id}">🚩 إبلاغ عن المستخدم</button>`}</div>`;
  if (mine) {
    const vs = u.vstatus;
    h += `<div class="card"><h3>توثيق الهوية</h3>` + (u.verified ? '<p class="chip ok">✔ هويتك موثّقة</p>' :
      vs === 'pending' ? '<p class="chip warn">⏳ قيد المراجعة</p>' :
        `<p class="mute">أدخل رقمك المدني (12 رقماً) وارفع صورة البطاقة. لا نعرض هذه البيانات لأي مستخدم، وتُراجع يدوياً.</p>
        <form id="vf"><label for="vc">الرقم المدني</label><input id="vc" inputmode="numeric" pattern="\\d{12}" required>
        <label for="vi">صورة البطاقة</label><input id="vi" type="file" accept="image/*" required>
        <label for="vs">مهاراتك</label><input id="vs" value="${esc(u.skills)}" maxlength="120"><br><button>إرسال للتوثيق</button></form>
        <p class="mute">⚙ تجريبي: لا يُرفع أي ملف فعلياً، ويُعتمد الطلب تلقائياً بعد ثوانٍ.</p>`) + '</div>';
  }
  h += portfolioSection(id, mine);
  h += `<h2>التقييمات</h2>` + (revs.map(x => `<div class="card"><span class="stars">${stars(x.stars)}</span> <b>${esc(user(x.from).name)}</b><p>${esc(x.text)}</p></div>`).join('') || '<p class="mute">لا توجد تقييمات.</p>');
  if (mine && !S.tasks.some(t => t.status === 'completed' && t.worker === id)) h += san3aCard('طوّر مهارتك قبل أول عرض', 'خطة «صنعة · تعلّم» الشخصية تقترح لك مساراً يناسب وقتك ومهاراتك. ويمكنك نشر بطاقة مهارتك في ركن مجتمعها ليعرفك الآخرون.');
  if (mine) h += `<h2>مهامي</h2><div class="grid">${S.tasks.filter(t => t.owner === id || t.worker === id).map(taskCard).join('') || '<p class="mute">لا مهام.</p>'}</div>`;
  return h;
};
views.profile.after = () => {
  $('#wAdd') && ($('#wAdd').onclick = () => editWork());
  document.querySelectorAll('[data-wedit]').forEach(b => b.onclick = () => editWork(S.portfolio.find(w => w.id === b.dataset.wedit && w.by === me())));
  document.querySelectorAll('[data-wdel]').forEach(b => b.onclick = () => {
    const w = S.portfolio.find(x => x.id === b.dataset.wdel && x.by === me()); if (!w) return;
    dialog(`<h3>حذف «${esc(w.title)}»؟</h3><p class="mute">لا يمكن التراجع عن الحذف.</p>
    <div class="row fit"><button class="danger" id="wyes">حذف</button><button class="sec" id="wno">إلغاء</button></div>`, d => {
      $('#wno', d).onclick = closeDlg;
      $('#wyes', d).onclick = () => { S.portfolio = S.portfolio.filter(x => x !== w); save(); closeDlg(); toast('حُذف العمل'); route(); };
    });
  });
  document.querySelectorAll('[data-wzoom]').forEach(b => b.onclick = () => {
    const w = S.portfolio.find(x => x.id === b.dataset.wzoom); if (!w || !SAFE_IMG.test(w.img)) return;
    dialog(`<img class="wfull" src="${w.img}" alt="${esc(w.title)}"><h3>${esc(w.title)}</h3><p class="mute">${esc(w.desc)}</p><button class="sec" id="wz">إغلاق</button>`, d => { $('#wz', d).onclick = closeDlg; });
  });
  const vf = $('#vf'); if (!vf) return;
  vf.onsubmit = e => {
    e.preventDefault(); const u = S.users[me()], uid = me();
    u.skills = $('#vs').value.trim(); u.vstatus = 'pending'; save(); route();
    setTimeout(() => { S.users[uid].verified = true; S.users[uid].vstatus = 'done'; save(); notify(uid, 'تم توثيق هويتك ✔', '#profile-me'); if (/^#\/?profile/.test(location.hash)) route(); }, 4000);
  };
};

views.notifications = () => {
  const list = S.notifs.filter(n => n.to === me());
  S.notifs.forEach(n => { if (n.to === me()) n.read = true; }); save();
  const perm = 'Notification' in window ? Notification.permission : 'unsupported';
  return `<h1>الإشعارات</h1><div class="card"><h3>التنبيهات الفورية</h3>
  <label><input type="checkbox" id="pp" style="width:auto" ${S.prefs.push ? 'checked' : ''}> إشعارات المتصفح/الجوال (الحالة: ${perm})</label>
  <label for="wa">رقم واتساب (اختياري)</label><input id="wa" inputmode="tel" placeholder="965XXXXXXXX" value="${esc(S.prefs.wa)}">
  <p class="mute">إشعارات واتساب الفعلية تحتاج خادماً وحساب WhatsApp Business API؛ هنا يمكنك فقط معاينة الرسالة. الإشعارات في الخلفية على الجوال تحتاج خادم Web Push.</p>
  <div class="row fit"><button id="sv">حفظ</button><button class="sec" id="tst">إرسال إشعار تجريبي</button></div><p id="waOut"></p></div>
  <h2>السجل</h2>` + (list.map(n => `<a class="card notif" style="text-decoration:none;color:inherit" href="${esc(n.link)}"><span>${esc(n.text)}</span><span class="mute">${ago(n.at)}</span></a>`).join('') || '<p class="mute">لا إشعارات.</p>');
};
views.notifications.after = () => {
  $('#sv').onclick = async () => {
    const wa = $('#wa').value.replace(/\D/g, ''); S.prefs.wa = wa; S.prefs.push = $('#pp').checked;
    if (S.prefs.push && 'Notification' in window && Notification.permission === 'default') await Notification.requestPermission();
    save(); toast('تم الحفظ'); route();
  };
  $('#tst').onclick = () => {
    notify(me(), 'هذا إشعار تجريبي من صنعة');
    if (S.prefs.wa) $('#waOut').innerHTML = `معاينة رسالة واتساب إلى ${esc(S.prefs.wa)}: <a href="https://wa.me/${esc(S.prefs.wa)}?text=${encodeURIComponent('إشعار تجريبي من صنعة')}" target="_blank" rel="noopener">افتح واتساب ↗</a>`;
  };
};


/* ---------- المحادثة المباشرة ---------- */
// كل محادثة مرتبطة بمهمة وبين طرفين، أحدهما صاحب المهمة.
const pairKey = (tid, a, b) => `${tid}:${[a, b].sort().join('-')}`;
const thread = (tid, a, b) => S.messages.filter(m => m.key === pairKey(tid, a, b));
const unreadIn = (tid, other) => thread(tid, me(), other).filter(m => m.to === me() && !m.read).length;
// تنبيه عند محاولة نقل التواصل أو الدفع خارج المنصة
const RISKY = /(\+?\d[\s-]?){8,}|واتس|whats\s?app|تحويل\s*بنك|حو[ّ]?ل\s*لي|خارج\s*المنصة|كاش|ايبان|iban|@[a-z0-9_.]{3,}/i;
const canChat = (t, a, b) => t && a !== b && S.users[a] && S.users[b] && (t.owner === a || t.owner === b);

views.chat = (tid, other) => {
  const t = task(tid);
  if (!canChat(t, me(), other)) return '<p class="card">لا يمكن فتح هذه المحادثة. المحادثات تكون بين صاحب المهمة والمنفّذين فقط.</p>';
  const msgs = thread(t.id, me(), other);
  msgs.forEach(m => { if (m.to === me()) m.read = true; }); save();
  return `<a href="#task-${t.id}">← ${esc(t.title)}</a>
  <div class="card"><div class="row fit" style="justify-content:space-between"><div>💬 محادثة مع ${uname(other)}</div>
  <button class="danger" data-report="user:${other}">🚩 إبلاغ</button></div>
  <p class="mute">🔒 الرسائل محفوظة في المنصة ويُرجع إليها عند النزاع. لا تشارك أرقام هواتف أو بيانات بنكية، ولا تدفع خارج المنصة.</p>
  <div class="chat" id="chatBox" aria-live="polite">${msgs.map(m => `<div class="msg ${m.from === me() ? 'mine' : ''}">
    <div>${esc(m.text).replace(/\n/g, '<br>')}</div><div class="mute">${ago(m.at)}${m.from === me() ? (m.read ? ' · ✓✓ قُرئت' : ' · ✓') : ''}</div>
    ${m.risky ? '<div class="chip warn">⚠ تذكير: الدفع والتواصل خارج المنصة يُسقط حماية الأمانة</div>' : ''}</div>`).join('') || '<p class="mute" style="text-align:center">ابدأ المحادثة: اسأل عن التفاصيل والموعد قبل اختيار العرض.</p>'}</div>
  <form id="cf" class="row" style="margin-top:10px"><label for="ct" class="sr">رسالتك</label>
  <textarea id="ct" rows="2" maxlength="1000" required placeholder="اكتب رسالتك… (Enter للإرسال، Shift+Enter لسطر جديد)" style="flex:5"></textarea><button style="flex:1">إرسال</button></form></div>`;
};
views.chat.after = (tid, other) => {
  const t = task(tid); if (!canChat(t, me(), other)) return;
  const box = $('#chatBox'); box.scrollTop = box.scrollHeight;
  const ta = $('#ct');
  ta.focus();
  ta.onkeydown = e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); $('#cf').requestSubmit(); } };
  $('#cf').onsubmit = e => {
    e.preventDefault();
    const text = ta.value.trim(); if (!text) return;
    const risky = RISKY.test(text);
    S.messages.push({ id: now() + Math.random(), key: pairKey(t.id, me(), other), task: t.id, from: me(), to: other, text, at: now(), read: false, risky });
    save();
    notify(other, `رسالة من ${user(me()).name} بخصوص «${t.title}»: ${text.slice(0, 60)}`, `#chat-${t.id}-${me()}`);
    if (risky) toast('⚠ تنبيه: لا تشارك أرقاماً أو بيانات دفع؛ الحماية تشمل ما يتم داخل المنصة فقط');
    route();
  };
};

views.inbox = () => {
  const mine = S.messages.filter(m => m.from === me() || m.to === me());
  const threads = {};
  mine.forEach(m => { const o = m.from === me() ? m.to : m.from; (threads[m.key] ||= { task: m.task, other: o, last: m, unread: 0 }); const th = threads[m.key]; if (m.at >= th.last.at) th.last = m; if (m.to === me() && !m.read) th.unread++; });
  const list = Object.values(threads).sort((a, b) => b.last.at - a.last.at);
  return `<h1>الرسائل</h1>` + (list.map(th => { const t = task(th.task); return `<a class="card notif ${th.unread ? 'unread' : ''}" style="text-decoration:none;color:inherit" href="#chat-${th.task}-${th.other}">
    <span><b>${esc(user(th.other).name)}</b> · <span class="mute">${esc(t?.title || '')}</span><br>${esc(th.last.text.slice(0, 80))}</span>
    <span class="mute">${ago(th.last.at)}${th.unread ? ` <span class="chip ok">${th.unread} جديدة</span>` : ''}</span></a>`; }).join('') || '<p class="card mute">لا توجد محادثات بعد. افتح مهمة واضغط «راسل صاحب المهمة».</p>');
};

/* ---------- صفحات قانونية ---------- */
const legalNote = '<p class="card mute">⚠ هذا نص إرشادي أولي ولا يُغني عن مراجعة محامٍ مرخّص في الكويت قبل الإطلاق الفعلي، خصوصاً بنود الدفع وحماية البيانات.</p>';
views.terms = () => `<div class="prose"><h1>شروط الاستخدام</h1>${legalNote}
<h2>1. طبيعة المنصة</h2><p>«صنعة» وسيط يربط أصحاب المهام بالمنفّذين، وليست طرفاً في العمل نفسه ولا صاحبة عمل للمنفّذين.</p>
<h2>2. الحسابات</h2><p>يجب أن يكون عمرك 18 سنة فأكثر وأن تقدم معلومات صحيحة. تحتفظ المنصة بحق تعليق أي حساب مخالف.</p>
<h2>3. العروض والاتفاق</h2><p>يقدّم المنفّذ سعره وموعده، ويُعدّ اختيار صاحب المهمة للعرض وإيداع المبلغ في الأمانة اتفاقاً ملزماً بينهما.</p>
<h2>4. نظام الأمانة</h2><p>يُحجز المبلغ لدى المنصة عند اختيار العرض، ويُحوَّل للمنفّذ بعد تأكيد صاحب المهمة الاستلام، أو تلقائياً بعد 72 ساعة من التسليم دون اعتراض. تُخصم عمولة المنصة (5%) من المنفّذ.</p>
<h2>5. الدفع خارج المنصة</h2><p>يُمنع طلب الدفع أو قبوله خارج المنصة، ويُعدّ مخالفة تُوجب إيقاف الحساب.</p>
<h2>6. المحتوى المحظور</h2><p>الأعمال المخالفة لقوانين دولة الكويت، والمحتوى المسيء، والاحتيال، وانتحال الهوية.</p>
<h2>7. التقييمات والإبلاغ</h2><p>التقييمات تعكس تجارب حقيقية بعد إتمام المهمة. يمكن الإبلاغ عن أي مستخدم أو مهمة وتراجع الإدارة البلاغات.</p>
<h2>8. النزاعات</h2><p>عند فتح نزاع يُجمَّد المبلغ وتراجع الإدارة الأدلة وتقرر الاسترجاع أو الصرف.</p>
<h2>9. المسؤولية</h2><p>لا تضمن المنصة جودة العمل، وتقتصر مسؤوليتها على إدارة الأمانة والإجراءات الموصوفة هنا بالقدر الذي يسمح به القانون.</p></div>`;
views.privacy = () => `<div class="prose"><h1>سياسة الخصوصية</h1>${legalNote}
<h2>البيانات التي نجمعها</h2><ul><li>الاسم ووسيلة التواصل والمنطقة.</li><li>الرسائل المتبادلة داخل المنصة، وتُراجَع فقط عند بلاغ أو نزاع.</li><li>بيانات توثيق الهوية (الرقم المدني وصورة البطاقة) لأغراض التحقق فقط.</li><li>سجل المهام والعروض والتقييمات.</li></ul>
<h2>كيف نستخدمها</h2><p>لتشغيل الخدمة، ومنع الاحتيال، وإرسال الإشعارات التي طلبتها (المتصفح أو واتساب).</p>
<h2>المشاركة</h2><p>لا نبيع بياناتك. لا يُعرض الرقم المدني لأي مستخدم، ويظهر فقط وسم «موثّق». قد نشارك البيانات مع الجهات المختصة بموجب القانون.</p>
<h2>حقوقك</h2><p>يمكنك طلب الاطلاع على بياناتك أو تصحيحها أو حذف حسابك عبر الدعم.</p>
<h2>النسخة التجريبية</h2><p>في هذه النسخة تُحفظ بياناتك في متصفحك فقط ولا تُرسل إلى أي خادم.</p></div>`;
views.refund = () => `<div class="prose"><h1>سياسة الاسترجاع</h1>${legalNote}
<ul><li><b>قبل اختيار عرض:</b> لا يُحجز أي مبلغ.</li>
<li><b>بعد الحجز وقبل التسليم:</b> يُسترجع المبلغ كاملاً إذا لم يبدأ المنفّذ أو اتفق الطرفان على الإلغاء.</li>
<li><b>بعد التسليم:</b> لديك 72 ساعة للتأكيد أو فتح نزاع، وإلا يُصرف المبلغ للمنفّذ.</li>
<li><b>النزاع:</b> يُجمَّد المبلغ وتقرر الإدارة استرجاعاً كاملاً أو جزئياً أو صرفاً حسب الأدلة.</li>
<li><b>مدة الاسترجاع:</b> تُعاد المبالغ المسترجعة بالوسيلة نفسها خلال مدة تحددها بوابة الدفع.</li></ul></div>`;
views.safety = () => `<div class="prose"><h1>الأمان والثقة</h1>
<ul><li>✔ <b>توثيق الهوية:</b> وسم «موثّق» للمنفّذين الذين راجعنا هويتهم.</li>
<li>⭐ <b>تقييمات بعد كل مهمة</b> من الطرفين.</li>
<li>🔒 <b>الأمانة:</b> المال محجوز حتى تؤكد الاستلام.</li>
<li>🚩 <b>إبلاغ</b> عن أي مستخدم أو مهمة بضغطة.</li>
<li>⚠ لا تدفع ولا تتواصل خارج المنصة، ولا تشارك بيانات بطاقتك.</li></ul></div>`;

/* ---------- الإبلاغ ---------- */
document.addEventListener('click', e => {
  const b = e.target.closest('[data-report]'); if (!b) return;
  const [type, id] = b.dataset.report.split(':');
  dialog(`<h3>إبلاغ</h3><label for="rr">السبب</label><select id="rr">${optionList(REPORT_REASONS)}</select>
  <label for="rd">تفاصيل</label><textarea id="rd" rows="3" maxlength="500"></textarea>
  <div class="row fit"><button id="rs1">إرسال البلاغ</button><button class="sec" id="rc">إلغاء</button></div>`, d => {
    $('#rc', d).onclick = closeDlg;
    $('#rs1', d).onclick = () => {
      S.reports.push({ type, id, by: me(), reason: $('#rr', d).value, details: $('#rd', d).value, at: now() }); save();
      closeDlg(); toast('تم استلام البلاغ وسنراجعه');
    };
  });
});

/* ---------- التوجيه ---------- */
function route() {
  // روابط الصفحات بالشكل #task-7 أو #chat-1-u2 (عارض claude.ai يمرّر الأسماء البسيطة فقط)، مع قبول الشكل القديم #/task/7.
  const [name = '', ...args] = location.hash.replace(/^#\/?/, '').split(/[-/]/);
  const key = name || 'home', v = views[key] || views.home;
  $('#app').innerHTML = v(...args);
  v.after?.(...args);
  window.scrollTo(0, 0); $('#app').focus({ preventScroll: true }); updateBadge(); markNav(key, args);
  document.title = 'سوق صنعة';
}
window.addEventListener('hashchange', route);

// إبراز القسم الحالي في القائمة العلوية
function markNav(name, args) {
  const cur = name === 'profile' ? (args[0] === 'me' || args[0] === S.me ? 'profile' : '') : name;
  document.querySelectorAll('[data-nav]').forEach(a => {
    const on = a.dataset.nav.split(' ').includes(cur);
    a.classList.toggle('on', on);
    on ? a.setAttribute('aria-current', 'page') : a.removeAttribute('aria-current');
  });
}

// ميل خفيف ثلاثي الأبعاد لبطاقات المهام مع حركة الفأرة (لا يعمل على اللمس أو مع «تقليل الحركة»)
if (matchMedia('(hover: hover) and (pointer: fine)').matches && !matchMedia('(prefers-reduced-motion: reduce)').matches) {
  let tilted = null;
  const reset = () => { if (tilted) tilted.style.transform = ''; tilted = null; };
  document.addEventListener('pointermove', e => {
    const c = e.target.closest?.('.task');
    if (c !== tilted) reset();
    if (!c) return;
    tilted = c;
    const r = c.getBoundingClientRect(), x = (e.clientX - r.left) / r.width - .5, y = (e.clientY - r.top) / r.height - .5;
    c.style.transform = `perspective(900px) rotateX(${(-y * 6).toFixed(2)}deg) rotateY(${(x * 7).toFixed(2)}deg) translateY(-3px)`;
  });
  document.addEventListener('pointerout', e => { if (!e.relatedTarget) reset(); });
}

function initUserSwitch() {
  const s = $('#userSwitch');
  s.innerHTML = Object.entries(S.users).map(([k, u]) => `<option value="${k}" ${k === me() ? 'selected' : ''}>${esc(u.name)}</option>`).join('');
  s.onchange = () => { S.me = s.value; save(); route(); toast('تم التبديل إلى: ' + user(me()).name); };
}

/* ---------- PWA ---------- */
let deferred;
window.addEventListener('beforeinstallprompt', e => { e.preventDefault(); deferred = e; $('#installBtn').hidden = false; });
$('#installBtn').onclick = async () => { if (!deferred) return; deferred.prompt(); await deferred.userChoice; deferred = null; $('#installBtn').hidden = true; };
if ('serviceWorker' in navigator && location.protocol.startsWith('http')) navigator.serviceWorker.register('sw.js').catch(() => { });

initUserSwitch(); route();
