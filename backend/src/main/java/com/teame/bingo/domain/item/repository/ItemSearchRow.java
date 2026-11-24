package com.teame.bingo.domain.item.repository;

/** One row of {@link ItemRepository#search}. matchedKeyword is null when the item name matched. */
public interface ItemSearchRow {

    Long getItemId();

    String getName();

    Long getCategoryId();

    String getCategoryCode();

    String getCategoryName();

    String getMatchedKeyword();
}
