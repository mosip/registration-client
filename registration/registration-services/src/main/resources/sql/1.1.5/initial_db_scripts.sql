CREATE TABLE "REG"."CA_CERT_STORE"("CERT_ID" VARCHAR(36) NOT NULL, "CERT_SUBJECT" VARCHAR(500) NOT NULL, "CERT_ISSUER" VARCHAR(500) NOT NULL, "ISSUER_ID" VARCHAR(36) NOT NULL, "CERT_NOT_BEFORE" TIMESTAMP, "CERT_NOT_AFTER" TIMESTAMP, "CRL_URI" VARCHAR(120), "CERT_DATA" VARCHAR(3000), "CERT_THUMBPRINT" VARCHAR(100), "CERT_SERIAL_NO" VARCHAR(50),    "PARTNER_DOMAIN" VARCHAR(36), "CR_BY" VARCHAR(256) NOT NULL, "CR_DTIMES" TIMESTAMP NOT NULL, "UPD_BY" VARCHAR(256), "UPD_DTIMES" TIMESTAMP, "IS_DELETED" BOOLEAN, "DEL_DTIMES" TIMESTAMP);

ALTER TABLE "REG"."CA_CERT_STORE" ADD CONSTRAINT "PK_CACS_ID" PRIMARY KEY ("CERT_ID");

ALTER TABLE "REG"."SYNC_JOB_DEF" ADD COLUMN "JOB_TYPE" VARCHAR(128);


MERGE INTO "REG"."GLOBAL_PARAM" gp1 USING "SYSIBM"."SYSDUMMY1" ON gp1.code = 'mosip.registration.regclient_installed_time' WHEN NOT MATCHED THEN INSERT VALUES ('mosip.registration.regclient_installed_time','mosip.registration.regclient_installed_time',current timestamp,'CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

MERGE INTO "REG"."GLOBAL_PARAM" gp1 USING "SYSIBM"."SYSDUMMY1" ON gp1.code = 'mosip.registration.mdm.trust.domain.rcapture' WHEN NOT MATCHED THEN INSERT VALUES ('mosip.registration.mdm.trust.domain.rcapture','mosip.registration.mdm.key.domain.rcapture','DEVICE','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

MERGE INTO "REG"."GLOBAL_PARAM" gp1 USING "SYSIBM"."SYSDUMMY1" ON gp1.code = 'mosip.registration.mdm.trust.domain.digitalId' WHEN NOT MATCHED THEN INSERT VALUES ('mosip.registration.mdm.trust.domain.digitalId','mosip.registration.mdm.key.domain.digitalId','FTM','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

UPDATE "REG"."GLOBAL_PARAM" SET val='false' WHERE name='mosip.registration.machinecenterchanged';

MERGE INTO "REG"."GLOBAL_PARAM" gp1 USING "SYSIBM"."SYSDUMMY1" ON gp1.code = 'mosip.registration.mdm.trust.domain.deviceinfo' WHEN NOT MATCHED THEN INSERT VALUES ('mosip.registration.mdm.trust.domain.deviceinfo','mosip.registration.mdm.key.domain.deviceinfo','DEVICE','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

INSERT INTO "REG"."KEY_POLICY_DEF" ("APP_ID", "KEY_VALIDITY_DURATION", "IS_ACTIVE", "CR_BY", "CR_DTIMES") VALUES('SERVER-RESPONSE', 1095, true, 'mosipadmin', current timestamp);
