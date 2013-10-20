#!/bin/sh

psql -U store_user -h /tmp store_db -f sample01.sql
