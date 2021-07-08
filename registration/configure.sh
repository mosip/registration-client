#!/bin/bash

set -e

echo "Started with args"

work_dir="$work_dir"

client_version_env="$client_version_env" #We should pick this from the jar not as an argument.
crypto_key_env="$crypto_key_env" #key to encrypt the jar files
client_certificate="$client_certificate_env" # Not used as of now
client_upgrade_server="$client_upgrade_server_env" #docker hosted url
reg_client_sdk_url="$reg_client_sdk_url_env"
artifactory_url="$artifactory_url_env"

echo "initalized variables"

mkdir -p ${work_dir}/registration-libs/target/props

echo "mosip.reg.app.key=${crypto_key_env}" > "${work_dir}"/registration-libs/target/props/mosip-application.properties
echo "mosip.reg.version=${client_version_env}" >> "${work_dir}"/registration-libs/target/props/mosip-application.properties
echo "mosip.reg.client.url=${client_upgrade_server}/registration-client/" >> "${work_dir}"/registration-libs/target/props/mosip-application.properties
echo "mosip.reg.healthcheck.url=${healthcheck_url_env}" >> "${work_dir}"/registration-libs/target/props/mosip-application.properties
echo "mosip.reg.rollback.path=../BackUp" >> "${work_dir}"/registration-libs/target/props/mosip-application.properties
echo "mosip.reg.cerpath=/cer/mosip_cer.cer" >> "${work_dir}"/registration-libs/target/props/mosip-application.properties
echo "mosip.reg.dbpath=db/reg" >> "${work_dir}"/registration-libs/target/props/mosip-application.properties
echo "mosip.reg.xml.file.url=${client_upgrade_server}/registration-client/maven-metadata.xml" >> "${work_dir}"/registration-libs/target/props/mosip-application.properties
echo "mosip.reg.client.tpm.availability=Y" >> "${work_dir}"/registration-libs/target/props/mosip-application.properties
echo "mosip.client.upgrade.server.url=${client_upgrade_server}" >> "${work_dir}"/registration-libs/target/props/mosip-application.properties

echo "created mosip-application.properties"

cd "${work_dir}"/registration-libs/target
jar uf registration-libs-${client_version_env}.jar props/mosip-application.properties
cd "${work_dir}"

if wget "${artifactory_url}/artifactory/libs-release-local/reg-client/resources.zip"
then
  echo "Successfully downloaded reg-client resources, Adding it to reg-client jar"
  /usr/bin/unzip ./resources.zip
  cd ./resources
  jar uvf "${work_dir}"/registration-client/target/registration-client-${client_version_env}.jar .
else
  echo "No separate resources found !!"
fi

cd "${work_dir}"
mkdir -p "${work_dir}"/sdkjars

