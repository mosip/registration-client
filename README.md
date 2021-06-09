# Registration
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mosip_registration&metric=alert_status)](https://sonarcloud.io/dashboard?id=mosip_registration)
[![Join the chat at https://gitter.im/mosip-community/Registration](https://badges.gitter.im/mosip-community/Registration.svg)](https://gitter.im/mosip-community/Registration?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)



This repository contains the two components of registration:
1. Registration Client (UI Component)
2. Registration Services

Refer to README in respective folders for details.

### License
This project is licensed under the terms of [Mozilla Public License 2.0](https://github.com/mosip/mosip-platform/blob/master/LICENSE)

### Setting up Registration client

1. Download TPM utility and run the to get machine keys
   Please find the instructions to checkout, build and run the utility [here] (https://github.com/mosip/mosip-infra/blob/develop/deployment/sandbox-v2/utils/tpm/key_extractor/README.md)

2. Whitelist the machine keys in server DB
    Machine name and keys output from utility should be updated in server.
   Use the below API to create / whitelist your machine
   
   `curl -X POST "https://<HOSTNAME>/v1/masterdata/machines" -H "accept: */*" -H "Content-Type: application/json" -d "{ \"id\": \"string\", \"metadata\": {}, \"request\": { \"id\": \"string\", \"ipAddress\": \"string\", \"isActive\": true, \"langCode\": \"string\", \"macAddress\": \"string\", \"machineSpecId\": \"string\", \"name\": \"string\", \"publicKey\": \"string\", \"regCenterId\": \"string\", \"serialNum\": \"string\", \"signPublicKey\": \"string\", \"validityDateTime\": \"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", \"zoneCode\": \"string\" }, \"requesttime\": \"2018-12-10T06:12:52.994Z\", \"version\": \"string\"}"`

    NOTE : Replace appropriate HOSTNAME

3. Setup MDS (mock)
   
4. Download registration client.
    Registration client package can be downloaded from below URL, if env is setup with mosip standard deployment

    https://<HOSTNAME>/registration-client/<VERSION>/reg-client.zip

5. set "mosip.hostname" environment variable
   Eg ->  mosip.hostname=dev.mosip.net
   
6. Start registration client on whitelisted machine

