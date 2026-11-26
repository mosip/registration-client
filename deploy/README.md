# Regclient 

## Introduction
The chart here installs a regclient downloader accessed over an http URL. 

## Install
* Make sure global configmap contains the url for regclient host e.g. `regclient.sandbox.xyz.net`.
* The url must point to your internal loadbalancer as regclient will typically not be open to public.
* Install
```sh
./install.sh
```
* `install.sh` asks for `regclient.lastUpdated` (see below). Press Enter to use the default (current date/time) or enter a value in `yyyyMMddHHmmss` format. Invalid values are rejected.
* If the same `regclient.version` is already deployed, the existing `lastUpdated` is kept and you are not asked again. The date is requested only when a new version is deployed.

## regclient.lastUpdated
`regclient.lastUpdated` is published as `<lastUpdated>` in `/registration-client/maven-metadata.xml`, served to clients by the upgrade server. It is the date from which the version is considered available.

Impact on `mosip.registration.softwareUpdateCheck_configured_frequency` (in `registration-default.properties`, in days):
* When the server `<version>` differs from the client version, the client counts the days since `lastUpdated`.
* If fewer days than `softwareUpdateCheck_configured_frequency` have passed, the operator can choose "Update later" and continue registrations.
* Once that many days have passed, the update is forced: "Update later" is ignored and registrations (New, UIN update, Lost UIN, Correction) are blocked until the client is updated.

Choosing the value:
* Use the date from which clients are expected to start updating, normally the deployment date (the default). Operators then get the full `softwareUpdateCheck_configured_frequency` days of grace.
* An older date shortens the grace period. A date older than `softwareUpdateCheck_configured_frequency` days forces the update immediately. A country can use this deliberately for a mandatory update.
* Do not leave it hard-coded or very old, or every client is forced to update at once. Redeploying the same version must not change it, otherwise the grace period restarts. `install.sh` preserves it for you.
* `regclient.version` in `values.yaml` must exactly match the client build version (e.g. `1.3.0` vs `1.3.0-SNAPSHOT`). Any mismatch is treated as an update.
* The chart fails with `regclient.lastUpdated is required (yyyyMMddHHmmss)` if the value is empty, and also if it is not exactly 14 digits. To set it without `install.sh`, use `--set-string regclient.lastUpdated=20260101000000`. In a values file, quote it (`lastUpdated: "20260101000000"`); an unquoted number is rendered in scientific notation and is rejected.
* Deployments done with older charts carry the legacy value `20200729125958`. `install.sh` never keeps that value and asks for a new date.
* Any other tooling that installs this chart (for example Helmsman DSF files) must now pass `regclient.lastUpdated`, otherwise the install fails by design.
* The existing date is read from the deployed `regclient` configmap. If the release is deleted (`delete.sh`), it is lost and asked again on the next install.

## Download
The download URL will be available at `https://your-reglient-host`. Example: `https://reglient.sandbox.xyz.net`.

## Customization
If you want to add extra environment variables to the regclient docker do follow the below mentioned steps.
1. Add the variables in extraEnvVars section of the sample 'values.yaml.sample' file given.
2. Rename 'values.yaml.sample' file to 'values.yaml'.
3. Append 'values.yaml' to the install.sh script as '-f values.yaml' to include the environmental variables to the regclient docker.
 
```
helm -n $NS install regclient mosip/regclient \
  --set regclient.upgradeServerUrl=https://$INTERNAL_HOST \
  --set regclient.healthCheckUrl=$HEALTH_URL \
  --set regclient.hostName=$INTERNAL_HOST \
  --set istio.host=$REGCLIENT_HOST \
  --wait \
  -f values.yaml \
  --version $CHART_VERSION 
  ```
  
## Set up of regclient
Follow the guide [here](https://github.com/mosip/registration-client/blob/develop/README.md)
