package com.gimenes.alex.rickandmorty.feature.quiz.di

import com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateTriviaQuestionUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.GetCharacterPoolUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.RecordQuizResultUseCase
import com.gimenes.alex.rickandmorty.feature.quiz.QuizViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * DI wiring for `feature:quiz` (issue #15).
 *
 * This is the first module in the project that actually wires `core:domain` use cases into Koin -
 * issues #10/#11/#13 landed [GetCharacterPoolUseCase]/[GenerateTriviaQuestionUseCase]/
 * [RecordQuizResultUseCase] themselves but nothing consumed them yet, so no prior module had a
 * reason to bind them. They're registered here as `single`s (matching how `core:data`'s
 * `dataModule` binds its repositories) rather than `factory`s: each is a small, stateless wrapper
 * around a repository call / pure function - there's no per-use benefit to a fresh instance per
 * injection site, and a shared singleton avoids needless allocation on every quiz replay.
 *
 * [GetCharacterPoolUseCase] and [RecordQuizResultUseCase] resolve their `CharacterRepository`/
 * `GameStateRepository` dependencies from `core:data`'s `dataModule`, which - like `core:database`'s
 * `databaseModule` and `core:network`'s `networkModule` - must already be included alongside this
 * module (see `app`'s `sharedAppModules`); [GenerateTriviaQuestionUseCase] has no dependencies of
 * its own.
 */
val quizModule = module {
    single { GetCharacterPoolUseCase(get()) }
    single { GenerateTriviaQuestionUseCase() }
    single { RecordQuizResultUseCase(get()) }
    viewModel { QuizViewModel(get(), get(), get()) }
}
