import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.detekt.plugin) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.kover.plugin)
    alias(libs.plugins.allure.report)
    alias(libs.plugins.allure.adapter) apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/snapshot") }
    }
}

subprojects {
    // 中間ディレクトリ (adapter, adapter/inbound など) を除外するため、build.gradle(.kts) があるプロジェクトのみ設定する
    if (!file("build.gradle").exists() && !file("build.gradle.kts").exists()) {
        return@subprojects
    }

    group = "com.github.shinnosuke0522"
    version = "0.0.1-SNAPSHOT"

    // Version Catalog は rootProject から参照する
    val libs = rootProject.libs

    // ==================================
    // Common
    // ==================================

    // Plugin
    pluginManager.apply(libs.plugins.detekt.plugin.get().pluginId)
    pluginManager.apply(libs.plugins.kotlin.jvm.get().pluginId)
    pluginManager.apply(libs.plugins.kover.plugin.get().pluginId)
    pluginManager.apply(libs.plugins.allure.adapter.get().pluginId)
    pluginManager.apply(libs.plugins.gradle.test.fixtures.get().pluginId)
    pluginManager.apply(libs.plugins.gradle.test.suite.get().pluginId)

    @Suppress("UnstableApiUsage")
    configure<TestingExtension> {
        suites {
            val test by getting(JvmTestSuite::class) {
                useJUnitJupiter()
            }
            register<JvmTestSuite>("integrationTest") {
                useJUnitJupiter()
                dependencies {
                    implementation(project())
                    implementation(testFixtures(project()))
                }
                targets {
                    all {
                        testTask.configure {
                            shouldRunAfter(test)
                        }
                    }
                }
            }
            register<JvmTestSuite>("componentTest") {
                useJUnitJupiter()
                dependencies {
                    implementation(project())
                    implementation(testFixtures(project()))
                }
                targets {
                    all {
                        testTask.configure {
                            shouldRunAfter(test)
                            shouldRunAfter(suites.named("integrationTest"))
                        }
                    }
                }
            }
        }
    }

    // 独自テストスイートが、標準の test スイートの依存関係（implementationやKotestなど）を引き継ぐように設定
    configurations {
        named("testFixturesImplementation") {
            extendsFrom(configurations.getByName("implementation"))
        }
        named("testFixturesRuntimeOnly") {
            extendsFrom(configurations.getByName("runtimeOnly"))
        }
        named("integrationTestImplementation") {
            extendsFrom(configurations.getByName("testImplementation"))
        }
        named("integrationTestRuntimeOnly") {
            extendsFrom(configurations.getByName("testRuntimeOnly"))
        }
        named("componentTestImplementation") {
            extendsFrom(configurations.getByName("testImplementation"))
        }
        named("componentTestRuntimeOnly") {
            extendsFrom(configurations.getByName("testRuntimeOnly"))
        }
    }

    dependencies {
        add("detektPlugins", libs.detekt.formatting)
        add("testImplementation", libs.kotest.extentions.allure)
        add("integrationTestImplementation", libs.kotest.extentions.allure)
        add("componentTestImplementation", libs.kotest.extentions.allure)
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        // Detekt に関する設定ファイル
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        // 並列処理
        parallel = true
        // デフォルト設定の上に自分の設定ファイルを適用する
        buildUponDefaultConfig = true
        // レポートファイルに出力されるファイルパスのベースとなる
        basePath = rootDir.absolutePath
    }

    // Java
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(24))
        }
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "21"
    }

    // ==================================
    // App & Infra
    // ==================================
    if (path.startsWith(":app") || path.startsWith(":adapter")) {
        pluginManager.apply(libs.plugins.kotlin.spring.get().pluginId)
        pluginManager.apply(libs.plugins.spring.boot.get().pluginId)
    }
}

// kover
dependencies {
    kover(project(":domain"))
}

kover {
    reports {
        total {
            filters {
                includes {
                    classes("com.shinnosuke0522.flight.checker.*")
                }
            }
            html {
                onCheck = true
            }
            verify {
                rule {
                    bound {
                        coverageUnits.set(CoverageUnit.BRANCH)
                        minValue.set(50)
                    }
                }
            }
        }
    }
}

allure {
    version.set(libs.versions.allure.get())
}
