import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class GitOperations : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("git", GitExtension::class.java, project)
    }
}

open class GitExtension(private val project: Project) {
    /**
     * Get the current git revision (short hash)
     * @param submodulePath Optional path to a submodule relative to the project root
     */
    fun getRevision(submodulePath: String? = null): Provider<String> {
        return project.provider {
            executeGitCommand(submodulePath, "rev-parse", "--short", "HEAD")
        }
    }

    /**
     * Get the current git revision (full hash)
     * @param submodulePath Optional path to a submodule relative to the project root
     */
    fun getFullRevision(submodulePath: String? = null): Provider<String> {
        return project.provider {
            executeGitCommand(submodulePath, "rev-parse", "HEAD")
        }
    }

    /**
     * Get the current git branch name
     * @param submodulePath Optional path to a submodule relative to the project root
     */
    fun getBranch(submodulePath: String? = null): Provider<String> {
        return project.provider {
            executeGitCommand(submodulePath, "rev-parse", "--abbrev-ref", "HEAD")
        }
    }

    /**
     * Check if the working directory is clean (no uncommitted changes)
     * @param submodulePath Optional path to a submodule relative to the project root
     */
    fun isClean(submodulePath: String? = null): Provider<Boolean> {
        return project.provider {
            val output = executeGitCommand(submodulePath, "status", "--porcelain")
            output.isEmpty()
        }
    }

    /**
     * Get the latest git tag
     * @param submodulePath Optional path to a submodule relative to the project root
     */
    fun getLatestTag(submodulePath: String? = null): Provider<String> {
        return project.provider {
            executeGitCommand(submodulePath, "describe", "--tags", "--abbrev=0")
        }
    }

    /**
     * Get a descriptive version string (e.g., "v1.0.0-5-g1a2b3c4")
     * @param submodulePath Optional path to a submodule relative to the project root
     */
    fun getDescribe(submodulePath: String? = null): Provider<String> {
        return project.provider {
            executeGitCommand(submodulePath, "describe", "--tags", "--always", "--dirty")
        }
    }

    private fun executeGitCommand(submodulePath: String?, vararg args: String): String {
        return try {
            val execOutput = project.providers.exec {
                if (submodulePath != null) {
                    commandLine("git", "-C", submodulePath, *args)
                } else {
                    commandLine("git", *args)
                }
                isIgnoreExitValue = true
            }

            val output = execOutput.standardOutput.asText.get().trim()
            if (execOutput.result.get().exitValue == 0) {
                output
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            project.logger.warn("Failed to execute git command: ${e.message}")
            "unknown"
        }
    }
}
