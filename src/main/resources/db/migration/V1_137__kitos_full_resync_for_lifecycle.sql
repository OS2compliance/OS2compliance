-- 2.8.0 deactivated systems whose Kitos lifecycle was undecided or missing; a full resync recomputes active for all of them
UPDATE settings SET setting_value = '1970-01-01T00:00Z' WHERE setting_key = 'kitos_itsystem_usage_offset';
