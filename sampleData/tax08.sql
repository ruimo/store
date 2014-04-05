update tax_history set valid_until=timestamp'2014-04-01 00:00:00';

insert into tax_history (tax_history_id, tax_id, tax_type, rate, valid_until)
values ((select nextval('tax_history_seq')), 1, 1, 8.00, timestamp'9999-12-31 00:00:00');
insert into tax_history (tax_history_id, tax_id, tax_type, rate, valid_until)
values ((select nextval('tax_history_seq')), 2, 0, 8.00, timestamp'9999-12-31 00:00:00');
