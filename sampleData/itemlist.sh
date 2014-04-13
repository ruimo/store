#!/bin/sh

psql -A -F, -U store_user -h /tmp store_db -f itemlist.sql
