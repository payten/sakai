-- Magic '-123' value here never gets used: if the merge succeeds it
-- takes NEXTVAL from the sequence.  If it fails (because the function
-- is already defined), then nothing gets inserted.  We just include
-- the placeholder to keep Oracle happy.
-- 

MERGE INTO SAKAI_REALM_FUNCTION srf
USING (
SELECT -123 as function_key, 
'pasystem.manage' as function_name
FROM dual
) t on (srf.function_name = t.function_name) 
WHEN NOT MATCHED THEN 
INSERT (function_key, function_name)
VALUES (SAKAI_REALM_FUNCTION_SEQ.NEXTVAL, t.function_name);
