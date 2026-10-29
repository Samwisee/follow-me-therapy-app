import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Web first. Android and iOS targets slot in here later; the design
    // system lives in commonMain, so nothing in it is web-specific.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "au.com.followmetherapy.design.resources"
    generateResClass = always
}
