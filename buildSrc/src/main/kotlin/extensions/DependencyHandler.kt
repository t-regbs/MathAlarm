package extensions

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Adds the Compose dependencies on Gradle.
 */
fun DependencyHandler.addComposeDependencies() {
    implementation(Deps.compose.ui)
    implementation(Deps.compose.material)
    implementation(Deps.compose.icons)
    implementation(Deps.compose.tooling)
    implementation(Deps.compose.runtime)
    implementation(Deps.compose.runtimeSaveable)
    implementation(Deps.compose.runtimeLivedata)
}

private fun DependencyHandler.implementation(dependencyNotation: String): Dependency? =
    add("implementation", dependencyNotation)
