----- insert / update statements

UPDATE "REG"."GLOBAL_PARAM" set VAL='AES/GCM/NoPadding' WHERE CODE='mosip.kernel.crypto.symmetric-algorithm-name';

MERGE INTO "REG"."GLOBAL_PARAM" gp1 USING "SYSIBM"."SYSDUMMY1" ON gp1.code = 'mosip.kernel.partner.cacertificate.upload.minimumvalidity.month' WHEN NOT MATCHED THEN INSERT VALUES ('mosip.kernel.partner.cacertificate.upload.minimumvalidity.month', 'mosip.kernel.partner.cacertificate.upload.minimumvalidity.month', '1', 'CONFIGURATION', 'eng', true, 'SYSTEM', current timestamp, 'SYSTEM', current timestamp, false, current timestamp);
