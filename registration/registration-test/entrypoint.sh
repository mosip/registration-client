#!/bin/bash
# Runs the same sequence as runAll.bat inside the container.
#   all      setup, then initial launch, then the suite (default)
#   setup    environment setup only
#   initial  initial launch only
#   test     suite only
# Extra arguments are passed to the client, as runAll.bat does.

set -euo pipefail

cd "${work_dir}"

link_dir() {
    local name="$1"
    local target="$2"
    if [ -L "${name}" ] || [ ! -e "${name}" ]; then
        ln -sfn "${target}" "${name}"
    fi
}

link_dir registration-test.jar /opt/registration-test/registration-test.jar
link_dir lib /opt/registration-test/lib
link_dir testlib /opt/registration-test/testlib
link_dir mockmds /opt/registration-test/mockmds

if [ ! -d repository_eng ]; then
    cp -a /opt/registration-test/resources/repository_eng repository_eng
fi

# The image file is the suite config. A mount at /config replaces it for a local
# run. Always copy again so a volume left from an older run cannot hide new keys.
if [ -f /config/config.properties ]; then
    cp /config/config.properties config.properties
else
    cp /opt/registration-test/resources/config.properties config.properties
fi
sed -i 's/\r$//' config.properties

# Rancher injects a ConfigMap or Secret with envFrom. A variable whose name is a
# config key, or contains a dot (mosip.hostname, setup.admin.password), overrides
# the file. This is the same idea as uitest-admin reading System.getenv(key).
apply_config_from_env() {
    local -A known=()
    local key value line
    while IFS= read -r key; do
        [ -n "${key}" ] && known["${key}"]=1
    done < <(awk '
        /^[[:space:]]*#/ { next }
        {
            sep = index($0, ":")
            eq = index($0, "=")
            if (eq > 0 && (sep == 0 || eq < sep)) sep = eq
            if (sep > 1) print substr($0, 1, sep - 1)
        }
    ' config.properties)
    while IFS= read -r line || [ -n "${line}" ]; do
        key="${line%%=*}"
        value="${line#*=}"
        case "${key}" in
            ''|REG_CFG_*|MINIO_*|REPORT_NAME|HEADLESS|PATH|HOME|HOSTNAME|PWD|SHLVL|OLDPWD|LANG|LC_*|JAVA_*|work_dir)
                continue
                ;;
        esac
        if [[ "${key}" == *.* ]] || [[ -n "${known[${key}]+x}" ]]; then
            set_config "${key}" "${value}"
        fi
    done < <(env)
}

set_config() {
    local key="$1"
    local value="$2"
    local tmp
    tmp="$(mktemp)"
    awk -v key="${key}" -v value="${value}" '
        {
            sep = index($0, ":")
            eq = index($0, "=")
            if (eq > 0 && (sep == 0 || eq < sep)) sep = eq
            if (sep > 1 && substr($0, 1, sep - 1) == key) {
                print key "=" value
                found = 1
                next
            }
            print
        }
        END {
            if (!found) print key "=" value
        }
    ' config.properties > "${tmp}"
    mv "${tmp}" config.properties
}

config_value() {
    awk -v key="$1" '
        {
            sep = index($0, ":")
            eq = index($0, "=")
            if (eq > 0 && (sep == 0 || eq < sep)) sep = eq
            if (sep > 1 && substr($0, 1, sep - 1) == key) {
                print substr($0, sep + 1)
                exit
            }
        }
    ' config.properties
}

apply_config_from_env

while IFS= read -r line; do
    case "${line}" in
        REG_CFG_*)
            rest="${line#REG_CFG_}"
            key="${rest%%=*}"
            value="${rest#*=}"
            if [ -n "${key}" ]; then
                set_config "${key}" "${value}"
            fi
            ;;
    esac
done < <(env)

if [ "${HEADLESS:-true}" != "false" ]; then
    set_config headless Y
fi

