package com.teame.bingo.domain.item.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.domain.item.entity.DisposalType;
import com.teame.bingo.domain.item.entity.Item;
import com.teame.bingo.domain.item.enums.Recyclable;
import com.teame.bingo.support.RepositoryTest;

@RepositoryTest
class ItemRepositoryTest {

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void ordersByNameIgnoringCase() {
        Category category = tem.persist(Category.builder().code("PL").name("Plastic").build());
        DisposalType type = tem.persist(DisposalType.builder().code("RECYCLABLE").name("Recyclable Waste").build());
        persistItem(category, type, "banana");
        persistItem(category, type, "Zebra mug");
        persistItem(category, type, "cherry");
        persistItem(category, type, "Apple");
        persistItem(category, type, "apple pie");
        tem.flush();
        tem.clear();

        List<Item> result = itemRepository.findAllByCategoryIdOrderByName(category.getId());

        assertThat(result).extracting(Item::getName)
                .containsExactly("Apple", "apple pie", "banana", "cherry", "Zebra mug");
    }

    @Test
    void ordersByIdWhenNamesMatchIgnoringCase() {
        Category category = tem.persist(Category.builder().code("PL").name("Plastic").build());
        DisposalType type = tem.persist(DisposalType.builder().code("RECYCLABLE").name("Recyclable Waste").build());
        Item first = persistItem(category, type, "cup");
        Item second = persistItem(category, type, "Cup");
        tem.flush();
        tem.clear();

        List<Item> result = itemRepository.findAllByCategoryIdOrderByName(category.getId());

        assertThat(result).extracting(Item::getId).containsExactly(first.getId(), second.getId());
    }

    @Test
    void returnsOnlyItemsOfRequestedCategory() {
        Category plastic = tem.persist(Category.builder().code("PL").name("Plastic").build());
        Category glass = tem.persist(Category.builder().code("GL").name("Glass").build());
        DisposalType type = tem.persist(DisposalType.builder().code("RECYCLABLE").name("Recyclable Waste").build());
        persistItem(plastic, type, "Bottle");
        persistItem(glass, type, "Jar");
        tem.flush();
        tem.clear();

        List<Item> result = itemRepository.findAllByCategoryIdOrderByName(plastic.getId());

        assertThat(result).extracting(Item::getName).containsExactly("Bottle");
    }

    @Test
    void fetchesCategoryEagerly() {
        Category category = tem.persist(Category.builder().code("PL").name("Plastic").build());
        DisposalType type = tem.persist(DisposalType.builder().code("RECYCLABLE").name("Recyclable Waste").build());
        persistItem(category, type, "Bottle");
        tem.flush();
        tem.clear();

        List<Item> result = itemRepository.findAllByCategoryIdOrderByName(category.getId());

        assertThat(result).hasSize(1);
        assertThat(Hibernate.isInitialized(result.get(0).getCategory())).isTrue();
    }

    @Test
    void returnsEmptyListWhenCategoryHasNoItems() {
        Category category = tem.persist(Category.builder().code("PL").name("Plastic").build());
        tem.flush();
        tem.clear();

        assertThat(itemRepository.findAllByCategoryIdOrderByName(category.getId())).isEmpty();
    }

    @Test
    void sortsByCategorySortOrderThenNameIgnoringCase() {
        Category glass = tem.persist(Category.builder().code("GL").name("Glass").sortOrder(2).build());
        Category plastic = tem.persist(Category.builder().code("PL").name("Plastic").sortOrder(1).build());
        DisposalType type = tem.persist(DisposalType.builder().code("RECYCLABLE").name("Recyclable Waste").build());
        persistItem(glass, type, "jar");
        persistItem(plastic, type, "bottle");
        persistItem(glass, type, "Bottle");
        persistItem(plastic, type, "Zip bag");
        persistItem(plastic, type, "Apple tray");
        tem.flush();
        tem.clear();

        List<Item> result = itemRepository.findAllOrderByCategoryAndName();

        assertThat(result).extracting(Item::getName)
                .containsExactly("Apple tray", "bottle", "Zip bag", "Bottle", "jar");
    }

    @Test
    void groupsItemsByCategoryIdWhenSortOrderMatches() {
        Category first = tem.persist(Category.builder().code("AA").name("Alpha group").sortOrder(1).build());
        Category second = tem.persist(Category.builder().code("BB").name("Beta group").sortOrder(1).build());
        DisposalType type = tem.persist(DisposalType.builder().code("RECYCLABLE").name("Recyclable Waste").build());
        persistItem(second, type, "beta");
        persistItem(first, type, "gamma");
        persistItem(first, type, "alpha");
        tem.flush();
        tem.clear();

        List<Item> result = itemRepository.findAllOrderByCategoryAndName();

        assertThat(result).extracting(Item::getName).containsExactly("alpha", "gamma", "beta");
    }

    @Test
    void fetchesCategoryForAllItems() {
        Category category = tem.persist(Category.builder().code("PL").name("Plastic").build());
        DisposalType type = tem.persist(DisposalType.builder().code("RECYCLABLE").name("Recyclable Waste").build());
        persistItem(category, type, "Bottle");
        tem.flush();
        tem.clear();

        List<Item> result = itemRepository.findAllOrderByCategoryAndName();

        assertThat(result).hasSize(1);
        assertThat(Hibernate.isInitialized(result.get(0).getCategory())).isTrue();
    }

    @Test
    void returnsEmptyListWhenNoItemsExist() {
        assertThat(itemRepository.findAllOrderByCategoryAndName()).isEmpty();
    }

    @Test
    void findsItemWithCategoryInitialized() {
        Category category = tem.persist(Category.builder().code("PL").name("Plastic").build());
        DisposalType type = tem.persist(DisposalType.builder().code("RECYCLABLE").name("Recyclable Waste").build());
        Item saved = persistItem(category, type, "Bottle");
        tem.flush();
        tem.clear();

        Optional<Item> result = itemRepository.findWithCategoryById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Bottle");
        assertThat(Hibernate.isInitialized(result.get().getCategory())).isTrue();
    }

    @Test
    void returnsEmptyOptionalForUnknownItemId() {
        assertThat(itemRepository.findWithCategoryById(999999L)).isEmpty();
    }

    private Item persistItem(Category category, DisposalType type, String name) {
        return tem.persist(Item.builder()
                .itemKey("key_" + name).category(category).disposalType(type)
                .name(name).recyclable(Recyclable.RECYCLABLE).clipPrompt("a photo of " + name)
                .sourceName("Source").sourceUrl("https://example.com").verifiedAt(LocalDate.of(2026, 11, 19))
                .build());
    }
}
