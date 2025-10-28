plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "uc-container-orchestration"

includeBuild("odrl-parser") {
    dependencySubstitution {  }
    pluginManagement {
        resolutionStrategy {
            eachPlugin {
                if (requested.id.id == "org.gradle.toolchains.foojay-resolver-convention") {
                    useVersion("1.0.0")
                }
            }
        }
    }
}
