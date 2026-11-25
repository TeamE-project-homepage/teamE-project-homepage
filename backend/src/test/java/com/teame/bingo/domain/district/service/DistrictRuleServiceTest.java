package com.teame.bingo.domain.district.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.teame.bingo.domain.district.dto.DistrictRuleAreaResponse;
import com.teame.bingo.domain.district.dto.DistrictRuleDongResponse;
import com.teame.bingo.domain.district.dto.DistrictRuleResponse;
import com.teame.bingo.domain.district.entity.District;
import com.teame.bingo.domain.district.entity.DistrictRule;
import com.teame.bingo.domain.district.entity.DistrictRuleArea;
import com.teame.bingo.domain.district.entity.DistrictRuleAreaDong;
import com.teame.bingo.domain.district.enums.AreaScope;
import com.teame.bingo.domain.district.enums.DistrictRuleStatus;
import com.teame.bingo.domain.item.entity.DisposalType;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import com.teame.bingo.support.RepositoryTest;

@RepositoryTest
@Import(DistrictRuleService.class)
class DistrictRuleServiceTest {

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private DistrictRuleService districtRuleService;

    @Test
    void returnsDistrictRuleWithSourceAndNoAreas() {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("RECYCLABLE");
        DistrictRule rule = tem.persist(rule(district, type, AreaScope.DISTRICT).build());
        area(rule, 1, "Tue, Fri", "19:00~24:00");
        flushAndClear();

        DistrictRuleResponse result = districtRuleService.getDistrictRule(district.getId(), type.getId());

        assertThat(result.districtId()).isEqualTo(district.getId());
        assertThat(result.districtName()).isEqualTo("Mapo-gu");
        assertThat(result.disposalTypeId()).isEqualTo(type.getId());
        assertThat(result.status()).isEqualTo(DistrictRuleStatus.AVAILABLE);
        assertThat(result.areaScope()).isEqualTo(AreaScope.DISTRICT);
        assertThat(result.method()).isEqualTo("Transparent bag");
        assertThat(result.day()).isEqualTo("Mon, Thu");
        assertThat(result.time()).isEqualTo("18:00~24:00");
        assertThat(result.localNotes()).isEqualTo("Put PET bottles in a separate bag.");
        assertThat(result.sourceName()).isEqualTo("District Office");
        assertThat(result.sourceUrl()).isEqualTo("https://example.com/rules");
        assertThat(result.verifiedAt()).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(result.areas()).isEmpty();
    }

    @Test
    void returnsSortedDongAreasWithAreaTimeOverrideAndDistrictFallback() {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("RECYCLABLE");
        DistrictRule rule = tem.persist(rule(district, type, AreaScope.DONG).build());
        DistrictRuleArea second = area(rule, 2, "Tue, Fri", "19:00~23:00");
        DistrictRuleArea first = area(rule, 1, "Mon, Thu", null);
        dong(first, "Gongdeok-dong", "공덕동");
        dong(first, "Ahyeon-dong", "아현동");
        dong(second, "Seogyo-dong", "서교동");

        DistrictRule foreignRule = tem.persist(rule(
                district("11110", "Jongno-gu"), type, AreaScope.DONG).build());
        DistrictRuleArea foreignArea = area(foreignRule, 1, "Every day", null);
        dong(foreignArea, "Foreign-dong", "다른동");
        DistrictRule otherTypeRule = tem.persist(rule(district, type("GENERAL"), AreaScope.DONG).build());
        dong(area(otherTypeRule, 1, "Sun, Wed", null), "Other-type-dong", "다른유형동");
        flushAndClear();

        DistrictRuleResponse result = districtRuleService.getDistrictRule(district.getId(), type.getId());

        assertThat(result.status()).isEqualTo(DistrictRuleStatus.AVAILABLE);
        assertThat(result.day()).isNull();
        assertThat(result.areas()).extracting(DistrictRuleAreaResponse::areaId)
                .containsExactly(first.getId(), second.getId());
        assertThat(result.areas().getFirst().day()).isEqualTo("Mon, Thu");
        assertThat(result.areas().getFirst().time()).isEqualTo("18:00~24:00");
        assertThat(result.areas().getLast().time()).isEqualTo("19:00~23:00");
        assertThat(result.areas().getFirst().dongs()).containsExactly(
                new DistrictRuleDongResponse("Ahyeon-dong", "아현동"),
                new DistrictRuleDongResponse("Gongdeok-dong", "공덕동"));
        assertThat(result.areas().getLast().dongs()).containsExactly(
                new DistrictRuleDongResponse("Seogyo-dong", "서교동"));
    }