if [ "$reg_client_sdk_url" ]
then
	echo "Found thirdparty SDK"
	wget "$reg_client_sdk_url"
	/usr/bin/unzip "${work_dir}"/sdkDependency.zip
	cp "${work_dir}"/sdkDependency/*.jar "${work_dir}"/sdkjars/
else
	echo "Downloading MOCK SDK..."
	wget "${artifactory_url}/artifactory/libs-release-local/mock-sdk/1.1.5/mock-sdk.jar" -O "${work_dir}"/sdkjars/mock-sdk.jar
fi

cp "${work_dir}"/sdkjars/*.jar "${work_dir}"/registration-client/target/lib/
wget "${artifactory_url}/artifactory/libs-release-local/icu4j/icu4j.jar" -O "${work_dir}"/registration-client/target/lib/icu4j.jar
wget "${artifactory_url}/artifactory/libs-release-local/icu4j/kernel-transliteration-icu4j.jar" -O "${work_dir}"/registration-client/target/lib/kernel-transliteration-icu4j.jar
wget "${artifactory_url}/artifactory/libs-release-local/clamav/clamav.jar" -O "${work_dir}"/registration-client/target/lib/clamav.jar
wget "${artifactory_url}/artifactory/libs-release-local/clamav/kernel-virusscanner-clamav.jar" -O "${work_dir}"/registration-client/target/lib/kernel-virusscanner-clamav.jar

#unzip Jre to be bundled
wget "${artifactory_url}/artifactory/libs-release-local/zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip" -O "${work_dir}"/zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip
/usr/bin/unzip "${work_dir}"/zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip
mkdir -p "${work_dir}"/registration-libs/resources/jre
mv "${work_dir}"/zulu11.41.23-ca-fx-jre11.0.8-win_x64/* "${work_dir}"/registration-libs/resources/jre/
chmod -R a+x "${work_dir}"/registration-libs/resources/jre

/usr/local/openjdk-11/bin/java -cp "${work_dir}"/registration-libs/target/*:"${work_dir}"/registration-client/target/lib/* io.mosip.registration.cipher.ClientJarEncryption "${work_dir}/registration-client/target/registration-client-${client_version_env}.jar" "${crypto_key_env}" "${client_version_env}" "${work_dir}/registration-libs/target/" "${work_dir}/build_files/${client_certificate}" "${work_dir}/registration-libs/resources/db/reg" "${work_dir}/registration-client/target/registration-client-${client_version_env}.jar" "${work_dir}/registration-libs/resources/rxtx" "${work_dir}/registration-libs/resources/jre" "${work_dir}/registration-libs/resources/batch/run.bat" "${work_dir}/registration-libs/target/props/mosip-application.properties"

echo "encryption completed"

cd "${work_dir}"/registration-client/target/
mv "mosip-sw-${client_version_env}.zip" reg-client.zip
mkdir -p "${work_dir}"/registration-client/target/bin
cp "${work_dir}"/registration-client/target/lib/mosip-client.jar "${work_dir}"/registration-client/target/bin/
cp "${work_dir}"/registration-client/target/lib/mosip-services.jar "${work_dir}"/registration-client/target/bin/

ls -ltr lib | grep bc

/usr/bin/zip -r reg-client.zip bin
/usr/bin/zip -r reg-client.zip lib

## adding logback.xml
/usr/bin/zip -j reg-client.zip "${work_dir}"/build_files/logback.xml

#Creating Regclient testing framework
mkdir -p "${work_dir}"/registration-test-utility
mkdir -p "${work_dir}"/registration-test-utility/lib
cp "${work_dir}"/registration-test/target/registration-test-*-dependencies.jar "${work_dir}"/registration-test-utility/registration-test.jar
cp -r "${work_dir}"/registration-test/resources/*  "${work_dir}"/registration-test-utility/
cp -r "${work_dir}"/registration-libs/resources/jre "${work_dir}"/registration-test-utility/
#cp -r "${work_dir}"/registration-client/target/lib/morena* "${work_dir}"/registration-test-utility/lib
cp -r "${work_dir}"/registration-client/target/lib/icu4j.jar "${work_dir}"/registration-test-utility/lib
cp -r "${work_dir}"/registration-client/target/lib/kernel-transliteration-icu4j.jar "${work_dir}"/registration-test-utility/lib
cp -r "${work_dir}"/registration-client/target/lib/clamav.jar "${work_dir}"/registration-test-utility/lib
cp -r "${work_dir}"/registration-client/target/lib/kernel-virusscanner-clamav.jar "${work_dir}"/registration-test-utility/lib
cp -r "${work_dir}"/sdkjars/*.jar "${work_dir}"/registration-test-utility/lib
cp "${work_dir}"/registration-client/target/MANIFEST.MF "${work_dir}"/registration-test-utility/

cd "${work_dir}"
/usr/bin/zip -r ./registration-test-utility.zip ./registration-test-utility/*

echo "setting up nginx static content"

mkdir -p /var/www/html/registration-client
mkdir -p /var/www/html/registration-client/${client_version_env}
mkdir -p /var/www/html/registration-client/${client_version_env}/lib
mkdir -p /var/www/html/registration-test/${client_version_env}
 
cp "${work_dir}"/registration-client/target/lib/* /var/www/html/registration-client/${client_version_env}/lib
cp "${work_dir}"/registration-client/target/MANIFEST.MF /var/www/html/registration-client/${client_version_env}/
cp "${work_dir}"/build_files/maven-metadata.xml /var/www/html/registration-client/
cp "${work_dir}"/registration-client/target/reg-client.zip /var/www/html/registration-client/${client_version_env}/
cp "${work_dir}"/registration-test-utility.zip /var/www/html/registration-client/${client_version_env}/

echo "setting up nginx static content - completed"

/usr/sbin/nginx -g "daemon off;"
