pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // CCV SDK için flatDir
        flatDir {
            dirs("app/libs")
        }
    }
}

rootProject.name = "CCV Payment"
include(":app")
