import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.SpdxLicenseBundleNormalizer
import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.render.ReportRenderer

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrainsKotlinJvm) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.versions)
    alias(libs.plugins.dependencyLicenseReport)
}

licenseReport {
    projects = arrayOf(project(":composeApp"))
    configurations = arrayOf("jvmRuntimeClasspath")
    outputDir = layout.buildDirectory.dir("reports/dependency-license").get().asFile.absolutePath
    filters = arrayOf<DependencyFilter>(
        SpdxLicenseBundleNormalizer(),
    )
    excludeBoms = true
    renderers = arrayOf<ReportRenderer>(
        JsonReportRenderer("third-party-libs.json"),
    )
}

fun isNonStable(version: String): Boolean {
    return "alpha" in version
            || "beta" in version
            || "Beta" in version
            || ".x-compat" in version
            || "rc" in version
            || "RC" in version
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
