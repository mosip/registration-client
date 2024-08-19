#!/bin/bash

set -e

echo "Started with args"

work_dir="$work_dir"

client_version_env="$client_version_env" #We should pick this from the jar not as an argument.
client_upgrade_server="$client_upgrade_server_env" #docker hosted url
reg_client_sdk_url="$reg_client_sdk_url_env"
artifactory_url="$artifactory_url_env"
keystore_secret="$keystore_secret_env"
reg_client_custom_impls_url="$reg_client_custom_impls_url_env"
host_name="$host_name_env"

echo "initialized variables"

echo "environment=PRODUCTION" > "${work_dir}"/mosip-application.properties
echo "mosip.reg.version=${client_version_env}" >> "${work_dir}"/mosip-application.properties
echo "mosip.reg.client.url=${client_upgrade_server}/registration-client/" >> "${work_dir}"/mosip-application.properties
echo "mosip.reg.healthcheck.url=${healthcheck_url_env}" >> "${work_dir}"/mosip-application.properties
echo "mosip.reg.rollback.path=BackUp" >> "${work_dir}"/mosip-application.properties
echo "mosip.reg.xml.file.url=${client_upgrade_server}/registration-client/maven-metadata.xml" >> "${work_dir}"/mosip-application.properties
echo "mosip.client.upgrade.server.url=${client_upgrade_server}" >> "${work_dir}"/mosip-application.properties
echo "mosip.hostname=${host_name}"  >> "${work_dir}"/mosip-application.properties

echo "created mosip-application.properties"
cd "${work_dir}"/registration-client/target/lib
mkdir -p ${work_dir}/registration-client/target/lib/props
cp "${work_dir}"/mosip-application.properties ${work_dir}/registration-client/target/lib/props/mosip-application.properties
jar uf registration-services-${client_version_env}.jar props/mosip-application.properties
rm -rf ${work_dir}/registration-client/target/lib/props

echo "Adding signer certificate"
cp "${work_dir}"/build_files/Client.crt ${work_dir}/registration-client/target/lib/provider.pem
jar uf registration-services-${client_version_env}.jar provider.pem
rm ${work_dir}/registration-client/target/lib/provider.pem

cd "${work_dir}"

if wget "${artifactory_url}/artifactory/libs-release-local/reg-client/resources.zip"
then
  echo "Successfully downloaded reg-client resources, Adding it to reg-client jar"
  mkdir resources
  /usr/bin/unzip ./resources.zip -d ./resources/
  cd ./resources
  jar uvf "${work_dir}"/registration-client/target/registration-client-${client_version_env}.jar .
else
  echo "No separate resources found !!"
fi

