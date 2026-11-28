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
  create table public.notifications (
    id uuid primary key default gen_random_uuid(), recipient_id uuid,
    title text not null, content text not null, type text not null
  );
  create function auth.uid() returns uuid language sql stable
    as $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
  create function auth.jwt() returns jsonb language sql stable
    as $$ select coalesce(nullif(current_setting('request.jwt.claims', true), '')::jsonb, '{}'::jsonb) $$;
  create function public.is_admin() returns boolean language sql stable
    as $$ select auth.uid() = '${admin}'::uuid $$;
  grant usage on schema auth to authenticated;
  grant execute on function auth.uid() to authenticated;
  insert into auth.users values ('${alice}'), ('${bob}'), ('${admin}');
  insert into public.profiles select id from auth.users;
  insert into storage.objects values ('media_uploads', '${bob}/77777777-7777-4777-8777-777777777777.webp');
`)
await db.exec(await readFile('supabase/migrations/20261128035312_kan182_incident_reports.sql', 'utf8'))
await db.exec(await readFile('supabase/migrations/20261128040954_kan182_incident_workflow.sql', 'utf8'))
await db.exec(`create trigger validate_personal_notification_type before insert or update of type
  on public.notifications for each row execute function public.validate_personal_notification_type()`)
await db.exec(`
  create function public.test_notification_failure() returns trigger language plpgsql as $$
  begin
    if current_setting('test.fail_notification', true) = '1' then
      raise exception 'TEST_NOTIFICATION_FAILURE';
    end if;
    return new;
  end $$;
  create trigger test_notification_failure before insert on public.notifications
    for each row execute function public.test_notification_failure();
`)
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

const transition = 'select public.transition_incident_report($1, $2, $3, $4, $5) as version'
await db.query("select set_config('request.jwt.claims', $1, false)", ['{"is_anonymous":true}'])
await assert.rejects(db.query(create, ['aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaab', []]), /INCIDENT_UNAUTHENTICATED/)
await assert.rejects(db.query('select * from public.list_open_incidents(null)'), /INCIDENT_UNAUTHENTICATED/)
await db.query("select set_config('request.jwt.claims', $1, false)", ['{}'])
const feed = (await db.query("select * from public.list_open_incidents('CS1')")).rows
assert.ok(feed.some(row => row.id === incident))
assert.ok(feed.every(row => !('reporter_id' in row) && !('description' in row)))
assert.equal(Number((await db.query('select public.confirm_incident_report($1) as count', [incident])).rows[0].count), 2)
assert.equal(Number((await db.query('select public.confirm_incident_report($1) as count', [incident])).rows[0].count), 2)
await selectAs(alice, 'incident_reports')
assert.equal((await db.query('select * from public.incident_confirmations')).rows.length, 0)
await assert.rejects(db.query('select public.confirm_incident_report($1)', [incident]), /INCIDENT_SELF_CONFIRM/)
await selectAs(bob, 'incident_reports')
await assert.rejects(db.query(transition, [incident, 1, 'CANCELLED', null, null]), /INCIDENT_NOT_FOUND/)
const assign = 'select public.assign_incident_report($1, $2, $3, $4) as version'
await assert.rejects(db.query(assign, [first, 1, null, 'Tổ điện']), /INCIDENT_FORBIDDEN/)
await selectAs(admin, 'incident_reports')
assert.equal((await db.query(assign, [first, 1, null, 'Tổ điện'])).rows[0].version, 2)
await assert.rejects(db.query(assign, [first, 1, null, 'Tổ điện']), /INCIDENT_CONFLICT/)
await assert.rejects(db.query(assign, [first, 2, null, ' ']), /INCIDENT_INVALID_ASSIGNMENT/)
await db.query("select set_config('test.fail_notification', '1', false)")
await assert.rejects(db.query(transition, [incident, 1, 'ACKNOWLEDGED', null, null]), /TEST_NOTIFICATION_FAILURE/)
const afterFailure = (await db.query('select status, row_version from public.incident_reports where id = $1', [incident])).rows[0]
assert.equal(afterFailure.status, 'SUBMITTED')
assert.equal(afterFailure.row_version, 1)
await db.query("select set_config('test.fail_notification', '0', false)")
assert.equal((await db.query(transition, [incident, 1, 'ACKNOWLEDGED', null, null])).rows[0].version, 2)
await assert.rejects(db.query(transition, [incident, 1, 'IN_PROGRESS', null, null]), /INCIDENT_CONFLICT/)
await assert.rejects(db.query(transition, [incident, 2, 'RESOLVED', 'fixed', null]), /INCIDENT_INVALID_TRANSITION/)
assert.equal((await db.query(transition, [incident, 2, 'IN_PROGRESS', null, null])).rows[0].version, 3)
await assert.rejects(db.query(transition, [incident, 3, 'RESOLVED', ' ', null]), /INCIDENT_NOTE_REQUIRED/)
assert.equal((await db.query(transition, [incident, 3, 'RESOLVED', 'fixed', null])).rows[0].version, 4)
await selectAs(alice, 'incident_reports')
assert.equal((await db.query(transition, [incident, 4, 'REOPENED', 'still broken', null])).rows[0].version, 5)
await selectAs(admin, 'incident_reports')
assert.equal((await db.query(transition, [incident, 5, 'IN_PROGRESS', null, null])).rows[0].version, 6)
assert.equal((await db.query(transition, [incident, 6, 'RESOLVED', 'fixed again', null])).rows[0].version, 7)
await selectAs(alice, 'incident_reports')
assert.equal((await db.query(transition, [incident, 7, 'CLOSED', null, null])).rows[0].version, 8)
await assert.rejects(db.query(transition, [incident, 8, 'REOPENED', 'retry', null]), /INCIDENT_INVALID_TRANSITION/)
await assert.rejects(db.query('select public.confirm_incident_report($1)', [incident]), /INCIDENT_NOT_FOUND/)
assert.ok(!(await db.query("select * from public.list_open_incidents('CS1')")).rows.some(row => row.id === incident))
await db.exec('reset role')
const notifications = (await db.query('select type, recipient_id from public.notifications order by type')).rows
assert.equal(notifications.length, 6)
assert.equal(notifications.filter(row => row.recipient_id === alice).length, 5)
assert.equal(notifications.filter(row => row.recipient_id === bob).length, 1)
assert.equal(notifications.filter(row => row.type === 'INCIDENT_RESOLVED').length, 2)
for (const type of ['HOUSING_APPROVED', 'HOUSING_REJECTED', 'EVENT_REMINDER', 'SYSTEM']) {
  await db.query('insert into public.notifications (recipient_id, title, content, type) values ($1, $2, $3, $4)', [alice, 'title', 'content', type])
}
console.log('PASS: RLS, creation, public feed, confirmations, workflow, conflicts and notifications (PGlite fixtures)')
await db.close()
