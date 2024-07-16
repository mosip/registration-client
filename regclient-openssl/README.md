## keystore Docker Image
## Overview
This Directory provides a Docker image for generating openssl certificates and secrets. It uses OpenSSL to create a Root CA certificate and a Client certificate, and then packages these into a ConfigMap and Secret for use in Kubernetes applications.
## Docker Image
The Docker image contains:

OpenSSL for certificate management.
kubectl for interacting with Kubernetes clusters.
A script (create-signing-certs.sh) that generates certificates and creates Kubernetes ConfigMap and Secret.
```
create-signing-certs.sh Script
```
This script generates the following:

Root CA Certificate
Client Certificate
Client Certificate Signing Request (CSR)
Keystore (PKCS12 format)
The script then deletes existing ConfigMaps and Secrets, and creates new ones with the generated certificates and the keystore password.


## Kubernetes Job Configuration
The YAML file configures a Kubernetes Job to run the create-signing-certs.sh script. It mounts a volume for storing certificates and sets up environment variables.
The configuration was present in mosip-helm repo.


## Usage Instructions
1. Build the Docker Image
bash
```
docker build -t my-certs-image:latest .
```
2. Push the Docker Image to Your Registry
bash
```
docker push my-certs-image:latest
```
3. Update the Job Configuration
Apply the updated Job configuration:

bash
```
kubectl apply -f path/to/job.yaml
```
4. Verify the Job Execution
Check the logs to verify that the certificates are being generated correctly and the ConfigMap and Secret are created:

bash
```
kubectl logs -f job/{{.Release.Name}}-{{ .Release.Namespace }}-job
```
## Troubleshooting
Common Issues
No such file or directory: Ensure that the certs directory is created before certificates are generated.
Failed to create ConfigMap: Verify that the certificates are created successfully in the certs directory.
Failed to create Secret: Check that the KEYSTORE_PWD environment variable is provided and valid.
Example Commands for Troubleshooting
Check the Status of the Job

```
kubectl describe job {{ .Values.job.name }}-{{ .Release.Namespace }}
```
Inspect the ConfigMap and Secret
```
kubectl get configmap regclient-certs -n {{ .Release.Namespace }} -o yaml
kubectl get secret keystore-secret-env -n {{ .Release.Namespace }} -o yaml
```
Debug Inside the Running Pod

If the job fails, you can debug inside the running pod:
```
kubectl exec -it pod/{{.Release.Name}} -- /bin/bash
```
