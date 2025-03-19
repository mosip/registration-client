----- 

UPDATE "REG"."GLOBAL_PARAM" set VAL='AES/GCM/PKCS5Padding' WHERE CODE='mosip.kernel.crypto.symmetric-algorithm-name';

ALTER TABLE IF EXISTS "REG"."CA_CERT_STORE" DROP COLUMN "CA_CERT_TYPE";
