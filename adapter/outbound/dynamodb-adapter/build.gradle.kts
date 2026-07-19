import org.springframework.boot.gradle.tasks.bundling.BootJar

dependencies {
    // BOM
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    implementation(platform(libs.aws.bom))
    testImplementation(platform(libs.kotest.bom))

    // Dependencies
    implementation(libs.bundles.core)
    implementation(libs.bundles.spring.boot.base)
    testImplementation(libs.bundles.test.core)

    implementation(project(":domain"))
    implementation(project(":libs:aws"))

    implementation(libs.aws.sdk.dynamodb)
    implementation(libs.aws.sdk.dynamodb.enhanced)
    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.jackson.module.kotlin)
    testImplementation(libs.kotest.assertions.arrow)
    implementation(libs.spring.tx)
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

tasks.withType<BootJar> {
    enabled = false
}
