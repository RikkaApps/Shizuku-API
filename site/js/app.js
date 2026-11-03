'use strict';
/* مهام الكويت — نموذج تجريبي. كل الحالة في localStorage؛ الدفع/التوثيق/الإشعارات محاكاة. */

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
    reports: [], notifs: [], prefs: { push: false, wa: '' }
  };
}
let S;
try { S = JSON.parse(localStorage.getItem(KEY)) || seed(); } catch { S = seed(); }
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
const uname = id => `<a href="#/profile/${id}">${esc(user(id).name)}</a> ${badge(id)}`;
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
      navigator.serviceWorker?.ready.then(r => r.showNotification('مهام الكويت', { body: text, icon: 'icon.svg', data: location.pathname + link }))
        .catch(() => new Notification('مهام الكويت', { body: text }));
    }
  }
  updateBadge();
}
function updateBadge() {
  const n = S.notifs.filter(x => x.to === me() && !x.read).length, b = $('#badge');
  b.hidden = !n; b.textContent = n;
}

/* ---------- نافذة حوار ---------- */
function dialog(html, onOpen) { const d = $('#dlg'); d.innerHTML = html; d.showModal(); onOpen?.(d); return d; }
const closeDlg = () => $('#dlg').close();

/* ---------- العروض (مكوّنات) ---------- */
function taskCard(t) {
  const r = rating(t.owner);
  return `<article class="card task"><h3><a class="t" href="#/task/${t.id}">${esc(t.title)}</a></h3>
  <div><span class="chip">${esc(t.cat)}</span><span class="chip">📍 ${esc(t.area)}</span><span class="chip ${STATUS[t.status][1]}">${STATUS[t.status][0]}</span></div>
  <p class="mute">${esc(t.desc.slice(0, 90))}${t.desc.length > 90 ? '…' : ''}</p>
  <div class="row fit"><span class="price">${money(t.budget)}</span><span class="mute">${t.bids.length} عروض · ${ago(t.created)}</span></div>
  <div class="mute">${esc(user(t.owner).name)} ${r.n ? `<span class="stars">${stars(r.avg)}</span>` : ''}</div></article>`;
}
const optionList = (arr, sel) => arr.map(a => `<option ${a === sel ? 'selected' : ''}>${esc(a)}</option>`).join('');

/* ---------- الصفحات ---------- */
const views = {};

views.home = () => {
  const open = S.tasks.filter(t => t.status === 'open').slice(0, 6);
  return `<section class="hero card"><h1>أنجز مهمتك أو اربح من مهارتك — بأمان</h1>
  <p>منصة مهام في الكويت: عروض أسعار، دفع محجوز بنظام الأمانة، تقييمات حقيقية وتوثيق هوية.</p>
  <div class="row fit" style="justify-content:center"><a class="btn" href="#/new">انشر مهمة</a><a class="btn sec" href="#/tasks">تصفّح المهام</a></div></section>
  <h2>كيف تبني المنصة الثقة؟</h2>
  <div class="steps">
   <div class="card"><div class="n">1️⃣</div><b>عروض</b><p class="mute">المنفّذون يقدمون سعرهم وموعدهم وأنت تختار.</p></div>
   <div class="card"><div class="n">2️⃣</div><b>أمانة</b><p class="mute">المبلغ يُحجز عند المنصة ولا يصل للمنفّذ إلا بتأكيدك.</p></div>
   <div class="card"><div class="n">3️⃣</div><b>تأكيد</b><p class="mute">استلمت العمل؟ اضغط تأكيد ويُحوَّل المبلغ.</p></div>
   <div class="card"><div class="n">4️⃣</div><b>تقييم</b><p class="mute">كل مهمة تنتهي بتقييم وتعليق للطرفين.</p></div>
  </div>
  <h2>أحدث المهام</h2><div class="grid">${open.map(taskCard).join('') || '<p class="mute">لا توجد مهام.</p>'}</div>`;
};

