# Kubernetes deployment

## 1. Status

**Not used.** Production runs as a single container on Render. This document describes what a
Kubernetes deployment would look like if the platform changed, so the work is scoped.

## 2. Prerequisites for Kubernetes

| Requirement | Why |
| --- | --- |
| Container image in a registry | Kubernetes cannot build from a Dockerfile |
| External databases | Neon and Supabase remain managed services |
| Secret management | `Secret` objects or an external secret store |
| Ingress with TLS | HTTPS for the API and WSS for chat |
| Liveness and readiness probes | `/health` (a readiness probe should check the database) |
| Persistent volume | Not required — the app keeps no local state |

## 3. Manifests (sketch)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: family-wishes
spec:
  replicas: 1
  selector:
    matchLabels: { app: family-wishes }
  template:
    metadata:
      labels: { app: family-wishes }
    spec:
      containers:
        - name: app
          image: registry.example.com/family-wishes:1.0.0
          ports: [{ containerPort: 8080 }]
          envFrom:
            - secretRef: { name: family-wishes-secrets }
          resources:
            requests: { cpu: '250m', memory: '512Mi' }
            limits: { cpu: '1000m', memory: '1Gi' }
          livenessProbe:
            httpGet: { path: /health, port: 8080 }
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet: { path: /health, port: 8080 }
            initialDelaySeconds: 30
            periodSeconds: 10
```

```yaml
apiVersion: v1
kind: Service
metadata:
  name: family-wishes
spec:
  selector: { app: family-wishes }
  ports: [{ port: 80, targetPort: 8080 }]
```

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: family-wishes
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt
    nginx.ingress.kubernetes.io/proxy-read-timeout: '3600'   # WebSocket
spec:
  tls: [{ hosts: [api.example.com], secretName: family-wishes-tls }]
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service: { name: family-wishes, port: { number: 80 } }
```

## 4. Things that need attention on Kubernetes

| Concern | Detail |
| --- | --- |
| Replicas > 1 | Rate limiting and caches are per pod; Quartz clustering must be verified |
| Startup time | `ddl-auto=update` and Quartz init run on every pod; with several pods they race |
| WebSocket | The ingress must allow upgrades and long timeouts |
| Secrets | Use `Secret` objects; never bake them into the image |
| Probes | `/health` does not check dependencies; add a readiness endpoint that pings the database |
| Logs | stdout is collected by the platform; no file appender |
| Time zone | Set `TZ=Asia/Kolkata` for consistency in logs |
| Graceful shutdown | `terminationGracePeriodSeconds` should exceed the longest job step |
| Jobs | Quartz is in-process; do not also run Kubernetes CronJobs for the same tasks |

## 5. Local cluster (KIND) — optional

```bash
kind create cluster --name family-wishes
docker build -t family-wishes:dev .
kind load docker-image family-wishes:dev --name family-wishes
kubectl apply -f k8s/secret.yaml -f k8s/deployment.yaml -f k8s/service.yaml
kubectl port-forward svc/family-wishes 8080:80
```

## 6. Migration path from Render

1. Publish the image to a registry (GHCR or Docker Hub).
2. Create the namespace, secret and deployment.
3. Point DNS at the ingress; keep Render running until the new host passes smoke tests.
4. Switch UptimeRobot to the new `/health`.
5. Decommission Render after a stable week.

## 7. Cost and complexity

| Item | Render | Kubernetes |
| --- | --- | --- |
| Setup effort | Hours | Days |
| Operations | Platform-managed | Cluster, ingress, certificates, secrets |
| Scaling | Manual instances | Horizontal pod autoscaler |
| Fit for this workload | Good | Overkill unless other services share the cluster |

**Recommendation:** stay on Render until there are several services or a multi-tenant requirement.
