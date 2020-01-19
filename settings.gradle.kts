rootProject.name = "ontrack"

include(":ontrack-common")
include(":ontrack-json")
include(":ontrack-model")
include(":ontrack-client")
include(":ontrack-job")
include(":ontrack-dsl")
include(":ontrack-dsl-shell")
include(":ontrack-git")
include(":ontrack-extension-api")
include(":ontrack-extension-support")
include(":ontrack-extension-plugin")
include(":ontrack-database")
include(":ontrack-repository")
include(":ontrack-repository-impl")
include(":ontrack-repository-support")
include(":ontrack-service")
include(":ontrack-tx")
include(":ontrack-ui")
include(":ontrack-ui-graphql")
include(":ontrack-ui-support")
include(":ontrack-web")
include(":ontrack-acceptance")
include(":ontrack-postgresql-migration")
include(":ontrack-docs")

// Test support
include(":ontrack-test-engine")
include(":ontrack-test-support")
include(":ontrack-test-utils")
include(":ontrack-it-utils")

// Abstract DSL
include(":ontrack-kdsl-model")

// Kotlin DSL
include(":ontrack-kdsl-client")
include(":ontrack-kdsl-core")
include(":ontrack-kdsl-impl")
include(":ontrack-kdsl-test")

// Kotlin DSL app
include(":ontrack-kdsl-runner")
include(":ontrack-kdsl-app")

// E2E testing
include(":ontrack-bdd-engine")
include(":ontrack-bdd-model")
include(":ontrack-bdd-definitions")
include(":ontrack-bdd")

// Core extensions
include(":ontrack-extension-artifactory")
include(":ontrack-extension-general")
include(":ontrack-extension-issues")
include(":ontrack-extension-combined")
include(":ontrack-extension-jenkins")
include(":ontrack-extension-jira")
include(":ontrack-extension-scm")
include(":ontrack-extension-svn")
include(":ontrack-extension-git")
include(":ontrack-extension-github")
include(":ontrack-extension-gitlab")
include(":ontrack-extension-stash")
include(":ontrack-extension-ldap")
include(":ontrack-extension-stale")
include(":ontrack-extension-vault")
include(":ontrack-extension-influxdb")
include(":ontrack-extension-sonarqube")
