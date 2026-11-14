create extension if not exists pg_cron;
create extension if not exists pg_net;
create extension if not exists supabase_vault;

create or replace function public.dispatch_notification_pending_job()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  function_url text;
  webhook_secret text;
begin
  select decrypted_secret into function_url
  from vault.decrypted_secrets
  where name = 'SUPABASE_FUNCTIONS_URL';

  select decrypted_secret into webhook_secret
  from vault.decrypted_secrets
  where name = 'WEBHOOK_SHARED_SECRET';

  if function_url is null or webhook_secret is null then
    raise warning 'dispatch-notification scheduler is missing Vault secrets';
    return;
  end if;

  if function_url !~* '^https://' then
    raise exception 'dispatch-notification scheduler URL must use HTTPS';
  end if;

  perform net.http_post(
    url := rtrim(function_url, '/') || '/dispatch-notification',
    headers := jsonb_build_object(
      'content-type', 'application/json',
      'x-webhook-secret', webhook_secret
    ),
    body := '{"type":"DISPATCH_PENDING"}'::jsonb
  );
end;
$$;

revoke all on function public.dispatch_notification_pending_job() from public, anon, authenticated;
grant execute on function public.dispatch_notification_pending_job() to service_role;

select cron.unschedule(jobid)
from cron.job
where jobname = 'dispatch-notification-pending';

select cron.schedule(
  'dispatch-notification-pending',
  '*/6 * * * *',
  $$select public.dispatch_notification_pending_job();$$
);
