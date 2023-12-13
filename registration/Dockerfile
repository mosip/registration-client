FROM mosipdev/javafx-reg-client-openjdk:latest

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME

LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

ARG client_version

ARG healthcheck_url

ARG client_upgrade_server

ARG cert_server_URL

ARG keystore_secret

ENV client_version_env=${client_version}

ENV healthcheck_url_env=${healthcheck_url}

ENV client_upgrade_server_env=${client_upgrade_server}

ENV client_repo_env=${client_repo_url}

ENV artifactory_url_env=${artifactory_url}

ENV host_name_env=${host_name}

ENV keystore_secret_env=${keystore_secret}

ENV signer_timestamp_url_env=${signer_timestamp_url}

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user=mosip

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_group=mosip

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_uid=1001

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_gid=1001

# install packages and create user
RUN apt-get -y update \
&& apt-get install -y unzip zip nginx \
&& groupadd -g ${container_user_gid} ${container_user_group} \
&& useradd -u ${container_user_uid} -g ${container_user_group} -s /bin/sh -m ${container_user}

# set working directory for the user
WORKDIR /home/${container_user}

ENV work_dir=/home/${container_user}

ADD registration-client/target ./registration-client/target

ADD registration-test/target ./registration-test/target

ADD registration-test/src/main/resources ./registration-test/resources

ADD configure.sh configure.sh

RUN chmod a+x configure.sh \
 && rm -f ./registration-client/target/registration-client-*-javadoc.jar \
 && rm -f ./registration-client/target/registration-client-*-sources.jar

# change permissions of file inside working dir
RUN chown -R ${container_user}:${container_user} /home/${container_user}
RUN chown -R ${container_user}:${container_user} /var/www

# select container user for all tasks
#USER ${container_user_uid}:${container_user_gid}

ENTRYPOINT ["./configure.sh" ]
