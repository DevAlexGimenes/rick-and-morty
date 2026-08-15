package com.gimenes.alex.rickandmorty.feature.quiz.di

import com.gimenes.alex.rickandmorty.feature.quiz.QuizViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val quizModule = module {
    viewModel { QuizViewModel() }
}
