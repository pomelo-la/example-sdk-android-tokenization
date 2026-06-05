// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.ncorti.ktfmt.gradle.tasks.KtfmtBaseTask

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.ncorti.ktfmt.gradle") version "0.26.0"
}

ktfmt {
    kotlinLangStyle()
    maxWidth.set(140)
}

tasks.withType<KtfmtBaseTask>().configureEach { exclude("**/build/**") }