    @Test
    void dongScheduleCanBeCompleteWithoutDistrictTime() {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("RECYCLABLE");
        DistrictRule rule = tem.persist(rule(district, type, AreaScope.DONG).disposalTime(null).build());
        dong(area(rule, 1, "Mon, Thu", "19:00~24:00"), "Gongdeok-dong", "공덕동");
        flushAndClear();

        DistrictRuleResponse result = districtRuleService.getDistrictRule(district.getId(), type.getId());

        assertThat(result.status()).isEqualTo(DistrictRuleStatus.AVAILABLE);
        assertThat(result.time()).isNull();
        assertThat(result.areas().getFirst().time()).isEqualTo("19:00~24:00");
    }

    @Test
    void missingBothTimesProducesPartialAndNoInventedTime() {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("RECYCLABLE");
        DistrictRule rule = tem.persist(rule(district, type, AreaScope.DONG).disposalTime(null).build());
        dong(area(rule, 1, "Mon, Thu", null), "Gongdeok-dong", "공덕동");
        flushAndClear();

        DistrictRuleResponse result = districtRuleService.getDistrictRule(district.getId(), type.getId());

        assertThat(result.status()).isEqualTo(DistrictRuleStatus.PARTIAL);
        assertThat(result.time()).isNull();
        assertThat(result.areas().getFirst().time()).isNull();
    }

    @Test
    void blankAreaTimeFallsBackToDistrictTime() {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("RECYCLABLE");
        DistrictRule rule = tem.persist(rule(district, type, AreaScope.DONG).build());
        dong(area(rule, 1, "Mon, Thu", " \t "), "Gongdeok-dong", "공덕동");
        flushAndClear();

        DistrictRuleResponse result = districtRuleService.getDistrictRule(district.getId(), type.getId());

        assertThat(result.status()).isEqualTo(DistrictRuleStatus.AVAILABLE);
        assertThat(result.areas().getFirst().time()).isEqualTo("18:00~24:00");
    }

    @Test
    void dongScopeWithoutAreasIsPartial() {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("RECYCLABLE");
        tem.persist(rule(district, type, AreaScope.DONG).build());
        flushAndClear();

        DistrictRuleResponse result = districtRuleService.getDistrictRule(district.getId(), type.getId());

        assertThat(result.status()).isEqualTo(DistrictRuleStatus.PARTIAL);
        assertThat(result.areas()).isEmpty();
    }

    @Test
    void dongAreaWithoutDongsIsPartial() {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("RECYCLABLE");
        DistrictRule rule = tem.persist(rule(district, type, AreaScope.DONG).build());
        area(rule, 1, "Mon, Thu", null);
        flushAndClear();

        DistrictRuleResponse result = districtRuleService.getDistrictRule(district.getId(), type.getId());

        assertThat(result.status()).isEqualTo(DistrictRuleStatus.PARTIAL);
        assertThat(result.areas().getFirst().dongs()).isEmpty();
    }

    @Test
    void addressScopeUsesOfficialLookupWithoutReturningDistrictDayOrDongAreas() {
        District district = district("11110", "Jongno-gu");
        DisposalType type = type("GENERAL");
        DistrictRule rule = tem.persist(rule(district, type, AreaScope.ADDRESS)
                .sourceUrl("https://example.com/address-search").build());
        dong(area(rule, 1, "Tue, Fri", null), "Not-applicable-dong", "미적용동");
        flushAndClear();

        DistrictRuleResponse result = districtRuleService.getDistrictRule(district.getId(), type.getId());

        assertThat(result.status()).isEqualTo(DistrictRuleStatus.AVAILABLE);
        assertThat(result.areaScope()).isEqualTo(AreaScope.ADDRESS);
        assertThat(result.day()).isNull();
        assertThat(result.sourceUrl()).isEqualTo("https://example.com/address-search");
        assertThat(result.areas()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"method", "localNotes", "sourceName", "sourceUrl", "verifiedAt", "day", "time"})
    void missingApplicableFieldProducesPartial(String missingField) {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("RECYCLABLE");
        DistrictRule.DistrictRuleBuilder builder = rule(district, type, AreaScope.DISTRICT);
        switch (missingField) {
            case "method" -> builder.method(null);
            case "localNotes" -> builder.localNotes(null);
            case "sourceName" -> builder.sourceName(null);
            case "sourceUrl" -> builder.sourceUrl(null);
            case "verifiedAt" -> builder.verifiedAt(null);
            case "day" -> builder.disposalDay(null);
            case "time" -> builder.disposalTime(null);
            default -> throw new IllegalArgumentException(missingField);
        }
        tem.persist(builder.build());
        flushAndClear();

        assertThat(districtRuleService.getDistrictRule(district.getId(), type.getId()).status())
                .isEqualTo(DistrictRuleStatus.PARTIAL);
    }

