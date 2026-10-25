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
    }
}
rootProject.name = "aetherplay-suite"

include(":apps:lab")
include(":apps:space")
include(":core:model")
include(":core:protocol")
include(":core:design")
include(":engine:search")