# The client reads mosip.hostname from the services jar, then from Derby.
# The mounted config is what the tests use, so put that host first on the classpath.
apply_client_hostname() {
    local host services dest
    host="$(config_value mosip.hostname)"
    host="${host//[$' \r']/}"
    if [ -z "${host}" ]; then
        return 0
    fi
    services="$(find -H lib -maxdepth 1 -type f -name 'registration-services-*.jar' \
        ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print -quit)"
    if [ -z "${services}" ]; then
        echo "registration-services jar was not found under lib" >&2
        exit 1
    fi
    rm -rf client-config
    mkdir -p client-config
    ( cd client-config && jar xf "../${services}" props/mosip-application.properties )
    dest="client-config/props/mosip-application.properties"
    awk -v host="${host}" '
        BEGIN { found_host = 0; found_upgrade = 0 }
        {
            sep = index($0, "=")
            if (sep > 1) {
                key = substr($0, 1, sep - 1)
                if (key == "mosip.hostname") {
                    print key "=" host
                    found_host = 1
                    next
                }
                if (key == "mosip.client.upgrade.server.url") {
                    print key "=https://" host
                    found_upgrade = 1
                    next
                }
            }
            print
        }
        END {
            if (!found_host) print "mosip.hostname=" host
            if (!found_upgrade) print "mosip.client.upgrade.server.url=https://" host
        }
    ' "${dest}" > "${dest}.tmp"
    mv "${dest}.tmp" "${dest}"
    echo "Client hostname set to ${host}"
}

# The synced global param keeps object.store.base.location=../packets, which
# overrides the classpath file. From /home/mosip that directory is /home/packets.
# The image creates it because /home is not writable by the runtime user.
prepare_packet_store() {
    local packet_dir
    packet_dir="$(dirname "${work_dir}")/packets"
    if [ ! -d "${packet_dir}" ] || [ ! -w "${packet_dir}" ]; then
        echo "Packet directory ${packet_dir} is missing or not writable" >&2
        exit 1
    fi
    echo "Packet store directory ${packet_dir}"
}

apply_client_hostname
prepare_packet_store

mode="${1:-all}"
case "${mode}" in
    all|setup|initial|test)
        shift || true
        ;;
    *)
        mode=all
        ;;
esac

if [ -d client-config ]; then
    classpath="client-config:registration-test.jar:lib/*:testlib/*"
else
    classpath="registration-test.jar:lib/*:testlib/*"
fi
common_opts=(
    -Dpath.config=/config.properties
    -Dfile.encoding=UTF-8
    --add-opens java.base/java.lang=ALL-UNNAMED
)
javafx_opts=(
    --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics
    --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED
    --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
)

monocle_jar() {
    local jar
    jar="$(find -H lib testlib -maxdepth 1 -type f -name 'openjfx-monocle-21*.jar' -print -quit)"
    if [ -z "${jar}" ]; then
        echo "openjfx-monocle 21 was not found under lib or testlib" >&2
        exit 1
    fi
    printf '%s\n' "${jar}"
}

# Temurin does not ship JavaFX. The runnable modules are the platform jars
# (javafx-*-linux.jar); the unscoped javafx-*.jar files are empty placeholders.
javafx_module_dir() {
    local dir jar found=0
    dir="${work_dir}/.javafx-modules"
    mkdir -p "${dir}"
    while IFS= read -r jar; do
        ln -sfn "$(pwd)/${jar}" "${dir}/$(basename "${jar}")"
        found=1
    done < <(find -H lib -maxdepth 1 -type f -name 'javafx-*-linux*.jar' -print)
    if [ "${found}" -eq 0 ]; then
        echo "JavaFX Linux modules were not found under lib" >&2
        exit 1
    fi
    printf '%s\n' "${dir}"
}

run_setup() {
    echo "=== Environment setup ==="
    java "${common_opts[@]}" -cp "${classpath}" registrationtest.runapplication.EnvironmentSetup "$@" 2>&1 | tee setup.log
}

run_client() {
    local log_file="$1"
    shift
    local patch=()
    if [ "${HEADLESS:-true}" != "false" ]; then
        patch=(-Dheadless=true --patch-module "javafx.graphics=$(monocle_jar)")
    fi
    echo "=== ${log_file} ==="
    # shellcheck disable=SC2086
    java ${JAVA_OPTS:--Xms512m -Xmx2048m} "${patch[@]}" "${common_opts[@]}" \
        -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver \
        --module-path "$(javafx_module_dir)" \
        "${javafx_opts[@]}" \
        "$@" \
        -cp "${classpath}" \
        registrationtest.runapplication.RegistrationMain 2>&1 | tee "${log_file}"
}

