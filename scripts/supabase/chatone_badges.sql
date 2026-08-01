-- Chatone badges backend (project: zupjweowhttidaxqgxjq)
-- Apply in Supabase Studio → SQL Editor.
--
-- Public READ for everyone (any Chatone/3rd-party client with the publishable key),
-- writes only via service role (dashboard / edge functions).

create table if not exists public.chatone_badges (
    id text primary key,
    tooltip text not null,
    image_url text not null,
    sort integer not null default 0
);

create table if not exists public.chatone_badge_users (
    user_id text not null,
    badge_id text not null references public.chatone_badges (id) on delete cascade,
    granted_at timestamptz not null default now(),
    primary key (user_id, badge_id),
    constraint user_id_is_twitch_id check (user_id ~ '^[0-9]{1,32}$')
);

create index if not exists chatone_badge_users_badge_idx
    on public.chatone_badge_users (badge_id);

alter table public.chatone_badges enable row level security;
alter table public.chatone_badge_users enable row level security;

drop policy if exists "public read badges" on public.chatone_badges;
create policy "public read badges"
    on public.chatone_badges for select
    to anon, authenticated
    using (true);

drop policy if exists "public read badge users" on public.chatone_badge_users;
create policy "public read badge users"
    on public.chatone_badge_users for select
    to anon, authenticated
    using (true);

-- The three launch badges. Upload 18x18/36x36 PNGs to a PUBLIC storage bucket
-- named "badges" first, then keep these URLs (or replace with your own CDN).
insert into public.chatone_badges (id, tooltip, image_url, sort) values
    ('developer',  'Chatone Developer',  'https://zupjweowhttidaxqgxjq.supabase.co/storage/v1/object/public/badges/developer.png',  0),
    ('ambassador', 'Chatone Ambassador', 'https://zupjweowhttidaxqgxjq.supabase.co/storage/v1/object/public/badges/ambassador.png', 1),
    ('supporter',  'Chatone Supporter',  'https://zupjweowhttidaxqgxjq.supabase.co/storage/v1/object/public/badges/supporter.png',  2)
on conflict (id) do update set tooltip = excluded.tooltip, image_url = excluded.image_url, sort = excluded.sort;

-- Grant yourself the developer badge (replace with your Twitch user id):
-- insert into public.chatone_badge_users (user_id, badge_id) values ('YOUR_TWITCH_ID', 'developer');
