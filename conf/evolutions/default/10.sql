# --- 

# --- !Ups

alter table transaction_status add column mail_sent boolean default FALSE;
alter table transaction_status alter column mail_sent set not null;

# --- !Downs

alter table transaction_status drop column mail_sent;
