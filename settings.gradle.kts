plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "container-pipeline"

includeBuild("odrl-parser")
