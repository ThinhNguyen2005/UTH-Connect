-- Commit new enum value before using it in a later migration transaction.
alter type public.event_status add value if not exists 'DRAFT' before 'UPCOMING';
