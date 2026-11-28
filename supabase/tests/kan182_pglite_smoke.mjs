// Run with PGLITE_ROOT pointing to an extracted @electric-sql/pglite package.
// In-memory PostgreSQL with minimal auth fixtures; not a replacement for Supabase tests.
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import { pathToFileURL } from 'node:url'

if (!process.env.PGLITE_ROOT) throw new Error('Set PGLITE_ROOT to the extracted package directory')
const { PGlite } = await import(pathToFileURL(resolve(process.env.PGLITE_ROOT, 'dist/index.js')).href)
const db = new PGlite()
const alice = '11111111-1111-4111-8111-111111111111'
const bob = '22222222-2222-4222-8222-222222222222'
const admin = '33333333-3333-4333-8333-333333333333'
const incident = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'

await db.exec(`
  create role anon;
  create role authenticated;
  create schema auth;
  create schema storage;
  create table storage.objects (bucket_id text not null, name text not null);
  create table auth.users (id uuid primary key);
  create table public.profiles (id uuid primary key references auth.users(id));
  create function auth.uid() returns uuid language sql stable
    as $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
  create function auth.jwt() returns jsonb language sql stable as $$ select '{}'::jsonb $$;
  create function public.is_admin() returns boolean language sql stable
    as $$ select auth.uid() = '${admin}'::uuid $$;
  grant usage on schema auth to authenticated;
  grant execute on function auth.uid() to authenticated;
  insert into auth.users values ('${alice}'), ('${bob}'), ('${admin}');
  insert into public.profiles select id from auth.users;
  insert into storage.objects values ('media_uploads', '${bob}/77777777-7777-4777-8777-777777777777.webp');
`)
await db.exec(await readFile('supabase/migrations/20261128035312_kan182_incident_reports.sql', 'utf8'))
await db.exec(await readFile('supabase/tests/kan182_incident_schema_contract.sql', 'utf8'))
await db.exec(`
  insert into public.incident_reports
    (id, code, reporter_id, client_request_id, campus, room_code, category, severity, title, description)
  values ('${incident}', 'INC-TEST-1', '${alice}', '44444444-4444-4444-8444-444444444444',
    'CS1', 'H001', 'DOOR_LOCK', 'HIGH', 'Broken door', 'Door does not close');
  insert into public.incident_status_logs (incident_id, to_status, actor_id)
    values ('${incident}', 'SUBMITTED', '${alice}');
  set role authenticated;
`)
const selectAs = async (user, table) => {
  await db.query("select set_config('request.jwt.claim.sub', $1, false)", [user])
  return (await db.query(`select * from public.${table}`)).rows
}
assert.equal((await selectAs(alice, 'incident_reports')).length, 1)
assert.equal((await selectAs(bob, 'incident_reports')).length, 0)
assert.equal((await selectAs(admin, 'incident_reports')).length, 1)
assert.equal((await selectAs(alice, 'incident_status_logs')).length, 1)
assert.equal((await selectAs(bob, 'incident_status_logs')).length, 0)
await assert.rejects(db.query(`update public.incident_reports set status = 'RESOLVED'`), /permission denied/i)
await assert.rejects(db.query('insert into public.incident_confirmations (incident_id, user_id) values ($1, $2)', [incident, bob]), /permission denied/i)
const requestId = '55555555-5555-4555-8555-555555555555'
const create = `select public.create_incident_report($1, 'CS1', ' h002 ', null,
  'DOOR_LOCK', 'MEDIUM', ' Broken door ', ' Door cannot close ', $2, false) as id`
const first = (await db.query(create, [requestId, []])).rows[0].id
const retry = (await db.query(create, [requestId, []])).rows[0].id
assert.equal(first, retry)
assert.equal((await selectAs(bob, 'incident_reports')).length, 1)
assert.equal((await db.query('select room_code from public.incident_reports where id = $1', [first])).rows[0].room_code, 'H002')
await assert.rejects(db.query(create, ['66666666-6666-4666-8666-666666666666', ['https://example.com/evil.webp']]), /INCIDENT_INVALID_IMAGE/)
await assert.rejects(db.query(create, ['66666666-6666-4666-8666-666666666666', [`${alice}/77777777-7777-4777-8777-777777777777.webp`]]), /INCIDENT_INVALID_IMAGE/)
await db.query(create, ['77777777-7777-4777-8777-777777777777', [`${bob}/77777777-7777-4777-8777-777777777777.webp`]])
for (const request of [
  '88888888-8888-4888-8888-888888888881',
  '88888888-8888-4888-8888-888888888882',
  '88888888-8888-4888-8888-888888888883',
]) await db.query(create, [request, []])
await assert.rejects(db.query(create, ['99999999-9999-4999-8999-999999999999', []]), /INCIDENT_RATE_LIMIT/)
assert.equal((await db.query(create, [requestId, []])).rows[0].id, first)
console.log('PASS: RLS, write denial, idempotency, image ownership and rate limit (PGlite fixtures)')
await db.close()
