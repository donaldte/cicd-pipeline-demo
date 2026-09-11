# cicd-pipeline-demo

A deliberately small Java service used to demonstrate a complete,
professional CI/CD pipeline on GitHub Actions — built for the *DevOps
Moderne* webinar (Squad+ Technologies)

**The pipeline is the point of this repository, not the app.** The service
itself is a couple of JSON endpoints.

## The application

- Plain Java 21, **zero third-party runtime dependencies** (only JUnit at
  test scope) — built on `com.sun.net.httpserver`, part of the JDK. This
  keeps the demo focused on the pipeline instead of a framework, and means
  nothing here can break because of an unrelated library update.
- `GET /health` → `{"status":"UP"}`
- `GET /api/greeting?name=X` → `{"message":"Hello, X!"}` (defaults to
  `World`, rejects names over 60 characters with a 400)

Run it locally:

```bash
mvn clean package
java -jar target/cicd-pipeline-demo.jar
curl http://localhost:8080/health
```

Or via Docker:

```bash
docker build -t cicd-pipeline-demo .
docker run -p 8080:8080 cicd-pipeline-demo
```

## Branch & environment model

There is **one long-lived branch: `main`**. Everything else is a
short-lived feature branch merged in through a pull request. `dev`,
`staging` and `production` in this project are **deployment targets**
(GitHub Environments), not branches — don't look for a `dev` branch, it
doesn't exist on purpose. 

## The pipeline, in three workflows

| File | Trigger | What it does |
|---|---|---|
| [`ci.yml`](.github/workflows/ci.yml) | `pull_request` → `main` | Quality gate: format, dependency scan, SAST, unit/integration tests, build & scan a preview image. Nothing here is pushed or released. |
| [`release-and-deploy-dev.yml`](.github/workflows/release-and-deploy-dev.yml) | `push` → `main` (i.e. after merge) | Rebuilds & tests, runs semantic-release against Conventional Commits, and — **only if a new version was actually published** — pushes a versioned image to GHCR, DAST-scans it, and auto-deploys it to `dev`. |
| [`cd-manual.yml`](.github/workflows/cd-manual.yml) | `workflow_dispatch` only | Promotes an already-released version to a **chosen environment** (staging/production), gated by that environment's approval rules. |

Shared logic is factored out rather than copy-pasted:

- [`reusable-build-test.yml`](.github/workflows/reusable-build-test.yml) —
  a `workflow_call` reusable workflow; both `ci.yml` and
  `release-and-deploy-dev.yml` call it instead of duplicating the
  build/test steps.
- [`.github/actions/setup-build-env`](.github/actions/setup-build-env) — a
  composite action (JDK + Maven cache) used by every job that needs Java.
- `release-and-deploy-dev.yml` also demonstrates a **YAML anchor**: the
  registry/image env vars are defined once (`env: &image_env` on the
  `image` job) and reused as-is in the `dast` and `deploy-dev` jobs via
  `env: *image_env`, so the two jobs can't drift out of sync. 

## Versioning: semantic-release, not a version you type in by hand

`pom.xml` pins `<version>0.0.0</version>` **on purpose and permanently** —
it is never rewritten. The real version lives in git tags, GitHub Releases
and the Docker image tag, computed by
[semantic-release](https://semantic-release.gitbook.io/) from
[Conventional Commits](https://www.conventionalcommits.org/) on `main`:

| Commit prefix | Effect |
|---|---|
| `fix: ...` | patch release (1.2.0 → 1.2.1) |
| `feat: ...` | minor release (1.2.0 → 1.3.0) |
| `feat!: ...` or a `BREAKING CHANGE:` footer | major release (1.2.0 → 2.0.0) |
| `chore:`, `docs:`, `refactor:`, `test:`, ... | no release at all |

Configuration: [`.releaserc.json`](./.releaserc.json).

## One-time setup 

1. **Workflow permissions** — Settings → Actions → General → Workflow
   permissions → **Read and write permissions**. Without this,
   semantic-release cannot create tags/releases and the image push to GHCR
   will fail.
2. **Environments** — Settings → Environments → create `dev`, `staging`,
   `production`.
   - Leave `dev` unprotected (it must deploy automatically, with no
     human in the loop).
   - On `production` (and optionally `staging`), add yourself as a
     **required reviewer**. This is what makes `cd-manual.yml` pause for
     approval — it's the whole point of the manual-CD demo.
3.
