CREATE TABLE "REG"."LOC_HIERARCHY_LIST"("HIERARCHY_LEVEL" INTEGER NOT NULL, "HIERARCHY_LEVEL_NAME" VARCHAR(36) NOT NULL, "LANG_CODE" VARCHAR(3) NOT NULL, "IS_ACTIVE" BOOLEAN NOT NULL, "CR_BY" VARCHAR(256) NOT NULL, "CR_DTIMES" TIMESTAMP NOT NULL, "UPD_BY" VARCHAR(256), "UPD_DTIMES" TIMESTAMP);

ALTER TABLE "REG"."LOC_HIERARCHY_LIST" ADD CONSTRAINT "PK_LOCHL_ID" PRIMARY KEY ("HIERARCHY_LEVEL", "HIERARCHY_LEVEL_NAME", "LANG_CODE" );

INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.restricted-numbers','mosip.kernel.vid.restricted-numbers','786,666','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.not-start-with','mosip.kernel.vid.not-start-with','0,1','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.length.repeating-limit','mosip.kernel.vid.length.repeating-limit','2','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.length.repeating-block-limit','mosip.kernel.vid.length.repeating-block-limit','2','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.length.sequence-limit','mosip.kernel.vid.length.sequence-limit','3','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.kernel.vid.length','mosip.kernel.vid.length','16','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);
