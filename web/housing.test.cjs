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

async function renderPage(file, rows = [post], states = {}) {
  const effects = [];
  const filters = [];
  let stateIndex = 0;
  const query = {
    select() { return this; }, order() { return this; }, maybeSingle() { return this; },
    single() { return { then: resolve => Promise.resolve({ data: post, error: null }).then(resolve) }; },
    eq(...args) { filters.push(args); return this; },
    gt(...args) { filters.push(args); return this; },
    range() { return this; },
    then(resolve) { return Promise.resolve({ data: [post], error: null }).then(resolve); },
  };
  const mockReact = { ...React,
    useState(initial) {
      const index = stateIndex++;
      return [Object.hasOwn(states, index) ? states[index] : index === 0 ? rows : index === 1 ? false : initial, () => {}];
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
    if (name === '@/utils/supabase-client') return { supabaseBrowserClient: { from: () => query, rpc: () => Promise.resolve({ data: [], error: null }) } };
    if (name === '@/utils/housingImages') return loadHelper();
    if (name === '@/utils/campusReference') return loadHelper('campusReference.ts');
    if (name.includes('/layout/')) return new Proxy({}, { get: () => ({ children }) => children });
    if (name === 'next/link') return ({ children, ...props }) => React.createElement('a', props, children);
    return require(name);
  }
  new Function('require', 'module', 'exports', code)(load, module, module.exports);
  const html = renderToStaticMarkup(React.createElement(module.exports.default, { params: { id: post.id } }));
  effects.forEach(fn => fn());
  await new Promise(resolve => setImmediate(resolve));
  return { html, filters };
}

for (const file of ['src/app/housing/[id]/page.tsx', 'src/app/admin/moderation/page.tsx']) {
  test(`${file}: giá và thông tin chi tiết theo schema Android`, async () => {
    const { html } = await renderPage(file, [post], file.includes('[id]') ? { 0: post } : { 3: post });
    assert.ok(html.includes('2.500.000'));
    assert.ok(!html.includes('NaN'));
    for (const text of ['Mô tả đầy đủ', 'Địa chỉ kiểm thử', '1.000.000', 'Về trước 23 giờ', 'Điều hòa']) {
      assert.ok(html.includes(text), `Thiếu ${text}`);
    }
    assert.ok(!html.includes('javascript:'));
  });
}

test('Danh sách sinh viên chỉ truy vấn OPEN và dẫn sang trang riêng', async () => {
  const { html, filters } = await renderPage('src/app/housing/page.tsx');
  assert.deepEqual(filters, [['status', 'OPEN']]);
  assert.ok(html.includes('href="/housing/test-post"'));
  assert.ok(!html.includes('<details'));
  assert.ok(!html.includes('Mô tả đầy đủ'));
  assert.ok(html.includes('2.500.000'));
});

test('Admin mở chi tiết trong dialog và có thao tác quản trị', async () => {
  const { html, filters } = await renderPage('src/app/admin/moderation/page.tsx', [post], { 3: post });
  assert.ok(html.includes('<dialog'));
  assert.ok(!html.includes('<details'));
  for (const text of ['Báo cáo', 'Ẩn tin', 'Xóa tin']) assert.ok(html.includes(text), text);
  assert.ok(filters.some(([column, value]) => column === 'status' && value === 'PENDING'));
  assert.ok(filters.some(([column, value]) => column === 'report_count' && value === 0));
});

for (const file of ['src/app/housing/[id]/page.tsx', 'src/app/admin/moderation/page.tsx']) {
  test(`${file}: Zalo không an toàn không được liên kết`, async () => {
    const slot = file.includes('[id]') ? 0 : 3;
    const unsafe = await renderPage(file, [post], { [slot]: { ...post, contact_zalo_enabled: true } });
    assert.ok(!unsafe.html.includes('javascript:'));
    const valid = await renderPage(file, [post], { [slot]: { ...post, contact_zalo_enabled: true, contact_zalo_url: 'https://zalo.me/0912345678' } });
    assert.ok(valid.html.includes('href="https://zalo.me/0912345678"'));
  });
}

test('RPC quản trị luôn kiểm tra admin và không cấp quyền cho anon', () => {
  const sql = fs.readFileSync(path.join(__dirname, '../supabase/migrations/20261114111909_housing_admin_actions.sql'), 'utf8');
  for (const [name, signature] of [['hide_housing_listing', 'uuid'], ['delete_housing_listing', 'uuid'], ['get_housing_reports', 'uuid']]) {
    const body = sql.split(`function public.${name}(`)[1]?.split('$$;')[0];
    assert.ok(body?.includes('auth.uid()') && body.includes('public.is_admin()'), `${name}: admin check`);
    assert.ok(body.includes("set search_path = ''"), `${name}: fixed search path`);
    assert.ok(sql.includes(`revoke all on function public.${name}(${signature}) from public, anon;`));
  }
  assert.ok(!sql.includes('delete from storage.objects'));
});

test('Migration 20261117 kiểm tra updated_at, dọn dẹp key cũ và ràng buộc ownership/path trong trg_housing_url_guards', () => {
  const sql = fs.readFileSync(path.join(__dirname, '../supabase/migrations/20261117000000_housing_approval_revision.sql'), 'utf8');
  assert.ok(sql.includes('p_reviewed_updated_at'));
  assert.ok(sql.includes('HOUSING_LISTING_CHANGED'));
  assert.ok(sql.includes("set_config('housing.bypass_guard', 'on', true)"));
  assert.ok(sql.includes('update public.housing_posts'));
  assert.ok(sql.includes('HOUSING_IMAGE_OWNERSHIP_INVALID'));
  assert.ok(sql.includes('HOUSING_IMAGE_PATH_INVALID'));
  assert.ok(sql.includes("new.author_id::text || '/'"));
});

test('Trang chi tiết sinh viên lọc OPEN cả khi mở trực tiếp bằng ID', async () => {
  const { html, filters } = await renderPage('src/app/housing/[id]/page.tsx', [], { 0: null });
  assert.deepEqual(filters, [['id', 'test-post'], ['status', 'OPEN']]);
  assert.ok(html.includes('Tin không còn hiển thị'));
});

function loadHelper(file = 'housingImages.ts') {
  const code = ts.transpileModule(fs.readFileSync(path.join(__dirname, 'src/utils', file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)(require, module, module.exports);
  return module.exports;
}

test('isReferencePoint matches current and legacy campus references', () => {
  const { isReferencePoint } = loadHelper('campusReference.ts');
  assert.equal(isReferencePoint(10.8049, 106.7168), true);
  assert.equal(isReferencePoint(10.8332, 106.6533), true);
  assert.equal(isReferencePoint(10.805, 106.717), false);
});

const IMG = 'https://bvxilfcctwmgmrwrncvd.supabase.co/storage/v1/object/public/housing-images';

test('housingImagePaths chỉ tách object trong bucket housing-images của Supabase', () => {
  const { housingImagePaths } = loadHelper();
  assert.deepEqual(housingImagePaths([
    `${IMG}/uid-a/11111111-1111-1111-1111-111111111111.jpg`,
    `${IMG}/uid-b/22222222-2222-2222-2222-222222222222.jpg?width=100`,
    'https://evil.com/object/public/housing-images/x.jpg',
    'https://bvxilfcctwmgmrwrncvd.supabase.co/storage/v1/object/public/other-bucket/uid/c.jpg',
  ]), ['uid-a/11111111-1111-1111-1111-111111111111.jpg', 'uid-b/22222222-2222-2222-2222-222222222222.jpg']);
  assert.deepEqual(housingImagePaths([]), []);
  assert.deepEqual(housingImagePaths(null), []);
  assert.deepEqual(housingImagePaths(undefined), []);
});

test('housingImagePaths kiểm tra origin khớp expectedOrigin và từ chối domain khác', () => {
  const { housingImagePaths } = loadHelper();
  const validUrl = `${IMG}/uid-a/1.jpg`;
  const otherOrigin = 'https://other-project.supabase.co/storage/v1/object/public/housing-images/uid-a/1.jpg';
  
  assert.deepEqual(housingImagePaths([validUrl], IMG), ['uid-a/1.jpg']);
  assert.throws(() => housingImagePaths([otherOrigin], IMG), /HOUSING_IMAGE_ORIGIN_MISMATCH/);
});

test('housingImagePaths từ chối path chứa ..', () => {
  const { housingImagePaths } = loadHelper();
  const maliciousUrl = `${IMG}/uid-a/../../secret.jpg`;
  assert.throws(() => housingImagePaths([maliciousUrl]), /HOUSING_IMAGE_PATH_INVALID/);
});

test('deleteListingWithImages từ chối xóa khi URL ảnh lệch supabaseUrl của client', async () => {
  const { deleteListingWithImages } = loadHelper();
  const client = {
    supabaseUrl: 'https://bvxilfcctwmgmrwrncvd.supabase.co',
    storage: { from: () => ({ remove: async () => ({ data: [], error: null }) }) },
    rpc: async () => ({ error: null }),
  };
  const foreignUrl = 'https://foreign.supabase.co/storage/v1/object/public/housing-images/uid/a.jpg';
  await assert.rejects(() => deleteListingWithImages(client, 'p', [foreignUrl]), /HOUSING_IMAGE_ORIGIN_MISMATCH/);
});

test('deleteListingWithImages xóa ảnh TRƯỚC rồi mới xóa bản ghi', async () => {
  const { deleteListingWithImages } = loadHelper();
  const calls = [];
  const client = {
    storage: { from: bucket => ({ remove: async paths => { calls.push(['remove', bucket, paths]); return { data: paths.map(name => ({ name })), error: null }; } }) },
    rpc: async (fn, args) => { calls.push(['rpc', fn, args]); return { error: null }; },
  };
  await deleteListingWithImages(client, 'post-1', [`${IMG}/uid/a.jpg`]);
  assert.deepEqual(calls, [
    ['remove', 'housing-images', ['uid/a.jpg']],
    ['rpc', 'delete_housing_listing', { p_listing_id: 'post-1' }],
  ]);
});

test('deleteListingWithImages không xóa bản ghi nếu xóa ảnh thất bại', async () => {
  const { deleteListingWithImages } = loadHelper();
  const calls = [];
  const client = {
    storage: { from: () => ({ remove: async () => { calls.push('remove'); return { error: new Error('denied') }; } }) },
    rpc: async () => { calls.push('rpc'); return { error: null }; },
  };
  await assert.rejects(() => deleteListingWithImages(client, 'p', [`${IMG}/uid/a.jpg`]));
  assert.deepEqual(calls, ['remove']);
});

test('deleteListingWithImages giữ bản ghi nếu storage không xóa đủ ảnh', async () => {
  const { deleteListingWithImages } = loadHelper();
  const calls = [];
  const client = {
    storage: { from: () => ({ remove: async () => { calls.push('remove'); return { data: [], error: null }; } }) },
    rpc: async () => { calls.push('rpc'); return { error: null }; },
  };
  await assert.rejects(() => deleteListingWithImages(client, 'p', [`${IMG}/uid/a.jpg`]));
  assert.deepEqual(calls, ['remove']);
});

test('deleteListingWithImages thử lại thành công khi ảnh đã được xóa ở lần gọi trước (key đã vắng mặt)', async () => {
  const { deleteListingWithImages } = loadHelper();
  const calls = [];
  const client = {
    storage: {
      from: bucket => ({
        remove: async paths => { calls.push(['remove', bucket, paths]); return { data: [], error: null }; },
        list: async (folder, opts) => { calls.push(['list', folder, opts]); return { data: [], error: null }; },
      }),
    },
    rpc: async (fn, args) => { calls.push(['rpc', fn, args]); return { error: null }; },
  };
  await deleteListingWithImages(client, 'post-1', [`${IMG}/uid/a.jpg`]);
  assert.deepEqual(calls, [
    ['remove', 'housing-images', ['uid/a.jpg']],
    ['list', 'uid', { search: 'a.jpg' }],
    ['rpc', 'delete_housing_listing', { p_listing_id: 'post-1' }],
  ]);
});

test('deleteListingWithImages giữ bản ghi nếu key chưa xóa vẫn còn tồn tại trong storage', async () => {
  const { deleteListingWithImages } = loadHelper();
  const calls = [];
  const client = {
    storage: {
      from: () => ({
        remove: async () => ({ data: [], error: null }),
        list: async () => ({ data: [{ name: 'a.jpg' }], error: null }),
      }),
    },
    rpc: async () => { calls.push('rpc'); return { error: null }; },
  };
  await assert.rejects(() => deleteListingWithImages(client, 'p', [`${IMG}/uid/a.jpg`]), /HOUSING_IMAGE_DELETE_INCOMPLETE/);
  assert.equal(calls.length, 0);
});

test('deleteListingWithImages tin không ảnh chỉ gọi rpc xóa bản ghi', async () => {
  const { deleteListingWithImages } = loadHelper();
  const calls = [];
  const client = {
    storage: { from: () => ({ remove: async () => { calls.push('remove'); return { error: null }; } }) },
    rpc: async fn => { calls.push(fn); return { error: null }; },
  };
  await deleteListingWithImages(client, 'p', []);
  assert.deepEqual(calls, ['delete_housing_listing']);
});

test('Admin xác nhận xóa tin nhắc xóa cả tệp ảnh, bỏ cảnh báo giữ ảnh', async () => {
  const { html } = await renderPage('src/app/admin/moderation/page.tsx', [post], { 3: post, 9: 'delete' });
  assert.ok(html.includes('tệp ảnh'));
  assert.ok(!html.includes('chưa bị xóa'));
});
