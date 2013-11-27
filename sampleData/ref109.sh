#!/bin/sh

psql -U store_user -h /tmp store_db -f ref109.sql
