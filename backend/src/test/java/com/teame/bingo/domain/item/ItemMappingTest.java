package com.teame.bingo.domain.item;

import static org.assertj.core.api.Assertions.assertThat;

import com.teame.bingo.domain.activity.entity.SavedGuide;
import com.teame.bingo.domain.activity.entity.SearchHistory;
import com.teame.bingo.domain.activity.enums.SearchType;
import com.teame.bingo.domain.district.entity.District;
import com.teame.bingo.domain.district.entity.DistrictRule;
import com.teame.bingo.domain.district.entity.DistrictRuleArea;
import com.teame.bingo.domain.district.enums.AreaScope;
import com.teame.bingo.domain.item.entity.Category;
import com.teame.bingo.domain.item.entity.DisposalType;
import com.teame.bingo.domain.item.entity.Item;
import com.teame.bingo.domain.item.enums.Recyclable;
import com.teame.bingo.domain.member.entity.Member;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ItemMappingTest {

    @Autowired
    TestEntityManager tem;

    @Autowired
    EntityManager em;

    @Test
    void savesAndLoadsItemWithAssociations() {
        Category category = tem.persist(Category.builder()
                .code("PL").name("Plastic").sortOrder(1)
                .build());
        DisposalType type = tem.persist(DisposalType.builder()
                .code("RECYCLABLE").name("Recyclable Waste")
                .build());
        Item item = tem.persist(Item.builder()
                .itemKey("pet_bottle").category(category).disposalType(type)
                .name("PET Bottle").nameKo("pet-ko").recyclable(Recyclable.CONDITIONAL)
                .statusNote("if clean").caution("Remove the label.").clipPrompt("a photo of a pet bottle")
                .sourceName("Source").sourceUrl("https://example.com").verifiedAt(LocalDate.of(2026, 11, 19))
                .build());
        tem.flush();
        tem.clear();

        Item found = em.find(Item.class, item.getId());

        assertThat(found.getItemKey()).isEqualTo("pet_bottle");
        assertThat(found.getRecyclable()).isEqualTo(Recyclable.CONDITIONAL);
        assertThat(found.getCategory().getCode()).isEqualTo("PL");
        assertThat(found.getDisposalType().getCode()).isEqualTo("RECYCLABLE");
    }

    @Test
    void savesRuleAreaMemberAndHistory() {
        District district = tem.persist(District.builder().code("11110").nameEn("Jongno-gu").nameKo("종로구").build());
        DisposalType type = tem.persist(DisposalType.builder().code("FOOD").name("Food Waste").build());
        DistrictRule rule = tem.persist(DistrictRule.builder().district(district).disposalType(type)
                .disposalDay("Mon-Sat").disposalTime("19:00~24:00").build());
        DistrictRuleArea area = tem.persist(DistrictRuleArea.builder().districtRule(rule).disposalDay("Mon").build());
        Category category = tem.persist(Category.builder().code("PL").name("Plastic").build());
        Item item = tem.persist(Item.builder().itemKey("cup").category(category).disposalType(type)
                .name("Cup").recyclable(Recyclable.RECYCLABLE).clipPrompt("a photo of a cup")
                .sourceName("Source").sourceUrl("https://example.com").verifiedAt(LocalDate.of(2026, 11, 19))
                .build());
        Member member = tem.persist(Member.builder().id(UUID.randomUUID()).district(district).build());
        tem.persist(SavedGuide.builder().member(member).item(item).build());
        tem.persist(SearchHistory.builder().member(member).type(SearchType.TEXT).keyword("cup").build());
        tem.flush();
        tem.clear();

        assertThat(em.find(DistrictRule.class, rule.getId()).getAreaScope()).isEqualTo(AreaScope.DISTRICT);
        assertThat(em.find(DistrictRuleArea.class, area.getId()).getDisposalDay()).isEqualTo("Mon");
        assertThat(em.find(District.class, district.getId()).getCity()).isEqualTo("Seoul");
        assertThat(em.find(District.class, district.getId()).getUpdatedAt()).isNotNull();
    }
}
