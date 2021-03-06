package hm.binkley.layers

import org.eclipse.jgit.api.Git
import java.util.Objects.hash
import java.util.TreeMap
import javax.script.ScriptEngine

class PersistedLayer(
    private val layers: PersistedLayers,
    override val slot: Int,
    private val contents: MutableMap<String, Value<*>> = TreeMap()
) : Map<String, Value<*>> by contents,
    Layer {
    private val _meta: MutableMap<String, String> = mutableMapOf()
    private val included = mutableListOf<String>()

    fun toSourceCode() = map {
        "this[\"${it.key}\"] = ${it.value.toSourceCode()}"
    }.joinToString("\n")

    override val script: String
        get() = included.joinToString("\n\n")
    override val enabled = true
    override val meta: Map<String, String>
        get() = _meta

    override fun toDiff() = contents.entries.joinToString("\n") {
        val (key, value) = it
        "$key: ${value.toDiff()}"
    }

    override fun edit(block: MutableLayer.() -> Unit): Layer = apply {
        PersistedMutableLayer(this, _meta, contents).block()
    }

    override fun commit(description: String, notes: String?): Layer {
        val cleanDescription = description.clean()
        val cleanScript = script.clean()
        val cleanNotes = notes?.clean()

        save(cleanDescription, cleanScript, cleanNotes)

        return layers.newLayer()
    }

    /** @todo Confusing the relation of [commit] to [save] */
    internal fun save(
        cleanDescription: String,
        cleanScript: String,
        cleanNotes: String?
    ): String {
        fun Git.write(ext: String, contents: String) {
            val fileName = "$slot.$ext"
            val scriptFile = layers.scriptFile(fileName)
            scriptFile.writeText(contents)
            if (contents.isNotEmpty()) scriptFile.appendText("\n")
            add().addFilepattern(fileName).call()
        }

        return layers.letGit { git ->
            git.write("kts", cleanScript)
            val diff = toDiff()
            if (diff.isNotEmpty()) git.write("txt", diff)
            cleanNotes?.also {
                git.write("notes", it)
            }

            val commit = git.commit()
            commit.message = cleanDescription
            commit.call()

            git.push().call()

            "$slot.kts"
        }
    }

    override fun equals(other: Any?) = this === other
            || other is PersistedLayer
            && slot == other.slot
            && script == other.script
            && contents == other.contents
            && enabled == other.enabled

    override fun hashCode() = hash(slot, script, contents, enabled)

    override fun toString() =
        "${this::class.simpleName}#$slot:$contents\\$meta[${if (enabled) "enabled" else "disabled"}]"

    internal fun <R> letEngine(block: (ScriptEngine) -> R): R =
        layers.letEngine(block)

    internal fun include(script: String) = included.add(script.clean())
}
