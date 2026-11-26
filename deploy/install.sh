#!/bin/bash
# Installs regclient Downloader
## Usage: ./install.sh [kubeconfig]

if [ $# -ge 1 ] ; then
  export KUBECONFIG=$1
fi

NS=regclient
CHART_VERSION=0.0.1-develop

echo Create $NS namespace
kubectl create ns $NS

# Value hard-coded by older charts; it must never be carried over on redeploy (it forces every update).
LEGACY_LAST_UPDATED=20200729125958

# Reads the regclient.version configured in values.yaml
function get_values_version() {
  awk '/^regclient:/ {r=1; next} r && /^[^[:space:]#]/ {r=0} r && /^[[:space:]]+version:/ {gsub(/["\r]/, "", $2); print $2; exit}' values.yaml
}

# Validates a yyyyMMddHHmmss timestamp (14 digits and a real calendar date/time)
function is_valid_last_updated() {
  [[ "$1" =~ ^[0-9]{14}$ ]] && date -d "${1:0:4}-${1:4:2}-${1:6:2} ${1:8:2}:${1:10:2}:${1:12:2}" >/dev/null 2>&1
}

function installing_regclient() {
  echo Istio label
  kubectl label ns $NS istio-injection=enabled --overwrite
  helm repo update

  read -p "Is values.yaml for data-archive chart set correctly as part of Pre-requisites?(Y/n) " yn
  if [[ "$yn" != "Y" ]]; then
    echo "ERROR: values.yaml not set correctly; EXITING."
    exit 1
  fi

  echo Copy configmaps
  sed -i 's/\r$//' copy_cm.sh
  ./copy_cm.sh

  REGCLIENT_HOST=$(kubectl get cm global -o jsonpath={.data.mosip-regclient-host})
  INTERNAL_HOST=$(kubectl get cm global -o jsonpath={.data.mosip-api-internal-host})
  HEALTH_URL=https://$INTERNAL_HOST/v1/syncdata/actuator/health

  # lastUpdated is published in maven-metadata.xml and is the start date from which the client
  # counts mosip.registration.softwareUpdateCheck_configured_frequency days before forcing the update.
  NEW_VERSION=$(get_values_version)
  if [[ -z "$NEW_VERSION" ]]; then
    echo "ERROR: regclient.version not found in values.yaml; EXITING."
    exit 1
  fi
  CURRENT_METADATA=$(kubectl -n $NS get cm regclient -o jsonpath='{.data.maven-metadata\.xml}' 2>/dev/null || true)
  CURRENT_VERSION=$(echo "$CURRENT_METADATA" | sed -n 's:.*<version>\(.*\)</version>.*:\1:p' | head -1)
  CURRENT_LAST_UPDATED=$(echo "$CURRENT_METADATA" | sed -n 's:.*<lastUpdated>\([0-9]*\)</lastUpdated>.*:\1:p' | head -1)

  if [[ "$CURRENT_VERSION" == "$NEW_VERSION" && "$CURRENT_LAST_UPDATED" != "$LEGACY_LAST_UPDATED" ]] && is_valid_last_updated "$CURRENT_LAST_UPDATED"; then
    LAST_UPDATED=$CURRENT_LAST_UPDATED
    echo "Version $NEW_VERSION is already deployed; keeping existing lastUpdated=$LAST_UPDATED"
  else
    DEFAULT_LAST_UPDATED=$(date +%Y%m%d%H%M%S)
    while true; do
      read -r -p "Enter lastUpdated (yyyyMMddHHmmss) for version $NEW_VERSION, i.e. the date it is made available to clients [$DEFAULT_LAST_UPDATED]: " LAST_UPDATED
      LAST_UPDATED=${LAST_UPDATED:-$DEFAULT_LAST_UPDATED}
      if is_valid_last_updated "$LAST_UPDATED"; then
        break
      fi
      echo "Invalid value. Expected a valid date in yyyyMMddHHmmss format (14 digits)."
    done
  fi

  echo Installing reg client downloader. This may take a few minutes ..
  helm -n $NS upgrade --install regclient mosip/regclient -f values.yaml \
    --set regclient.upgradeServerUrl=https://$REGCLIENT_HOST \
    --set regclient.healthCheckUrl=$HEALTH_URL \
    --set regclient.hostName=$INTERNAL_HOST \
    --set-string regclient.lastUpdated="$LAST_UPDATED" \
    --set istio.host=$REGCLIENT_HOST \
    --wait \
    --version $CHART_VERSION

  echo Get your download url from here
  echo https://$REGCLIENT_HOST/
  return 0
}

# set commands for error handling.
set -e
set -o errexit   ## set -e : exit the script if any statement returns a non-true return value
set -o nounset   ## set -u : exit the script if you try to use an uninitialised variable
set -o errtrace  # trace ERR through 'time command' and other functions
set -o pipefail  # trace ERR through pipes
installing_regclient   # calling function
