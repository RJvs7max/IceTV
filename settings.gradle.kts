pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "FireLauncher"
include(":app")
Verification: Both the filename box and the code box are filled in, and the green "Commit changes..." button in the top-right corner turns clickable.
