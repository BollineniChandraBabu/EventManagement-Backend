# Documentation index

Documentation for the **Golden Greetings** backend (`family-wishes`, Spring Boot 3.3 / Java 17).
Everything here describes the code in this repository as it stands at version `1.0.0`.

> The web UI (Angular, hosted on Firebase) lives in a separate repository. This repository is the
> REST/WebSocket backend only. Where a document mentions "Angular", it refers to that external
> client.

## Product and requirements

| Document | Contents |
| --- | --- |
| [project-overview.md](business/project-overview.md) | Business purpose, scope, objectives, high-level functionality |
| [business-requirements.md](business/business-requirements.md) | Business problems, goals, expected outcomes |
| [system-requirements.md](business/system-requirements.md) | Functional and non-functional requirements summary |
| [functional-requirements.md](business/functional-requirements.md) | Detailed functionality and business rules |
| [non-functional-requirements.md](business/non-functional-requirements.md) | Performance, security, availability, scalability, usability |
| [user-stories.md](business/user-stories.md) | Stories with acceptance notes |
| [use-cases.md](business/use-cases.md) | Actors, preconditions, main and alternate flows |
| [acceptance-criteria.md](business/acceptance-criteria.md) | Definition of done per feature |
| [scope-and-limitations.md](business/scope-and-limitations.md) | In scope, out of scope, known limitations |
| [glossary.md](business/glossary.md) | Business terms, abbreviations, domain vocabulary |

## Architecture and design

| Document | Contents |
| --- | --- |
| [system-architecture.md](architecture/system-architecture.md) | Components, layers, runtime topology |
| [system-design.md](architecture/system-design.md) | Technical design of modules and interactions |
| [high-level-design.md](architecture/high-level-design.md) | Services, integrations, data flow at a glance |
| [low-level-design.md](architecture/low-level-design.md) | Classes, methods, responsibilities |
| [design-patterns.md](architecture/design-patterns.md) | Patterns used and why |
| [data-flow.md](architecture/data-flow.md) | How data moves through API, services, databases, storage |
| [sequence-diagrams.md](architecture/sequence-diagrams.md) | Step-by-step interactions for key workflows |
| [component-diagram.md](architecture/component-diagram.md) | Components and dependencies |
| [deployment-architecture.md](architecture/deployment-architecture.md) | Infrastructure and deployment topology |
| [integration-design.md](architecture/integration-design.md) | External APIs and integration flows |
| [architecture-decision-records/](architecture/architecture-decision-records/README.md) | Accepted decisions with rationale |

## Backend, data and configuration

| Document | Contents |
| --- | --- |
| [backend-design.md](backend/backend-design.md) | Layers, packages, responsibilities |
| [api-specification.md](backend/api-specification.md) | Endpoints, payloads, status codes |
| [database-design.md](backend/database-design.md) | Tables, columns, indexes, constraints |
| [entity-relationship-diagram.md](backend/entity-relationship-diagram.md) | Relationships and ER diagram |
| [data-dictionary.md](backend/data-dictionary.md) | Field meanings |
| [business-logic.md](backend/business-logic.md) | Rules and processing logic |
| [authentication-and-authorization.md](backend/authentication-and-authorization.md) | Authn, roles, access control |
| [exception-handling.md](backend/exception-handling.md) | Error strategy and payloads |
| [logging-and-monitoring.md](backend/logging-and-monitoring.md) | Logging, metrics, health |
| [caching-strategy.md](backend/caching-strategy.md) | Caches, TTLs, eviction |
| [external-api-integrations.md](backend/external-api-integrations.md) | External endpoints and auth |
| [configuration-reference.md](backend/configuration-reference.md) | Properties and environment variables |
| [background-jobs.md](backend/background-jobs.md) | Schedulers and async work |
| [dependency-management.md](backend/dependency-management.md) | Libraries and their purpose |

## Testing

