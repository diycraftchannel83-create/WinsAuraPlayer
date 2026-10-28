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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  // Prevent outdated transitive Fragment versions from entering release builds.
  // Keep Fragment artifacts aligned on a stable AndroidX release.
  resolutionStrategy {
    eachDependency {
      if (requested.group == "androidx.fragment" &&
          requested.name in setOf("fragment", "fragment-ktx", "fragment-compose")) {
        useVersion("1.9.1")
        because("Use the current stable AndroidX Fragment release instead of outdated transitive versions.")
      }
    }
  }
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "WinsAuraPlayer"

include(":app")
