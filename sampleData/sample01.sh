#!/bin/sh

psql -U store_user -h postgres store_db -f sample01.sql
