const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { renderToStaticMarkup } = require('react-dom/server');

const post = {
  id: 'test-post', title: 'Phòng kiểm thử', status: 'PENDING',
  type: 'LOOKING_FOR_ROOMMATE', campus: 'CS1_QUANG_TRUNG',
  monthly_rent_vnd: 2500000, deposit_vnd: 1000000, area_m2: 25,
  description: 'Mô tả đầy đủ', address_label: 'Địa chỉ kiểm thử',
  host_name: 'Chủ phòng', host_student_id: '123456', contact_phone: '0912345678',
  images: [], amenities: ['AIR_CONDITIONER'], available_slots: 2,
  gender_preference: 'ANY', utilities_included: true, schedule_note: 'Về trước 23 giờ',
  contact_zalo_enabled: false, contact_zalo_url: 'javascript:alert(1)',
  created_at: '2026-11-14T00:00:00Z', profiles: { display_name: 'Sinh viên' },
};

async function renderPage(file, rows = [post]) {
  const effects = [];
  const filters = [];
  let stateIndex = 0;
  const query = {
    select() { return this; }, order() { return this; },
    eq(...args) { filters.push(args); return this; },
    then(resolve) { return Promise.resolve({ data: [post], error: null }).then(resolve); },
  };
  const mockReact = { ...React,
    useState(initial) {
      const index = stateIndex++;
      return [index === 0 ? rows : index === 1 ? false : initial, () => {}];
    },
    useEffect(fn) { effects.push(fn); },
  };
  const code = ts.transpileModule(fs.readFileSync(path.join(__dirname, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.React, esModuleInterop: true },
  }).outputText;
  const module = { exports: {} };
  function load(name) {
    if (name === 'react') return mockReact;
    if (name === 'lucide-react') return new Proxy({}, { get: () => () => null });
    if (name === '@/utils/supabase-client') return { supabaseBrowserClient: { from: () => query } };
    if (name.includes('/layout/')) return new Proxy({}, { get: () => ({ children }) => children });
    if (name === 'next/link') return ({ children, ...props }) => React.createElement('a', props, children);
    return require(name);
  }
  new Function('require', 'module', 'exports', code)(load, module, module.exports);
  const html = renderToStaticMarkup(React.createElement(module.exports.default));
  effects.forEach(fn => fn());
  await new Promise(resolve => setImmediate(resolve));
  return { html, filters };
}

for (const file of ['src/app/housing/page.tsx', 'src/app/admin/moderation/page.tsx']) {
  test(`${file}: giá và thông tin chi tiết theo schema Android`, async () => {
    const { html } = await renderPage(file);
    assert.ok(html.includes('2.500.000'));
    assert.ok(!html.includes('NaN'));
    for (const text of ['Xem chi tiết', 'Mô tả đầy đủ', 'Địa chỉ kiểm thử', '1.000.000', 'Về trước 23 giờ', 'Điều hòa']) {
      assert.ok(html.includes(text), `Thiếu ${text}`);
    }
    assert.ok(!html.includes('javascript:'));
  });
}

test('Cổng sinh viên chỉ truy vấn tin OPEN và có sao chép số', async () => {
  const { html, filters } = await renderPage('src/app/housing/page.tsx');
  assert.deepEqual(filters, [['status', 'OPEN']]);
  assert.ok(html.includes('Sao chép số'));
  assert.ok(html.includes('0912345678'));
});

for (const file of ['src/app/housing/page.tsx', 'src/app/admin/moderation/page.tsx']) {
  test(`${file}: danh sách trống và Zalo không an toàn`, async () => {
    const empty = await renderPage(file, []);
    assert.ok(empty.html.includes('Chưa có tin') || empty.html.includes('Không có tin'));
    const unsafe = await renderPage(file, [{ ...post, contact_zalo_enabled: true }]);
    assert.ok(!unsafe.html.includes('javascript:'));
    const valid = await renderPage(file, [{ ...post, contact_zalo_enabled: true, contact_zalo_url: 'https://zalo.me/0912345678' }]);
    assert.ok(valid.html.includes('href="https://zalo.me/0912345678"'));
  });
}
