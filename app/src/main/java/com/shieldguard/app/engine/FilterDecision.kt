package com.shieldguard.app.engine

sealed class FilterDecision {
    object Allow : FilterDecision()
    data class Block(val reason: String) : FilterDecision()
    data class Silence(val reason: String) : FilterDecision()
}