wait_for_propagation() {
    if ! grep -q "SETUP_NEW_RESOURCES=true" setup.log; then
        return 0
    fi
    local minutes
    minutes="$(config_value setup.propagationWaitMinutes)"
    minutes="${minutes//[$' \r']/}"
    if [ -z "${minutes}" ]; then
        minutes=15
    fi
    if [ "${minutes}" = "0" ]; then
        return 0
    fi
    echo "New user or machine was created. Waiting ${minutes} minutes before initial launch."
    sleep "$((minutes * 60))"
}

# Copies the newest extentReport*.html to a stable name, then uploads report/ and snapshot/
# when MinIO settings are present. The Java report name is left as the tests wrote it.
publish_reports() {
    local stamp host report_name src prefix dest
    stamp="$(date +%Y%m%d%H%M%S)"
    host="$(hostname -s 2>/dev/null || hostname)"
    host="$(printf '%s' "${host}" | tr '[:upper:]' '[:lower:]')"
    report_name="${REPORT_NAME:-registration-test-${host}.html}"

    if [ -d report ]; then
        src="$(find -H report -maxdepth 1 -type f -name 'extentReport*.html' -printf '%T@ %p\n' | sort -n | awk 'END { print $2 }')"
        if [ -n "${src}" ]; then
            cp -f "${src}" "report/${report_name}"
            echo "Report copied to report/${report_name} (source $(basename "${src}"))"
        fi
    fi

    # uitest-admin reads these names from the s3 ConfigMap. MINIO_* still wins when set.
    local s3_host s3_key s3_secret s3_account push url access secret bucket
    s3_host="$(printenv 's3-host' 2>/dev/null || true)"
    s3_key="$(printenv 's3-user-key' 2>/dev/null || true)"
    s3_secret="$(printenv 's3-user-secret' 2>/dev/null || true)"
    s3_account="$(printenv 's3-account' 2>/dev/null || true)"
    push="$(printenv 'push-reports-to-s3' 2>/dev/null || true)"
    url="${MINIO_URL:-${s3_host}}"
    access="${MINIO_ACCESS_KEY:-${s3_key}}"
    secret="${MINIO_SECRET_KEY:-${s3_secret}}"
    bucket="${MINIO_BUCKET:-${s3_account}}"
    push="$(printf '%s' "${push}" | tr '[:upper:]' '[:lower:]')"

    if [ -z "${MINIO_URL:-}" ]; then
        case "${push}" in
            yes|y|true) ;;
            *) return 0 ;;
        esac
    fi
    if [ -z "${url}" ] || [ -z "${access}" ] || [ -z "${secret}" ] || [ -z "${bucket}" ]; then
        echo "Report upload needs s3-host, s3-user-key, s3-user-secret and s3-account, or MINIO_URL, MINIO_ACCESS_KEY, MINIO_SECRET_KEY and MINIO_BUCKET" >&2
        return 1
    fi

    prefix="${MINIO_PREFIX:-registration-test}"
    mc alias set regtest "${url}" "${access}" "${secret}" >/dev/null
    mc mb --ignore-existing "regtest/${bucket}" >/dev/null
    dest="regtest/${bucket}/${prefix}/${host}/${stamp}"
    if [ -d report ]; then
        mc cp --recursive report/ "${dest}/report/"
    fi
    if [ -d snapshot ]; then
        mc cp --recursive snapshot/ "${dest}/snapshot/"
    fi
    if [ -f "report/${report_name}" ]; then
        mc cp "report/${report_name}" "regtest/${bucket}/${prefix}/${host}/latest.html"
    fi
    echo "Report uploaded to ${bucket}/${prefix}/${host}/${stamp} and ${bucket}/${prefix}/${host}/latest.html"
}

finish() {
    local code=$?
    if ! publish_reports; then
        echo "Publishing the report failed" >&2
        if [ "${code}" -eq 0 ]; then
            code=1
        fi
    fi
    exit "${code}"
}
trap finish EXIT

case "${mode}" in
    setup)
        run_setup "$@"
        ;;
    initial)
        run_client startup.log -Dmodule.run=initiallaunch "$@"
        ;;
    test)
        run_client startup.log "$@"
        ;;
    all)
        run_setup
        wait_for_propagation
        run_client startup-initial.log -Dmodule.run=initiallaunch "$@"
        run_client startup.log "$@"
        ;;
esac