    @Test
    void blankFieldsAreMissingAndDoNotProduceAvailableStatus() {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("RECYCLABLE");
        tem.persist(rule(district, type, AreaScope.DISTRICT).method(" ").sourceUrl("\t").build());
        flushAndClear();

        DistrictRuleResponse result = districtRuleService.getDistrictRule(district.getId(), type.getId());

        assertThat(result.status()).isEqualTo(DistrictRuleStatus.PARTIAL);
        assertThat(result.method()).isNull();
        assertThat(result.sourceUrl()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECYCLABLE", "GENERAL", "FOOD", "BULKY", "NON_COMBUSTIBLE"})
    void allFiveCurrentTypesWithoutRulesReturnNoInfo(String code) {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type(code);
        flushAndClear();

        assertEmptyStatus(districtRuleService.getDistrictRule(district.getId(), type.getId()),
                district, type, DistrictRuleStatus.NO_INFO);
    }

    @Test
    void noInfoDoesNotUseRuleForAnotherDistrictOrDisposalType() {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("RECYCLABLE");
        tem.persist(rule(district("11110", "Jongno-gu"), type, AreaScope.DISTRICT).build());
        tem.persist(rule(district, type("GENERAL"), AreaScope.DISTRICT).build());
        flushAndClear();

        assertEmptyStatus(districtRuleService.getDistrictRule(district.getId(), type.getId()),
                district, type, DistrictRuleStatus.NO_INFO);
    }

    @Test
    void registeredTypeOutsideDistrictCollectionReturnsUnsupported() {
        District district = district("11440", "Mapo-gu");
        DisposalType type = type("SEPARATE_COLLECTION");
        flushAndClear();

        assertEmptyStatus(districtRuleService.getDistrictRule(district.getId(), type.getId()),
                district, type, DistrictRuleStatus.UNSUPPORTED);
    }

    @Test
    void missingDistrictReturnsDistrict404() {
        assertThatThrownBy(() -> districtRuleService.getDistrictRule(Long.MAX_VALUE, 1L))
                .isInstanceOfSatisfying(CustomException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.DISTRICT_NOT_FOUND));
    }

    @Test
    void unregisteredDisposalTypeIsInvalidRatherThanUnsupported() {
        District district = district("11440", "Mapo-gu");
        flushAndClear();

        assertThatThrownBy(() -> districtRuleService.getDistrictRule(district.getId(), Long.MAX_VALUE))
                .isInstanceOfSatisfying(CustomException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private void assertEmptyStatus(
            DistrictRuleResponse response, District district, DisposalType type, DistrictRuleStatus status) {
        assertThat(response).isEqualTo(new DistrictRuleResponse(
                district.getId(), district.getNameEn(), type.getId(), status,
                null, null, null, null, null, null, null, null, List.of()));
    }

    private District district(String code, String nameEn) {
        return tem.persist(District.builder().code(code).nameEn(nameEn).nameKo("자치구").build());
    }

    private DisposalType type(String code) {
        return tem.persist(DisposalType.builder().code(code).name(code).build());
    }

    private DistrictRule.DistrictRuleBuilder rule(District district, DisposalType type, AreaScope scope) {
        return DistrictRule.builder().district(district).disposalType(type).areaScope(scope)
                .method("Transparent bag").disposalDay("Mon, Thu").disposalTime("18:00~24:00")
                .localNotes("Put PET bottles in a separate bag.").sourceName("District Office")
                .sourceUrl("https://example.com/rules").verifiedAt(LocalDate.of(2026, 10, 30));
    }

    private DistrictRuleArea area(DistrictRule rule, int sortOrder, String day, String time) {
        return tem.persist(DistrictRuleArea.builder().districtRule(rule).sortOrder(sortOrder)
                .disposalDay(day).disposalTime(time).build());
    }

    private void dong(DistrictRuleArea area, String nameEn, String nameKo) {
        tem.persist(DistrictRuleAreaDong.builder().districtRuleArea(area).nameEn(nameEn).nameKo(nameKo).build());
    }

    private void flushAndClear() {
        tem.flush();
        tem.clear();
    }
}
