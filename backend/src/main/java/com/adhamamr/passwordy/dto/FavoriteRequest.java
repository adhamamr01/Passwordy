package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for the favorite-toggle endpoint. {@code favorite} is boxed and {@code @NotNull} so
 * that an omitted field is rejected with 400 rather than silently defaulting to {@code false}.
 */
public record FavoriteRequest(@NotNull Boolean favorite) {
}
