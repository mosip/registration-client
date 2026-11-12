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

Use a Job. Put `config.properties` in a ConfigMap key of the same name, mount it at `/config`, and mount a persistent volume at `/home/mosip`. Give the container about 3Gi of memory (the client heap is 2Gi). The pod must reach `mosip.hostname`.

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: registration-test
spec:
  backoffLimit: 0
  template:
    spec:
      hostname: reg-automation-01
      restartPolicy: Never
      containers:
        - name: registration-test
          image: registration-test:local
          args: ["all"]
          env:
            - name: REG_CFG_mosip.hostname
              value: api-internal.example.mosip.net
          resources:
            requests:
              memory: 3Gi
            limits:
              memory: 4Gi
          volumeMounts:
            - name: config
              mountPath: /config
            - name: data
              mountPath: /home/mosip
      volumes:
        - name: config
          configMap:
            name: registration-test-config
        - name: data
          persistentVolumeClaim:
            claimName: registration-test-data
```

The container runs as uid 1001 (`mosip`). The volume needs to be writable by that user.

## Reports and MinIO

The tests write the Extent report to `/home/mosip/report/extentReport<yyyyMMddHHmm>.html`. Failure screenshots are in `/home/mosip/snapshot/`. That Java name is unchanged, so a local `run.bat` still behaves as it does today.

After the run the container copies the newest report to a stable file:

- default name: `report/registration-test-<hostname>.html`
- override with `-e REPORT_NAME=regclient-dev2.html`

Two ways to land that file in MinIO:

Mount the report directory when the bucket is already exposed as a volume (MinIO CSI or an equivalent mount):

```yaml
volumeMounts:
  - name: minio-reports
    mountPath: /home/mosip/report
```

Or let the container upload with the MinIO client. Set `MINIO_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` and `MINIO_BUCKET`. Optional `MINIO_PREFIX` defaults to `registration-test`.

```bash
docker run --rm \
  --hostname reg-automation-01 \
  -v /path/to/config.properties:/config/config.properties:ro \
  -v registration-test-data:/home/mosip \
  -e MINIO_URL=http://minio.minio:9000 \
  -e MINIO_ACCESS_KEY=minio \
  -e MINIO_SECRET_KEY=minio123 \
  -e MINIO_BUCKET=qa-reports \
  -e REPORT_NAME=regclient-dev2.html \
  registration-test:local
```

That writes:

- `qa-reports/registration-test/reg-automation-01/<timestamp>/report/` (every report file, including the renamed copy)
- `qa-reports/registration-test/reg-automation-01/<timestamp>/snapshot/`
- `qa-reports/registration-test/reg-automation-01/latest.html` (the renamed report, replaced each run)

`latest.html` is the object to open after a run. The timestamp folder keeps the earlier runs.
