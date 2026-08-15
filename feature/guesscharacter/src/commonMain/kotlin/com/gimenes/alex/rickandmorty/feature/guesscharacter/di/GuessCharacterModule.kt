package com.gimenes.alex.rickandmorty.feature.guesscharacter.di

import com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateGuessCharacterRoundUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.GetCharacterPoolUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.RecordGuessOutcomeUseCase
import com.gimenes.alex.rickandmorty.feature.guesscharacter.GuessCharacterViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * DI wiring for `feature:guesscharacter` (issue #16), mirroring `feature:quiz`'s
 * [com.gimenes.alex.rickandmorty.feature.quiz.di.quizModule] (issue #15) - see that module's kdoc
 * for the reasoning behind `single` over `factory` for these use-case wrappers.
 *
 * ### Why [GetCharacterPoolUseCase] is declared here too
 * [quizModule] already binds a `single { GetCharacterPoolUseCase(get()) }`, and today both modules
 * are always loaded together (see `app`'s `sharedAppModules`), so relying on that one binding would
 * technically work. It's deliberately re-declared here instead: `feature:guesscharacter` has no
 * Gradle dependency on `feature:quiz` and shouldn't have an *implicit* one via Koin resolution
 * order/co-loading either - each feature module's DI graph should be self-sufficient on its own
 * merits. Koin's default `single { }` behavior allows the later-loaded definition to override the
 * earlier one silently; since both definitions are identical (same class, same `CharacterRepository`
 * dependency resolved via `get()`), the override is a functional no-op either way this module is
 * ordered relative to `quizModule`.
 *
 * This duplication is a known, minor DI wart - `GetCharacterPoolUseCase` has no feature-specific
 * dependency (only `core:domain`'s `CharacterRepository`, from `core:data`'s `dataModule`) and would
 * be better centralized in a shared module (e.g. `dataModule` itself, alongside the repositories it
 * wraps) so neither feature module needs to declare it at all. Left as-is here rather than editing
 * `feature:quiz`/`core:data` for an unrelated feature's task; worth a follow-up for ATLAS.
 */
val guessCharacterModule = module {
    single { GetCharacterPoolUseCase(get()) }
    single { GenerateGuessCharacterRoundUseCase() }
    single { RecordGuessOutcomeUseCase(get()) }
    viewModel { GuessCharacterViewModel(get(), get(), get(), get()) }
}
