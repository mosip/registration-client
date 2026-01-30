----- 

UPDATE "REG"."GLOBAL_PARAM" set VAL='AES/GCM/PKCS5Padding' WHERE CODE='mosip.kernel.crypto.symmetric-algorithm-name';

delete from "REG"."GLOBAL_PARAM" where code='mosip.kernel.partner.cacertificate.upload.minimumvalidity.month';
