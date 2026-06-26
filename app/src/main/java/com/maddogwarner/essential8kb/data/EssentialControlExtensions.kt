package com.maddogwarner.essential8kb.data

fun EssentialControl.allStepIds(): List<String> =
    listOf(ml1, ml2, ml3)
        .flatMap { content -> content.steps }
        .map { step -> step.id }
