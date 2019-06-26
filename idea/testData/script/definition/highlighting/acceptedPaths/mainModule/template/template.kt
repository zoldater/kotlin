package custom.scriptDefinition

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import java.io.File

@KotlinScript(fileExtension = "kts", compilationConfiguration = MyTestDefinition::class)
abstract class Template(val args: Array<String>)

object MyTestDefinition : ScriptCompilationConfiguration(
    {
        refineConfiguration {
            beforeCompiling(MyTestConfigurator())
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
            acceptedPaths(File("mainModule")) // TODO
        }
    })

class MyTestConfigurator : RefineScriptCompilationConfigurationHandler {
    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        return context.compilationConfiguration.asSuccess(
            listOf(
                ScriptDiagnostic("Warning", severity = ScriptDiagnostic.Severity.WARNING)
            )
        )
    }
}
