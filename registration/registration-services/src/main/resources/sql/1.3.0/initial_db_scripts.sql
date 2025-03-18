----- insert / update statements

UPDATE "REG"."GLOBAL_PARAM" set VAL='AES/GCM/NoPadding' WHERE CODE='mosip.kernel.crypto.symmetric-algorithm-name';

ALTER TABLE "REG"."CA_CERT_STORE" ADD COLUMN "KEY_TYPE" VARCHAR(10);
