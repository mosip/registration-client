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

# ----------------------------------------------------------------------------------------------
# SECURITY : snapshot a TRUSTED classpath for the build-time signing tools NOW, while
# target/lib still contains ONLY the maven-built project jar + its resolved dependencies. The blocks
# below copy externally-downloaded / operator-supplied jars (third-party SDK, custom impls,
# registration-api-impl, icu4j, clamav) into target/lib. Running ManifestCreator / ManifestSigner off
# the full "lib/*" wildcard would let one of those untrusted jars shadow a tool class (or one of its
# dependency classes) and execute during signing with access to the keystore secret. ManifestCreator
# reads target/lib only as DATA (to hash it), so the tools never need those jars on their classpath.
trusted_tools_cp="$(ls "${work_dir}"/registration-client/target/lib/*.jar | tr '\n' ':')"
trusted_tools_cp="${trusted_tools_cp%:}"

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
wget "${artifactory_url}/artifactory/libs-release-local/zulu21.34.19-ca-fx-jre21.0.3-win_x64.zip" -O "${work_dir}"/zulu21.34.19-ca-fx-jre21.0.3-win_x64.zip
/usr/bin/unzip "${work_dir}"/zulu21.34.19-ca-fx-jre21.0.3-win_x64.zip
mkdir -p "${work_dir}"/registration-client/target/jre
mv "${work_dir}"/zulu21.34.19-ca-fx-jre21.0.3-win_x64/* "${work_dir}"/registration-client/target/jre/
chmod -R a+x "${work_dir}"/registration-client/target/jre

#unzip registration-api-impl jars
wget "${artifactory_url}/artifactory/libs-release-local/registration-client/registration-api-impl.zip" -O "${work_dir}"/registration-api-impl.zip
mkdir -p "${work_dir}"/registration-client/target/registration-api-impl
/usr/bin/unzip "${work_dir}"/registration-api-impl.zip -d "${work_dir}"/registration-client/target/registration-api-impl
mv "${work_dir}"/registration-client/target/registration-api-impl/* "${work_dir}"/registration-client/target/lib/

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
echo "xcopy /f/k/y/v/q .TEMP lib && rmdir /s /q .TEMP && start jre\bin\javaw -Xmx2048m -Xms2048m -Dfile.encoding=UTF-8 -cp lib/*;/* --add-modules=javafx.controls,javafx.fxml --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED io.mosip.registration.controller.Initialization > startup.log 2>&1" >> "${work_dir}"/registration-client/target/run.bat
echo ") else (" >> "${work_dir}"/registration-client/target/run.bat
echo "echo Starting Registration Client" >> "${work_dir}"/registration-client/target/run.bat
echo "start jre\bin\javaw -Xmx2048m -Xms2048m -Dfile.encoding=UTF-8 -cp lib/*;/* --add-modules=javafx.controls,javafx.fxml --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED io.mosip.registration.controller.Initialization > startup.log 2>&1" >> "${work_dir}"/registration-client/target/run.bat
echo ")" >> "${work_dir}"/registration-client/target/run.bat

cp "${work_dir}"/registration-client/target/run.bat "${work_dir}"/registration-client/target/lib/1201to121_run.bat

## jar signing
jarsigner -keystore "${work_dir}"/build_files/keystore.p12 -storepass ${keystore_secret} -tsa ${signer_timestamp_url_env} -digestalg SHA-256 "${work_dir}"/registration-client/target/lib/registration-client-${client_version_env}.jar CodeSigning
jarsigner -keystore "${work_dir}"/build_files/keystore.p12 -storepass ${keystore_secret} -tsa ${signer_timestamp_url_env} -digestalg SHA-256 "${work_dir}"/registration-client/target/lib/registration-services-${client_version_env}.jar CodeSigning

# ----------------------------------------------------------------------------------------------
# 1.3.0 dual manifest + detached SHA256withRSA signatures (issue #812).
# Verified at runtime by the launcher's SignatureVerifier using the public key in provider.pem
# (the cert of the same CodeSigning keypair in keystore.p12).
# ----------------------------------------------------------------------------------------------
keystore="${work_dir}/build_files/keystore.p12"
signing_alias="CodeSigning"
target_dir="${work_dir}/registration-client/target"
# Trusted, external-jar-free classpath snapshotted above (see SECURITY note before the downloads).
# Do NOT use "${target_dir}/lib/*" here: by this point lib/ also holds downloaded / operator-supplied
# jars that must never be on the signing-tool classpath.
java_cp="${trusted_tools_cp}"

# lib/MANIFEST.MF : per-file integrity hashes of everything under lib/ (bundled inside lib.zip)
# ManifestSigner reads the keystore password from the keystore_secret_env env var (not argv) so the
# secret never appears in process listings.
java -cp "${java_cp}" io.mosip.registration.update.ManifestCreator "${client_version_env}" "${target_dir}/lib" "${target_dir}/lib"
java -cp "${java_cp}" io.mosip.registration.update.ManifestSigner "${keystore}" "${signing_alias}" "${target_dir}/lib/MANIFEST.MF" "${target_dir}/lib/MANIFEST.MF.sig"

# jre21.zip : the JRE artifact referenced by the root manifest / downloaded by the launcher
cd "${target_dir}"
/usr/bin/zip -r jre21.zip jre

# Root ./MANIFEST.MF : orchestration artifacts only (NO lib.zip entry). Tolerant of sibling 1.3.0
# artifacts (_launcher.jar / migration.exe / rollback.exe) that may not exist yet -- those entries
# are skipped with a warning and fill in automatically as T2/T3/T4 land.
java -cp "${java_cp}" io.mosip.registration.update.ManifestCreator --list "${client_version_env}" "${target_dir}" \
  "${target_dir}/jre21.zip" \
  "${target_dir}/_launcher.jar" \
  "${target_dir}/migration.exe" \
  "${target_dir}/rollback.exe" \
  "${target_dir}/run.bat"
java -cp "${java_cp}" io.mosip.registration.update.ManifestSigner "${keystore}" "${signing_alias}" "${target_dir}/MANIFEST.MF" "${target_dir}/MANIFEST.MF.sig"

# lib.zip : all of lib/** (including the signed lib/MANIFEST.MF) hosted from the upgrade server
/usr/bin/zip -r lib.zip lib

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
cp "${work_dir}"/registration-client/target/MANIFEST.MF.sig /var/www/html/registration-client/${client_version_env}/
cp "${work_dir}"/registration-client/target/lib.zip /var/www/html/registration-client/${client_version_env}/
cp "${work_dir}"/registration-client/target/jre21.zip /var/www/html/registration-client/${client_version_env}/
cp "${work_dir}"/build_files/maven-metadata.xml /var/www/html/registration-client/
cp "${work_dir}"/registration-client/target/reg-client.zip /var/www/html/registration-client/${client_version_env}/
cp "${work_dir}"/registration-test-utility.zip /var/www/html/registration-client/${client_version_env}/
cp "${work_dir}"/registration-client/target/run.bat /var/www/html/registration-client/${client_version_env}/

echo "setting up nginx static content - completed"

/usr/sbin/nginx -g "daemon off;"
