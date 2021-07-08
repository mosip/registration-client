# Registration
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mosip_registration&metric=alert_status)](https://sonarcloud.io/dashboard?id=mosip_registration)
[![Join the chat at https://gitter.im/mosip-community/Registration](https://badges.gitter.im/mosip-community/Registration.svg)](https://gitter.im/mosip-community/Registration?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)



This repository contains the two components of registration:
1. Registration Client (UI Component)
2. Registration Services
3. Registration libs (run wrapper)

Refer to README in respective folders for details.

### License
This project is licensed under the terms of [Mozilla Public License 2.0](https://github.com/mosip/mosip-platform/blob/master/LICENSE)

### Setting up Registration client

#### Step-1. Download TPM utility and run the to get machine keys
   
   > Please find the instructions to check out, build and run the utility [here](https://github.com/mosip/mosip-infra/blob/develop/deployment/sandbox-v2/utils/tpm/key_extractor/README.md)


#### Step-2. Whitelist the machine keys in server DB
   
   > Machine name and keys output from utility should be updated in server.
   
   > Use the below API to create / whitelist your machine
   
   `curl -X POST "https://<HOSTNAME>/v1/masterdata/machines" -H "accept: */*" -H "Content-Type: application/json" -d "{ \"id\": \"string\", \"metadata\": {}, \"request\": { \"id\": \"string\", \"ipAddress\": \"string\", \"isActive\": true, \"langCode\": \"string\", \"macAddress\": \"string\", \"machineSpecId\": \"string\", \"name\": \"string\", \"publicKey\": \"string\", \"regCenterId\": \"string\", \"serialNum\": \"string\", \"signPublicKey\": \"string\", \"validityDateTime\": \"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", \"zoneCode\": \"string\" }, \"requesttime\": \"2018-12-10T06:12:52.994Z\", \"version\": \"string\"}"`

   
    NOTE : 
    -> Replace appropriate HOSTNAME in the above curl command
    -> In case, you are trying to whitelist NON-TPM machine, Please set publicKey and signPublicKey with same value 
    -> check the machine status - it must be active
    -> Machine whitelisting can be done from admin portal - https://<HOSTNAME>/admin-ui
    -> ipAddress, macAddress,serialNum are optional


#### Step-3. Know your userId and required roles

   > Create the user in the keycloak.
   
   > Map the user to same center as that of the machine that is created/whitelisted in Step-2.
   
   > Either one of these roles must be assigned to the user in keycloak - "REGISTRATION_SUPERVISOR", "REGISTRATION_OFFICER"
  
  
    NOTE:
    -> Assign "Default" role if you need to skip operator(biometrics) onboarding
    -> Same operations can be done through admin portal


#### Step-4. Setup MDS (mock)

   > Please find the instructions to check out, build and run the mock MDS [here](https://github.com/mosip/mosip-mock-services/blob/master/MockMDS/README.md)


#### Step-5. Download registration client and start registration-client

   > Registration client package can be downloaded from below URL, if env is setup with mosip standard deployment.

      `https://<HOSTNAME>/registration-client/<VERSION>/reg-client.zip`

   > set "mosip.hostname" environment variable

   > Start registration client using run.bat


    NOTE:
    -> In case of NON-TPM, machine keys will be created under <home-dir>/.mosipkeys folder on the first run of registration-client. 
    Hence in case of NON-TPM machine Step-2 need to be executed after Step-5 before login
    


## Troubleshooting:

##### 1. Incorrect username/password
   
    -> Cross-check the machine keys mapping in server ('Machine not found' error in logs)
   
    -> Cross-check machine status

    -> Cross-check your credentials directly with auth-manager

    -> 'Invalid Request' error in log - Check your machine time, it shouldnt be less or greater than local timezone datetime (usually accepted lag is +5/-5 minutes)

    -> check logs/registration.log for more details

##### 2. Configuration / masterdata Sync failed
    
    -> check if kernel-syncdata-service is up. Swagger url for the same - https://<HOSTNAME>/v1/syncdata/swagger-ui.html
