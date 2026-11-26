# CI/CD pipeline

## 1. Current state

There is **no CI configuration in the repository** (no `.github/workflows`, no Jenkinsfile, no
GitLab CI file). Delivery relies on:

1. **Render's auto-deploy** — a push to the deployed branch builds the `Dockerfile` and restarts the
   service.
2. **Local checks** — the developer runs `./gradlew test` before merging.

That means the pipeline is effectively *build + containerise + deploy*, with **no automated test
gate**.

## 2. Effective pipeline today

```
developer ──push──► repository ──webhook──► Render build
                                             ├─ FROM gradle:8.14.3-jdk17-alpine
                                             ├─ gradle --no-daemon clean bootJar
                                             ├─ FROM eclipse-temurin:17-jre-alpine
                                             └─ start container → /health
```

| Stage | Automated? | Where |
| --- | --- | --- |
| Compile | Yes (inside the image build) | Render |
| Unit tests | **No** | Developer machine |
| Spotless | Partly (build depends on `spotlessApply`) | Image build |
| Security scan | No | — |
| Container build | Yes | Render |
| Deploy | Yes | Render |
| Smoke test | Manual | — |
| Rollback | Manual | Render dashboard |

## 3. Recommended pipeline

```yaml
# .github/workflows/ci.yml (suggested)
name: ci
on:
  pull_request:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - uses: gradle/actions/setup-gradle@v4
      - name: Compile and test
        run: gradle --no-daemon test
      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: build/reports/tests/test
```

Add these stages next:

| Stage | Tool | Gate |
| --- | --- | --- |
| Unit tests | Gradle | Required |
| Coverage | JaCoCo | Report (non-blocking at first) |
| Static analysis | SpotBugs or Error Prone | Warn |
| Secret scan | gitleaks | Required |
| Dependency scan | Trivy or OWASP Dependency-Check | Fail on critical |
| Container build | Docker | Required |
| Container scan | Trivy image | Warn |
| Contract check | OpenAPI diff | Required |
| Deploy | Render hook | On main only |
| Smoke | `curl /health` + sign-in | After deploy |

## 4. Suggested workflow with container build

```yaml
  docker:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build image
        run: docker build -t family-wishes:${{ github.sha }} .
      - name: Scan image
        uses: aquasecurity/trivy-action@0.28.0
        with:
          image-ref: family-wishes:${{ github.sha }}
          severity: CRITICAL,HIGH
          exit-code: '0'
```

## 5. Deployment triggers

| Trigger | Behaviour |
| --- | --- |
| Push to the deployed branch | Render rebuilds and restarts (current) |
| Pull request | No deploy (recommended: add a preview environment) |
| Manual | Render dashboard or `curl` to a deploy hook |
| Scheduled | None today |

## 6. Environment promotion

| Stage | Source | Gate |
| --- | --- | --- |
| Local | Developer branch | Tests green |
| QA (optional) | main | Smoke suite |
| Production | main | Release owner approval, backup taken |

## 7. Build reproducibility

* The Docker build image pins Gradle `8.14.3` and JDK 17.
* Dependencies come from Maven Central with BOM-pinned versions.
* The jar name is fixed (`family-wishes-1.0.0.jar`), so the Dockerfile `COPY` is stable. Bumping the
  version requires updating the `Dockerfile` too.

## 8. Failure handling

| Failure | Action |
| --- | --- |
| Image build fails (compile/test) | Fix and push; Render keeps the previous deploy running |
| Container starts then crashes | Check logs; the previous instance is already replaced, so roll back |
| Smoke test fails after deploy | Roll back immediately ([rollback-plan.md](rollback-plan.md)) |
| Dependency download failure | Retry; check Maven Central status |

## 9. Security in the pipeline

1. Never echo secrets in build logs.
2. Keep the Render deploy hook and API key in the CI secret store.
3. Pin action versions.
4. Require review on the protected branch.
5. Scan the image before promoting.

## 10. Metrics to track

| Metric | Target |
| --- | --- |
| Build duration | < 8 minutes |
| Test duration | < 3 minutes |
| Deploy frequency | As needed (weekly or on demand) |
| Failed deploy rate | < 10% |
| Time to roll back | < 10 minutes |
