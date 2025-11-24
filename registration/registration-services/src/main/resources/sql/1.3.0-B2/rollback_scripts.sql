\c mosip_reg
-- Rollback script for version 1.3.0-B2

ALTER TABLE "REG"."CA_CERT_STORE" DROP COLUMN "CA_CERT_TYPE";
