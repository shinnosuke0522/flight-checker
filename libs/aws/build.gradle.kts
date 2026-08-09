plugins {
    alias(libs.plugins.quarkus.plugin)
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.aws.bom))

    implementation(libs.bundles.core)
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.arc)

    implementation(libs.aws.sdk.core)
    implementation(libs.aws.sdk.auth)
    implementation(libs.aws.sdk.regions)
}
