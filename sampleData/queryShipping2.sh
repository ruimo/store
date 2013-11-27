#!/bin/sh

psql -h /tmp store_db -f queryShipping2.sql -A -F,
