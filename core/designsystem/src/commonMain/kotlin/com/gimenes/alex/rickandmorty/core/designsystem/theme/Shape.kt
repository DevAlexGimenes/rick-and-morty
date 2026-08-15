package com.gimenes.alex.rickandmorty.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Rounded / blobby "portal ring" corner language, used in place of Material's sharp default
 * corners across the app.
 */
val RickAndMortyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/** Fully round shape for portal-ring accents (badges, streak pips, avatar frames). */
val PortalRingShape = RoundedCornerShape(percent = 50)
