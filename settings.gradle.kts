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
        maven(url = "https://jitpack.io")
        maven(url = "https://devrepo.kakao.com/nexus/content/groups/public/")
        maven(url ="https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

rootProject.name = "soomirang"
include(":app")
 