-- Upgrade script for version 1.3.0
ALTER TABLE "REG"."CA_CERT_STORE" ADD COLUMN "CA_CERT_TYPE" VARCHAR(25);
UPDATE "REG"."GLOBAL_PARAM" set VAL='AES/GCM/NoPadding' WHERE CODE='mosip.kernel.crypto.symmetric-algorithm-name';