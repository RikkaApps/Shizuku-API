#!/usr/bin/env python3
"""يجهّز index.html للنشر كصفحة على claude.ai.

عارض claude.ai يضيف بنفسه <!doctype> و<html> و<head> و<body>، فهذا السكربت
يُخرج محتوى الصفحة بدونها: العنوان والأنماط في الأعلى ثم محتوى body.
الاستخدام: python3 site/scripts/build_artifact.py <مجلد الإخراج>
"""
import pathlib
import re
import sys

SITE = pathlib.Path(__file__).resolve().parent.parent
# ما لا يعمل داخل العارض أو يضيفه العارض بنفسه
DROP = re.compile(r'<meta (charset|name="viewport")[^>]*>|<link rel="(manifest|apple-touch-icon)"[^>]*>')

src = (SITE / "index.html").read_text(encoding="utf-8")
head = re.search(r"<head>(.*?)</head>", src, re.S).group(1)
body = re.search(r"<body>(.*?)</body>", src, re.S).group(1)
head = "\n".join(line for line in head.strip().splitlines() if not DROP.search(line))

# المكتبات تُحمّل في العارض من jsDelivr بنفس الإصدارات المضمّنة في vendor/
# (العارض يسمح بالسكربتات من هذا المصدر، ويمنع الأنماط الخارجية فتبقى leaflet.css ملفاً محلياً).
CDN = {
    "vendor/leaflet/leaflet.js": "https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js",
    "../vendor/three/three.module.min.js": "https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.min.js",
    "./vendor/three/three.module.min.js": "https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.min.js",
    "./vendor/three/OrbitControls.js": "https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/controls/OrbitControls.js",
    "./vendor/three/GLTFExporter.js": "https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/exporters/GLTFExporter.js",
    "./vendor/three/STLExporter.js": "https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/exporters/STLExporter.js",
}


def to_cdn(text):
    for local, url in CDN.items():
        text = text.replace(f'"{local}"', f'"{url}"').replace(f"'{local}'", f"'{url}'")
    return text


out = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else SITE / "dist")
(out / "js").mkdir(parents=True, exist_ok=True)
pages = {
    "index.html": head + "\n" + body.strip() + "\n",
    "model.html": (SITE / "model.html").read_text(encoding="utf-8"),
    "js/pin3d.js": (SITE / "js/pin3d.js").read_text(encoding="utf-8"),
}
for name, text in pages.items():
    text = to_cdn(text)
    assert "vendor/three" not in text and "vendor/leaflet/leaflet.js" not in text, name
    (out / name).write_text(text, encoding="utf-8")
    print(out / name)