let tf = { q: '', area: '', cat: '', view: 'list', sort: 'new' };
views.tasks = () => `<h1>المهام</h1>
 <div class="card"><div class="row">
  <div><label for="fq">بحث</label><input id="fq" value="${esc(tf.q)}" placeholder="مثال: تركيب"></div>
  <div><label for="fa">المنطقة</label><select id="fa"><option value="">كل الكويت</option>${optionList(Object.keys(AREAS), tf.area)}</select></div>
  <div><label for="fc">التصنيف</label><select id="fc"><option value="">الكل</option>${optionList(CATS, tf.cat)}</select></div>
  <div><label for="fs">الترتيب</label><select id="fs"><option value="new" ${tf.sort === 'new' ? 'selected' : ''}>الأحدث</option><option value="price" ${tf.sort === 'price' ? 'selected' : ''}>الأعلى ميزانية</option></select></div>
 </div></div>
 <div class="tabs"><button data-v="list" class="${tf.view === 'list' ? 'on' : ''}">☰ قائمة</button><button data-v="map" class="${tf.view === 'map' ? 'on' : ''}">🗺 خريطة</button></div>
 <div id="results"></div>`;
views.tasks.after = () => {
  const draw = () => {
    let l = S.tasks.filter(t => t.status !== 'cancelled' &&
      (!tf.area || t.area === tf.area) && (!tf.cat || t.cat === tf.cat) &&
      (!tf.q || (t.title + t.desc).includes(tf.q)));
    l.sort((a, b) => tf.sort === 'price' ? b.budget - a.budget : b.created - a.created);
    const box = $('#results');
    if (tf.view === 'list') { box.innerHTML = `<div class="grid">${l.map(taskCard).join('') || '<p class="mute">لا توجد نتائج.</p>'}</div>`; return; }
    box.innerHTML = '<div id="map" role="application" aria-label="خريطة المهام"></div>';
    if (typeof L === 'undefined') { box.innerHTML = '<p class="card">تعذّر تحميل الخريطة (لا يوجد اتصال).</p>'; return; }
    const m = L.map('map').setView(tf.area ? AREAS[tf.area] : [29.3, 47.95], tf.area ? 11 : 9);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 18, attribution: '© OpenStreetMap' }).addTo(m);
    l.forEach(t => L.marker([t.lat, t.lng]).addTo(m).bindPopup(`<b>${esc(t.title)}</b><br>${money(t.budget)} · ${esc(t.area)}<br><a href="#/task/${t.id}">فتح</a>`));
  };
  $('#fq').oninput = e => { tf.q = e.target.value; draw(); };
  $('#fa').onchange = e => { tf.area = e.target.value; draw(); };
  $('#fc').onchange = e => { tf.cat = e.target.value; draw(); };
  $('#fs').onchange = e => { tf.sort = e.target.value; draw(); };
  document.querySelectorAll('.tabs button').forEach(b => b.onclick = () => { tf.view = b.dataset.v; route(); });
  draw();
};

views.new = () => `<h1>انشر مهمة</h1><form class="card" id="nf">
 <label for="nt">العنوان</label><input id="nt" required maxlength="80">
 <label for="nd">الوصف</label><textarea id="nd" required rows="4" maxlength="800" placeholder="التفاصيل، الموعد المطلوب، أي شروط"></textarea>
 <div class="row"><div><label for="nc">التصنيف</label><select id="nc">${optionList(CATS)}</select></div>
 <div><label for="na">المنطقة</label><select id="na">${optionList(Object.keys(AREAS))}</select></div>
 <div><label for="nb">الميزانية (د.ك)</label><input id="nb" type="number" min="1" step="0.5" required></div></div>
 <p class="mute">لحمايتك: لا تشارك رقم بطاقتك أو كلمات المرور، ولا تدفع خارج المنصة.</p>
 <button>نشر المهمة</button></form>`;
views.new.after = () => {
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
      save(); notify(t.owner, `عرض جديد من ${user(w).name} على «${t.title}»`, `#/task/${t.id}`);
    }, 6000);
    location.hash = '#/task/' + t.id;
  };
};

