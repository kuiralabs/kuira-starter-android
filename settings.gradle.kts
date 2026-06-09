pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // mavenLocal() FIRST so locally-republished SDK builds (e.g.,
        // for testing a runtime bump against the live alpha01 pin)
        // take precedence over Maven Central. Local-dev only; the
        // template ships without this so cold consumers resolve from
        // Central. Safe to remove once the SDK is published to Central
        // with the desired runtime version.
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "kuira-starter-android"

include(":app")
