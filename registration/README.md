## Overview

This repository contains the two main components of registration:
1. Registration Client (UI Component)
2. Registration Services


To know more about setting up Registration Client, see [Registration Client User Guide](https://docs.mosip.io/1.2.0/modules/registration-client/registration-client-user-guide).

### Configuration
Configurations used for Registration Client repo is available under [mosip-config](https://github.com/mosip/mosip-config/blob/master/registration-default.properties)

### Build
Below command should be run in the parent project **registration**
`mvn clean install -Dgpg.skip -DskipTests`
