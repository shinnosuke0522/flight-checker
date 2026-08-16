import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.openapi.generator") version "7.8.0"
    kotlin("plugin.serialization")
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    implementation(project(":domain"))
    implementation(libs.swagger.annotations)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.arrow.core)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // For Test
    listOf(
        "testImplementation",
        "integrationTestImplementation",
        "testFixturesImplementation",
    ).forEach { configuration ->
        add(configuration, platform(libs.kotest.bom))
        add(configuration, libs.bundles.test.core)
    }
    listOf(
        "integrationTestImplementation",
        "testFixturesImplementation",
    ).forEach { configuration ->
        add(configuration, libs.bundles.container.test.base)
        add(configuration, libs.wiremock)
        add(configuration, libs.openapi.validator.wiremock)
    }
}

configurations.all {
    exclude(group = "com.github.tomakehurst", module = "wiremock-jre8")
    exclude(group = "org.eclipse.jetty", module = "jetty-alpn-openjdk8-client")
    exclude(group = "org.eclipse.jetty", module = "jetty-alpn-openjdk8-server")
}

tasks.test {
    useJUnitPlatform()
}

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-ktor")
    validateSpec.set(false)
    inputSpec.set("$projectDir/src/main/resources/contract/aerodatabox-api-v1.15.1.0.yaml")
    outputDir.set("${layout.buildDirectory.get().asFile.path}/generated/openapi")
    apiPackage.set("com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.api")
    modelPackage.set("com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model")
    apiNameSuffix.set("Client")

    configOptions.put("dateLibrary", "java8")
    configOptions.put("serializationLibrary", "kotlinx_serialization")

    generateApiTests.set(false)
    generateModelTests.set(false)
}

tasks.named("openApiGenerate") {
    val buildDirPath = layout.buildDirectory.get().asFile.path
    doLast {
        val apiClient = file(
            path = "$buildDirPath/generated/openapi/src/main/kotlin/org/openapitools/client/infrastructure/ApiClient.kt"
        )
        if (apiClient.exists()) {
            val content = apiClient.readText()
            apiClient.writeText(
                content.replace(
                    oldValue = "parseDateToQueryString(value)",
                    newValue = "parseDateToQueryString(value as Any)"
                )
            )
        }

        // Rename *Api to *Client
        val apiDir = file(
            path = "$buildDirPath/generated/openapi/src/main/kotlin/" +
                "com/shinnosuke0522/flight/checker/adapter/outbound/aerodatabox/api"
        )
        if (apiDir.exists()) {
            apiDir.listFiles()?.forEach { f ->
                if (f.name.endsWith("Api.kt")) {
                    val newName = f.name.replace("Api.kt", "Client.kt")
                    val oldClassName = f.nameWithoutExtension
                    val newClassName = newName.removeSuffix(".kt")
                    val content = f.readText().replace(oldClassName, newClassName)
                    f.writeText(content)
                    f.renameTo(File(apiDir, newName))
                }
            }
        }

        // 警告抑止の追加 (RedundantCallOfConversionMethod, DEPRECATION)
        val generatedSrcDir = file("$buildDirPath/generated/openapi/src/main/kotlin")
        if (generatedSrcDir.exists()) {
            generatedSrcDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { ktFile ->
                val content = ktFile.readText()
                if (content.contains("@file:Suppress(\n")) {
                    if (!content.contains("\"RedundantCallOfConversionMethod\"")) {
                        ktFile.writeText(
                            content.replace(
                                "@file:Suppress(\n",
                                "@file:Suppress(\n    \"RedundantCallOfConversionMethod\",\n    \"DEPRECATION\",\n"
                            )
                        )
                    }
                }
            }
        }

        // Ktor 3.x 互換性対応: generated/openapi 内の @InternalAPI アノテーションを削除する
        val authDir = file("$buildDirPath/generated/openapi/src/main/kotlin/org/openapitools/client/auth")
        if (authDir.exists()) {
            authDir.listFiles()?.forEach { f ->
                if (f.extension == "kt") {
                    val content = f.readText()
                    if (content.contains("InternalAPI")) {
                        f.writeText(
                            content.replace("import io.ktor.util.InternalAPI", "")
                                .replace("@OptIn(InternalAPI::class)", "")
                        )
                    }
                }
            }
        }
    }
}

sourceSets {
    main {
        kotlin.srcDir("${layout.buildDirectory.get().asFile.path}/generated/openapi/src/main/kotlin")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(tasks.openApiGenerate)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        if (name == "compileKotlin") {
            freeCompilerArgs.add("-nowarn")
        }
    }
}

tasks.withType<BootJar> {
    enabled = false
}

tasks.jar {
    enabled = true
}