| Document | Contents |
| --- | --- |
| [test-strategy.md](testing/test-strategy.md) | Approach and levels |
| [test-plan.md](testing/test-plan.md) | Scope, resources, schedule, responsibilities |
| [test-cases.md](testing/test-cases.md) | Scenarios and expected results |
| [test-data.md](testing/test-data.md) | Data requirements and preparation |
| [unit-testing.md](testing/unit-testing.md) | Unit test approach |
| [integration-testing.md](testing/integration-testing.md) | Backend, database, integrations |
| [api-testing.md](testing/api-testing.md) | Endpoint and contract validation |
| [e2e-testing.md](testing/e2e-testing.md) | End-to-end workflows |
| [performance-testing.md](testing/performance-testing.md) | Load, stress, capacity |
| [security-testing.md](testing/security-testing.md) | Security scenarios |
| [test-coverage-report.md](testing/test-coverage-report.md) | Coverage and gaps |
| [defect-tracking.md](testing/defect-tracking.md) | Severity, priority, lifecycle |

## Build, deployment, environments

| Document | Contents |
| --- | --- |
| [build-and-run.md](deployment/build-and-run.md) | Local build and start |
| [environment-setup.md](deployment/environment-setup.md) | Developer prerequisites |
| [environment-configuration.md](deployment/environment-configuration.md) | DEV / QA / UAT / PROD differences |
| [deployment-guide.md](deployment/deployment-guide.md) | Step-by-step deployment |
| [ci-cd-pipeline.md](deployment/ci-cd-pipeline.md) | Build, test, package, deploy |
| [release-management.md](deployment/release-management.md) | Versioning and approvals |
| [rollback-plan.md](deployment/rollback-plan.md) | Restoring a previous version |
| [database-migration.md](deployment/database-migration.md) | Schema change procedure |
| [infrastructure-requirements.md](deployment/infrastructure-requirements.md) | CPU, memory, storage, network |
| [containerization.md](deployment/containerization.md) | Docker build and run |
| [kubernetes-deployment.md](deployment/kubernetes-deployment.md) | Kubernetes options (not currently used) |
| [secrets-management.md](deployment/secrets-management.md) | Credentials handling |

## Security

| Document | Contents |
| --- | --- |
| [security-design.md](security/security-design.md) | Security architecture |
| [threat-model.md](security/threat-model.md) | Threats and mitigations |
| [access-control-matrix.md](security/access-control-matrix.md) | Role-by-endpoint permissions |
| [data-classification.md](security/data-classification.md) | Sensitivity of stored data |
| [security-requirements.md](security/security-requirements.md) | Requirements and compliance |
| [secure-coding-guidelines.md](security/secure-coding-guidelines.md) | Practices and pitfalls |
| [vulnerability-management.md](security/vulnerability-management.md) | Scanning and remediation |
| [privacy-and-data-retention.md](security/privacy-and-data-retention.md) | Personal data and retention |

## Operations and support

| Document | Contents |
| --- | --- |
| [operations-runbook.md](operations/operations-runbook.md) | Routine operations |
| [troubleshooting-guide.md](operations/troubleshooting-guide.md) | Symptoms and fixes |
| [health-checks.md](operations/health-checks.md) | Health endpoints |
| [monitoring-and-alerting.md](operations/monitoring-and-alerting.md) | Metrics, alerts, thresholds |
| [backup-and-recovery.md](operations/backup-and-recovery.md) | Backups and restore |
| [disaster-recovery.md](operations/disaster-recovery.md) | Major outage strategy |
| [known-issues.md](operations/known-issues.md) | Current defects and limitations |
| [support-guide.md](operations/support-guide.md) | Investigating user reports |
| [incident-management.md](operations/incident-management.md) | Classification and escalation |
| [maintenance-guide.md](operations/maintenance-guide.md) | Routine upkeep |

## Project management

| Document | Contents |
| --- | --- |
| [project-plan.md](project/project-plan.md) | Milestones and timeline |
| [work-breakdown-structure.md](project/work-breakdown-structure.md) | Tasks and deliverables |
| [risk-register.md](project/risk-register.md) | Risks and mitigations |
| [change-management.md](project/change-management.md) | Handling change |
| [decision-log.md](project/decision-log.md) | Decision history |
| [release-notes.md](project/release-notes.md) | Per-release changes |
| [changelog.md](project/changelog.md) | Chronological change record |
| [contribution-guide.md](project/contribution-guide.md) | Workflow and rules |
| [coding-standards.md](project/coding-standards.md) | Java conventions |
| [onboarding-guide.md](project/onboarding-guide.md) | New developer guide |
