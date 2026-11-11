# Linux image for the Registration Client UI automation.
# Build from the registration/ directory so Maven resolves Linux JavaFX natives:
#   docker build -f registration-test/Dockerfile -t registration-test:local .
#
# Same shape as mosip/admin-ui uitest-admin: a non-root user, a small entrypoint,
# and configuration supplied at run time rather than baked into the command.

FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /src
COPY . /src

RUN mvn -B -DskipTests -Dgpg.skip -Dmaven.gitcommitid.skip=true clean package -pl registration-test -am \
    && mkdir -p /out \
    && find registration-test/target -maxdepth 1 -type f -name 'registration-test-*.jar' \
         ! -name '*-sources.jar' ! -name '*-javadoc.jar' \
         -exec cp {} /out/registration-test.jar \; \
    && test -s /out/registration-test.jar

FROM eclipse-temurin:21-jre-jammy

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME

LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1001
ARG container_user_gid=1001

ENV container_user=${container_user}
ENV work_dir=/home/${container_user}
ENV HEADLESS=true

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bash \
        ca-certificates \
        curl \
        fontconfig \
        fonts-dejavu-core \
        libasound2 \
        libatk-bridge2.0-0 \
        libatk1.0-0 \
        libcups2 \
        libcairo2 \
        libgbm1 \
        libglib2.0-0 \
        libgtk-3-0 \
        libnss3 \
        libpango-1.0-0 \
        libx11-6 \
        libxcomposite1 \
        libxcursor1 \
        libxdamage1 \
        libxext6 \
        libxfixes3 \
        libxi6 \
        libxkbcommon0 \
        libxrandr2 \
        libxrender1 \
        libxss1 \
        libxtst6 \
        libgl1 \
    && case "$(dpkg --print-architecture)" in \
         amd64) mc_arch=amd64 ;; \
         arm64) mc_arch=arm64 ;; \
         *) echo "unsupported architecture" >&2; exit 1 ;; \
       esac \
    && curl -fsSL "https://github.com/minio/mc/releases/download/RELEASE.2025-08-13T08-35-41Z/mc.linux-${mc_arch}.RELEASE.2025-08-13T08-35-41Z" -o /usr/local/bin/mc \
    && chmod 755 /usr/local/bin/mc \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g ${container_user_gid} ${container_user_group} \
    && useradd -m -u ${container_user_uid} -g ${container_user_group} -s /bin/bash ${container_user} \
    && mkdir -p /opt/registration-test /config \
    && chown -R ${container_user}:${container_user} /opt/registration-test /config "${work_dir}"

COPY --from=build --chown=${container_user_uid}:${container_user_gid} /out/registration-test.jar /opt/registration-test/registration-test.jar
COPY --from=build --chown=${container_user_uid}:${container_user_gid} /src/registration-test/target/lib /opt/registration-test/testlib
COPY --from=build --chown=${container_user_uid}:${container_user_gid} /src/registration-client/target/lib /opt/registration-test/lib
COPY --from=build --chown=${container_user_uid}:${container_user_gid} /src/registration-test/target/mockmds /opt/registration-test/mockmds
COPY --from=build --chown=${container_user_uid}:${container_user_gid} /src/registration-test/src/main/resources /opt/registration-test/resources
COPY --chown=${container_user_uid}:${container_user_gid} registration-test/entrypoint.sh /opt/registration-test/entrypoint.sh
RUN chmod 755 /opt/registration-test/entrypoint.sh

USER ${container_user_uid}:${container_user_gid}
WORKDIR /home/mosip

ENTRYPOINT ["bash", "/opt/registration-test/entrypoint.sh"]
CMD ["all"]
