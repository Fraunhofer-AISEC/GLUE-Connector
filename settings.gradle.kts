plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0",
    id("com.google.cloud.tools.jib") version "3.4.3"

}
rootProject.name = "uc-container-orchestration"

includeBuild("odrl-parser")


