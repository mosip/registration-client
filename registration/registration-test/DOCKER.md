# Registration Client automation on Linux (Docker / Rancher)

The image runs the same headless flow as `runAll.bat -Dheadless=true`. It does not use the Windows `jre` folder. JavaFX is drawn with Monocle, and Mock MDS is started by the automation on port 4501 inside the container.

Build the registration reactor first, on Linux, then package that output. The GitHub Action does this in `build-regclient-local` and the test image job waits for it.

```bash
mvn -f pom.xml package -DskipTests -Dgpg.skip
docker build -f registration-test/Dockerfile -t registration-test:local .
```

## Run

Mount the same `config.properties` used locally. Keep a volume on `/home/mosip` so `.mosipkeys`, Derby, logs and the Extent report survive a restart. The machine name registered in masterdata is the container hostname, lowercased, so set a stable hostname.

`mosip.hostname` in that file is also applied to the client before it starts. The services jar is built with `dev.mosip.net`, and the client would keep calling that host. The container copies the jar properties, sets `mosip.hostname` and `mosip.client.upgrade.server.url` from the mounted config, and puts that file ahead of the jar on the classpath. Initial launch deletes the local Derby database, so an older hostname stored there is not reused.

The client stores packets at `../packets`. From the working directory `/home/mosip` that is `/home/packets`. A synced global parameter keeps that relative path, so the image creates `/home/packets` for the runtime user. The startup log prints `Packet store directory /home/packets` before the suite starts.

```bash
docker run --rm \
  --hostname reg-automation-01 \
  -v /path/to/config.properties:/config/config.properties:ro \
  -v registration-test-data:/home/mosip \
  registration-test:local
```

That command runs setup, then the initial launch, then the suite. Other commands:

```bash
docker run --rm --hostname reg-automation-01 -v /path/to/config.properties:/config/config.properties:ro -v registration-test-data:/home/mosip registration-test:local setup
docker run --rm --hostname reg-automation-01 -v /path/to/config.properties:/config/config.properties:ro -v registration-test-data:/home/mosip registration-test:local initial
docker run --rm --hostname reg-automation-01 -v /path/to/config.properties:/config/config.properties:ro -v registration-test-data:/home/mosip registration-test:local test
```

Logs are printed by the container and written under `/home/mosip` (`setup.log`, `startup.log`, `logs/AutomationLogs.log`, `report/`).

Headless is turned on in the container. Set `-e HEADLESS=false` only when the container has a display.

## Change a few keys without a new file

Any environment variable named `REG_CFG_<key>` overwrites that key in `config.properties`. The key is the exact property name, including dots.

```bash
docker run --rm \
  --hostname reg-automation-01 \
  -v /path/to/config.properties:/config/config.properties:ro \
  -v registration-test-data:/home/mosip \
  -e REG_CFG_mosip.hostname=api-internal.dev2.mosip.net \
  -e REG_CFG_operatorId=9343 \
  -e REG_CFG_operatorPwd=admin123 \
  -e REG_CFG_setup.regCenterId=10011 \
  -e REG_CFG_setup.zoneCode=TZT \
  -e "REG_CFG_setup.propagationWaitMinutes=0" \
  registration-test:local
```

A mounted `/config/config.properties` is copied in first. `REG_CFG_*` values are applied after that. With no mount, the image uses the `config.properties` that was in `src/main/resources` at build time.

## Rancher

Use a CronJob or Job, the same way `uitest-admin` is started. Do not mount a properties file. Add the test keys as environment variables from a ConfigMap, and the passwords from a Secret. Rancher does this with **Add from Config Map** and **Add from Secret**, prefix left empty, which is `envFrom`.

The variable name is the config key itself. `mosip.hostname` and `setup.admin.password` are valid. An environment value replaces the same key in the image's `config.properties`. `REG_CFG_<key>` still works and wins over the ConfigMap when both are set.

Report upload uses the same names as uitest-admin's `s3` ConfigMap. Set `push-reports-to-s3` to `yes`. The container uploads when it exits.

| Variable | Meaning |
|---|---|
| `s3-host` | MinIO URL, for example `http://minio.minio:9000` |
| `s3-user-key` | Access key |
| `s3-user-secret` | Secret key |
| `s3-account` | Bucket name |
| `push-reports-to-s3` | `yes` to upload, anything else skips the upload |
| `s3-region` | Accepted and ignored. The MinIO client does not use it |

With hostname `reg-automation-01` and `s3-account=automation`, the objects are:

- `automation/registration-test/reg-automation-01/<timestamp>/report/`
- `automation/registration-test/reg-automation-01/<timestamp>/snapshot/`
- `automation/registration-test/reg-automation-01/latest.html`

The copied report name is `registration-test-<hostname>.html`. Override it with `REPORT_NAME`. The tests still write `extentReport<yyyyMMddHHmm>.html` first, so a local `run.bat` is unchanged.

Give the container about 3Gi of memory (the client heap is 2Gi). Mount a persistent volume at `/home/mosip` so the machine key and Derby survive. The pod must reach `mosip.hostname` and `s3-host`. The container runs as uid 1001 (`mosip`), so that volume must be writable by uid 1001. Set a stable hostname. It is the machine name registered in masterdata.

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: registration-test
  namespace: uitestrig
spec:
  schedule: "0 2 * * *"
  concurrencyPolicy: Forbid
  jobTemplate:
    spec:
      backoffLimit: 0
      template:
        spec:
          restartPolicy: Never
          hostname: reg-automation-01
          containers:
            - name: registration-test
              image: jayesh12234/registration-test:develop
              imagePullPolicy: Always
              args: ["all"]
              envFrom:
                - configMapRef:
                    name: registration-test
                - secretRef:
                    name: registration-test
                - configMapRef:
                    name: s3
              resources:
                requests:
                  memory: 3Gi
                limits:
                  memory: 4Gi
              volumeMounts:
                - name: data
                  mountPath: /home/mosip
          volumes:
            - name: data
              persistentVolumeClaim:
                claimName: registration-test-data
```

`registration-test` ConfigMap holds the non-secret keys (`mosip.hostname`, `operatorId`, `setup.regCenterId`, `setup.zoneCode`, `datadir`, and the other `setup.*` urls). The Secret of the same name holds `operatorPwd`, `supervisorUserpwd`, `reviewerpwd`, `setup.admin.password`, the client secrets, and `regprc.db.password`. The shared `s3` ConfigMap or Secret holds `s3-host`, `s3-user-key`, `s3-user-secret`, `s3-account`, and `push-reports-to-s3=yes`.

`MINIO_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, and `MINIO_BUCKET` still upload the report when `push-reports-to-s3` is unset. Those four override the `s3-*` names when both are present.

Headless is turned on in the container. Set `HEADLESS=false` only when the container has a display.
