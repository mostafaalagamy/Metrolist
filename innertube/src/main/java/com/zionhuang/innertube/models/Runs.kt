package com.zionhuang.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Runs(
    val runs: List<Run>?,
)

@Serializable
data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint?,
)

fun List<Run>.splitBySeparator(): List<List<Run>> {
    val res = mutableListOf<List<Run>>()
    var tmp = mutableListOf<Run>()
    forEach { run ->
        if (run.text == " • ") {
            res.add(tmp)
            tmp = mutableListOf()
        } else {
            tmp.add(run)
        }
    }
    res.add(tmp)
    return res
}
fun List<List<Run>>.clean(): List<List<Run>> {
    val res = mutableListOf<List<Run>>()
    var l: List<List<Run>> = emptyList()
    this.getOrNull(0)?.getOrNull(0)?.navigationEndpoint
        ?.let {
            l = this
        }
        ?: run {
            l = this.drop(1)
        }
    l.forEach {
        res.add(it)
    }
    return res
}

fun List<Run>.oddElements() = filterIndexed { index, _ ->
    index % 2 == 0
}
