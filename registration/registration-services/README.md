# Registration Services

## Overview

This document guides the developer to find the traceability between functionality and the respective technical component.  The provided technical classes are available in the package of 'registration-service' module. 


|Functionality:| Login with UserName and Password/ BIO  |  
|:------:|-----|  
|Technical Detail:| Post successful login, session context would be created. That will be used throughout the application at the required places. The user detail and the respective roles are encapsulated inside the context. Without creation of this context object, the packet can't be created. |  
|Main Service class and method:| SessionContext.create(UserDTO userDTO, String loginMethod, boolean isInitialSetUp, boolean isUserNewToMachine, AuthenticationValidatorDTO authenticationValidatorDTO) |  
|Input parameter:| UserDTO – It should contain info of id, name, roles, center-id. loginMethod – possible values are PWD, FINGERPRINT, FACE, IRIS. isInitialSetUp – true/false, isUserNewToMachine – true/false,  AuthenticationValidatorDTO – should contain id, password|  
|Auth:| Not required. |  
|External Connectivity:| Service and DB |  


|Functionality:| Packet Creation - New Registation / Update UIN/ Lost UIN |   
|:------:|-----|  
|Technical Detail:| Based on the business need [New Registation / Update UIN/ Lost UIN] this 'RegistrationDTO' object should be populated with the relevant  data and also pass the 'RegistrationMetaDataDTO.RegistrationCategory' as [New/ Update/ Lost].  |
|Main Service class and methods| PacketHandlerService.handle(RegistrationDTO registrationDTO)|  
|Input Parameter:|  The RegistrationDTO object contains the RID, PRID, registration details of the individual and also contains the officer and supervisor details. This object has the following sub-classes / maps: 
* DemographicDTO - Details of the Demographic and Documents, 
* BiometricDTO - Biometrics (Fingerprints, Irises, Face and Exception Face) of the individual, parent (or guardian), officer and supervisor, 
* RegistrationMetaDataDTO - Meta data related to registration
* OSIDataDTO - Details of the officer and supervisor who had authenticated the registration.  |  
|Auth:| SessionContext is required for creating the packet |  
|External Connectivity| DB, File system |  

     
|Functionality:| Packet Upload |   
|:------:|-----|  
|Main Service class and method:| PacketUploadService.pushPacket(File packet)|  
|Input Parameter:|	File object, which contains the packet to be uploaded.  |  
|Auth:| Authentication token required while doing file upload. Based on the SessionContext object the advice would attach the token and invoke the required service call. |  
|External Connectivity:| Service, DB, File system |  


|Functionality:| Packet Export |  
|:------:|-----|  
|Main Service class and method:| PacketExportService.getSynchedRecords() - to fetch the packet to be exported. updateRegistrationStatus(List<PacketStatusDTO> exportedPackets) - update the status once exported. |  
|Input Parameter:|	List of packet object. |  
|Auth:| No. |  
|External Connectivity:| DB, File system |  


|Functionality:|  MDM Integration – Register Device |   
|:------:|-----|  
|Technical Detail:| This method automatically scans all devices by connecting to the MDM service, which is running in a particular port and stores it in device registry. |
|Main Service class and method:| MosipBioDeviceManager - init()|  
|Input Parameter:|  No parameter needed.  |  
|Auth:| Not required |  
|External Connectivity:| deviceInfo - MDM service REST call |  



|Functionality:|  MDM Integration  - Validate bio-metric against the bio value already captured and stored in Database. |   
|:------:|-----|  
|Main Service class and method:| BioServiceImpl  - validateFingerPrint(String userId) - based on provided user Id the relevant bio information would be fetched from database and same would be validated against the bio data received from MDM service. |  
|Input Parameter:|   mosipBioDeviceManager – scan(String deviceType)|  
|Auth:| Not required |  
|External Connectivity:| DB, Capture - MDM service REST call |  