views.task = id => {
  const t = task(id); if (!t) return '<p class="card">المهمة غير موجودة.</p>';
  const isOwner = t.owner === me(), isWorker = t.worker === me();
  const reviews = S.reviews.filter(r => r.task === t.id);
  let h = `<a href="#/tasks">← المهام</a><div class="card"><h1>${esc(t.title)}</h1>
  <div><span class="chip">${esc(t.cat)}</span><span class="chip">📍 ${esc(t.area)}</span><span class="chip ${STATUS[t.status][1]}">${STATUS[t.status][0]}</span></div>
  <p>${esc(t.desc).replace(/\n/g, '<br>')}</p><p class="price">الميزانية: ${money(t.budget)}</p>
  <p class="mute">صاحب المهمة: ${uname(t.owner)}${t.worker ? ` · المنفّذ: ${uname(t.worker)}` : ''}</p>
  <button class="danger" data-report="task:${t.id}">🚩 إبلاغ عن المهمة</button></div>`;

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
    ${user(me()).verified ? '' : `<p class="mute">المنفّذ غير الموثّق لا يمكنه تقديم عرض فوق ${VERIFY_LIMIT} د.ك. <a href="#/profile/me">وثّق هويتك</a></p>`}
    <button>إرسال العرض</button></form>`;
  }
  h += t.bids.length ? t.bids.map((b, i) => {
    const r = rating(b.by), chosen = t.worker === b.by;
    return `<div class="card ${chosen ? 'escrow' : ''}"><div class="row fit"><b class="price">${money(b.price)}</b><span class="mute">التسليم ${esc(b.date)}</span></div>
    <div>${uname(b.by)} ${r.n ? `<span class="stars">${stars(r.avg)}</span> <span class="mute">(${r.n})</span>` : '<span class="mute">لا تقييمات بعد</span>'}</div>
    <p>${esc(b.note)}</p>
    <div class="row fit">${isOwner && t.status === 'open' ? `<button data-accept="${i}">اختيار العرض والدفع للأمانة</button>` : ''}
    <button class="danger" data-report="user:${b.by}">🚩 إبلاغ</button></div></div>`;
  }).join('') : '<p class="mute">لا توجد عروض بعد.</p>';
  return h;
};
views.task.after = id => {
  const t = task(id); if (!t) return;
  const set = (st, msgTo, msg) => { t.status = st; save(); if (msgTo) notify(msgTo, msg, `#/task/${t.id}`); route(); };
  const bf = $('#bf');
  if (bf) bf.onsubmit = e => {
    e.preventDefault();
    const price = +$('#bp').value;
    if (price > VERIFY_LIMIT && !user(me()).verified) return toast(`العروض فوق ${VERIFY_LIMIT} د.ك تتطلب توثيق الهوية`);
    const b = { by: me(), price, date: $('#bd').value, note: $('#bn').value.trim() };
    const i = t.bids.findIndex(x => x.by === me()); i >= 0 ? t.bids[i] = b : t.bids.push(b);
    save(); notify(t.owner, `عرض جديد من ${user(me()).name}: ${money(price)} على «${t.title}»`, `#/task/${t.id}`);
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
    save(); notify(to, `${user(me()).name} قيّمك على «${t.title}»`, `#/profile/${to}`); toast('شكراً لتقييمك'); route();
  };
};

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
  h += `<h2>التقييمات</h2>` + (revs.map(x => `<div class="card"><span class="stars">${stars(x.stars)}</span> <b>${esc(user(x.from).name)}</b><p>${esc(x.text)}</p></div>`).join('') || '<p class="mute">لا توجد تقييمات.</p>');
  if (mine) h += `<h2>مهامي</h2><div class="grid">${S.tasks.filter(t => t.owner === id || t.worker === id).map(taskCard).join('') || '<p class="mute">لا مهام.</p>'}</div>`;
  return h;
};
views.profile.after = () => {
  const vf = $('#vf'); if (!vf) return;
  vf.onsubmit = e => {
    e.preventDefault(); const u = S.users[me()], uid = me();
    u.skills = $('#vs').value.trim(); u.vstatus = 'pending'; save(); route();
    setTimeout(() => { S.users[uid].verified = true; S.users[uid].vstatus = 'done'; save(); notify(uid, 'تم توثيق هويتك ✔', '#/profile/me'); if (location.hash.startsWith('#/profile')) route(); }, 4000);
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
  <div class="row fit"><button id="sv">حفظ</button><button class="sec" id="tst">إرسال إشعار تجريبي</button></div></div>
  <h2>السجل</h2>` + (list.map(n => `<a class="card notif" style="text-decoration:none;color:inherit" href="${esc(n.link)}"><span>${esc(n.text)}</span><span class="mute">${ago(n.at)}</span></a>`).join('') || '<p class="mute">لا إشعارات.</p>');
};
views.notifications.after = () => {
  $('#sv').onclick = async () => {
    const wa = $('#wa').value.replace(/\D/g, ''); S.prefs.wa = wa; S.prefs.push = $('#pp').checked;
    if (S.prefs.push && 'Notification' in window && Notification.permission === 'default') await Notification.requestPermission();
    save(); toast('تم الحفظ'); route();
  };
  $('#tst').onclick = () => {
    notify(me(), 'هذا إشعار تجريبي من مهام الكويت');
    if (S.prefs.wa) window.open(`https://wa.me/${S.prefs.wa}?text=${encodeURIComponent('إشعار تجريبي من مهام الكويت')}`, '_blank', 'noopener');
  };
};