if [ "$reg_client_custom_impls_url" ]
then
  wget "$reg_client_custom_impls_url" -O "${work_dir}"/custom-impl.zip
  echo "Successfully downloaded custom-implementations zip, Adding it to reg-client jar"
  mkdir "${work_dir}"/customimpls
  /usr/bin/unzip "${work_dir}"/custom-impl.zip -d "${work_dir}"/customimpls/
  cp "${work_dir}"/customimpls/*.jar "${work_dir}"/registration-client/target/lib/
else
  echo "No Custom(scanner & geo-position) implementations found !!"
fi

cd "${work_dir}"
mkdir -p "${work_dir}"/sdkjars

if [ "$reg_client_sdk_url" ]
then
	echo "Found thirdparty SDK"
	wget "$reg_client_sdk_url" -O sdkDependency.zip
	mkdir sdkDependency
	/usr/bin/unzip "${work_dir}"/sdkDependency.zip -d sdkDependency/
	cp "${work_dir}"/sdkDependency/*.jar "${work_dir}"/registration-client/target/lib/
else
	echo "Downloading MOCK SDK..."
	wget "${artifactory_url}/artifactory/libs-release-local/mock-sdk/1.1.5/mock-sdk.jar" -O "${work_dir}"/registration-client/target/lib/mock-sdk.jar
fi

wget "${artifactory_url}/artifactory/libs-release-local/icu4j/icu4j.jar" -O "${work_dir}"/registration-client/target/lib/icu4j.jar
wget "${artifactory_url}/artifactory/libs-release-local/icu4j/kernel-transliteration-icu4j.jar" -O "${work_dir}"/registration-client/target/lib/kernel-transliteration-icu4j.jar
wget "${artifactory_url}/artifactory/libs-release-local/clamav/clamav.jar" -O "${work_dir}"/registration-client/target/lib/clamav.jar
wget "${artifactory_url}/artifactory/libs-release-local/clamav/kernel-virusscanner-clamav.jar" -O "${work_dir}"/registration-client/target/lib/kernel-virusscanner-clamav.jar

#unzip Jre to be bundled
wget "${artifactory_url}/artifactory/libs-release-local/zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip" -O "${work_dir}"/zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip
/usr/bin/unzip "${work_dir}"/zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip
mkdir -p "${work_dir}"/registration-client/target/jre
mv "${work_dir}"/zulu11.41.23-ca-fx-jre11.0.8-win_x64/* "${work_dir}"/registration-client/target/jre/
chmod -R a+x "${work_dir}"/registration-client/target/jre

cp "${work_dir}"/build_files/logback.xml "${work_dir}"/registration-client/target/lib/logback.xml
cp "${work_dir}"/registration-client/target/registration-client-${client_version_env}.jar "${work_dir}"/registration-client/target/lib/registration-client-${client_version_env}.jar

echo "@echo off" > "${work_dir}"/registration-client/target/run.bat
echo "if exist jre\jre (" >> "${work_dir}"/registration-client/target/run.bat
echo "xcopy /s /k /y /q jre\jre jre && rmdir /s /q jre\jre" >> "${work_dir}"/registration-client/target/run.bat
echo ")" >> "${work_dir}"/registration-client/target/run.bat
echo "if exist .UNKNOWN_JARS (" >> "${work_dir}"/registration-client/target/run.bat
echo "FOR /F \"tokens=* delims=\" %%x in (.UNKNOWN_JARS) DO DEL /Q lib\%%x" >> "${work_dir}"/registration-client/target/run.bat
echo ")" >> "${work_dir}"/registration-client/target/run.bat
echo "if exist .TEMP (" >> "${work_dir}"/registration-client/target/run.bat
echo "echo Starting Registration Client after Upgrade" >> "${work_dir}"/registration-client/target/run.bat
echo "xcopy /f/k/y/v/q .TEMP lib && rmdir /s /q .TEMP && start jre\bin\javaw -Xmx2048m -Xms2048m -Dfile.encoding=UTF-8 -cp lib/*;/* io.mosip.registration.controller.Initialization > startup.log 2>&1" >> "${work_dir}"/registration-client/target/run.bat
echo ") else (" >> "${work_dir}"/registration-client/target/run.bat
echo "echo Starting Registration Client" >> "${work_dir}"/registration-client/target/run.bat
echo "start jre\bin\javaw -Xmx2048m -Xms2048m -Dfile.encoding=UTF-8 -cp lib/*;/* io.mosip.registration.controller.Initialization > startup.log 2>&1" >> "${work_dir}"/registration-client/target/run.bat
echo ")" >> "${work_dir}"/registration-client/target/run.bat

cp "${work_dir}"/registration-client/target/run.bat "${work_dir}"/registration-client/target/lib/114to1201_run.bat

## jar signing
jarsigner -keystore "${work_dir}"/build_files/keystore.p12 -storepass ${keystore_secret} -tsa ${signer_timestamp_url_env} -digestalg SHA-256 "${work_dir}"/registration-client/target/lib/registration-client-${client_version_env}.jar CodeSigning
jarsigner -keystore "${work_dir}"/build_files/keystore.p12 -storepass ${keystore_secret} -tsa ${signer_timestamp_url_env} -digestalg SHA-256 "${work_dir}"/registration-client/target/lib/registration-services-${client_version_env}.jar CodeSigning

/usr/local/openjdk-11/bin/java -cp "${work_dir}"/registration-client/target/registration-client-${client_version_env}.jar:"${work_dir}"/registration-client/target/lib/* io.mosip.registration.update.ManifestCreator "${client_version_env}" "${work_dir}/registration-client/target/lib" "${work_dir}/registration-client/target"

cd "${work_dir}"/registration-client/target/

echo "Started to create the registration client zip"

ls -ltr lib | grep bc

/usr/bin/zip -r reg-client.zip jre
/usr/bin/zip -r reg-client.zip lib
/usr/bin/zip -r reg-client.zip MANIFEST.MF
/usr/bin/zip -r reg-client.zip run.bat

#Creating client testing utility
mkdir -p "${work_dir}"/registration-test-utility
mkdir -p "${work_dir}"/registration-test-utility/testlib
mkdir -p "${work_dir}"/registration-test-utility/lib
cp "${work_dir}"/registration-test/target/registration-test-${client_version_env}.jar "${work_dir}"/registration-test-utility/registration-test.jar
cp -r "${work_dir}"/registration-test/target/lib/* "${work_dir}"/registration-test-utility/testlib
cp -r "${work_dir}"/registration-client/target/lib/*  "${work_dir}"/registration-test-utility/lib
cp -r "${work_dir}"/registration-test/resources/*  "${work_dir}"/registration-test-utility/
cp -r "${work_dir}"/registration-client/target/jre "${work_dir}"/registration-test-utility/
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
cp "${work_dir}"/registration-client/target/run.bat /var/www/html/registration-client/${client_version_env}/
cp "${work_dir}"/registration-client/target/run.bat /var/www/html/registration-client/${client_version_env}/run_upgrade.bat

echo "setting up nginx static content - completed"

/usr/sbin/nginx -g "daemon off;"