/* ---------- صفحات قانونية ---------- */
const legalNote = '<p class="card mute">⚠ هذا نص إرشادي أولي ولا يُغني عن مراجعة محامٍ مرخّص في الكويت قبل الإطلاق الفعلي، خصوصاً بنود الدفع وحماية البيانات.</p>';
views.terms = () => `<div class="prose"><h1>شروط الاستخدام</h1>${legalNote}
<h2>1. طبيعة المنصة</h2><p>«مهام الكويت» وسيط يربط أصحاب المهام بالمنفّذين، وليست طرفاً في العمل نفسه ولا صاحبة عمل للمنفّذين.</p>
<h2>2. الحسابات</h2><p>يجب أن يكون عمرك 18 سنة فأكثر وأن تقدم معلومات صحيحة. تحتفظ المنصة بحق تعليق أي حساب مخالف.</p>
<h2>3. العروض والاتفاق</h2><p>يقدّم المنفّذ سعره وموعده، ويُعدّ اختيار صاحب المهمة للعرض وإيداع المبلغ في الأمانة اتفاقاً ملزماً بينهما.</p>
<h2>4. نظام الأمانة</h2><p>يُحجز المبلغ لدى المنصة عند اختيار العرض، ويُحوَّل للمنفّذ بعد تأكيد صاحب المهمة الاستلام، أو تلقائياً بعد 72 ساعة من التسليم دون اعتراض. تُخصم عمولة المنصة (5%) من المنفّذ.</p>
<h2>5. الدفع خارج المنصة</h2><p>يُمنع طلب الدفع أو قبوله خارج المنصة، ويُعدّ مخالفة تُوجب إيقاف الحساب.</p>
<h2>6. المحتوى المحظور</h2><p>الأعمال المخالفة لقوانين دولة الكويت، والمحتوى المسيء، والاحتيال، وانتحال الهوية.</p>
<h2>7. التقييمات والإبلاغ</h2><p>التقييمات تعكس تجارب حقيقية بعد إتمام المهمة. يمكن الإبلاغ عن أي مستخدم أو مهمة وتراجع الإدارة البلاغات.</p>
<h2>8. النزاعات</h2><p>عند فتح نزاع يُجمَّد المبلغ وتراجع الإدارة الأدلة وتقرر الاسترجاع أو الصرف.</p>
<h2>9. المسؤولية</h2><p>لا تضمن المنصة جودة العمل، وتقتصر مسؤوليتها على إدارة الأمانة والإجراءات الموصوفة هنا بالقدر الذي يسمح به القانون.</p></div>`;
views.privacy = () => `<div class="prose"><h1>سياسة الخصوصية</h1>${legalNote}
<h2>البيانات التي نجمعها</h2><ul><li>الاسم ووسيلة التواصل والمنطقة.</li><li>بيانات توثيق الهوية (الرقم المدني وصورة البطاقة) لأغراض التحقق فقط.</li><li>سجل المهام والعروض والتقييمات.</li></ul>
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
  const [, name = '', arg] = location.hash.split('/');
  const key = name || 'home', v = views[key] || views.home;
  $('#app').innerHTML = v(arg);
  v.after?.(arg);
  window.scrollTo(0, 0); $('#app').focus({ preventScroll: true }); updateBadge();
  document.title = 'مهام الكويت';
}
window.addEventListener('hashchange', route);

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
