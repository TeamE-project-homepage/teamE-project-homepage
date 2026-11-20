package com.teame.bingo.domain.district.entity;

import com.teame.bingo.domain.district.enums.AreaScope;
import com.teame.bingo.domain.item.entity.DisposalType;
import com.teame.bingo.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "district_rule", uniqueConstraints = @UniqueConstraint(columnNames = {"district_id", "disposal_type_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DistrictRule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disposal_type_id", nullable = false)
    private DisposalType disposalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_scope", nullable = false, length = 10)
    @Builder.Default
    private AreaScope areaScope = AreaScope.DISTRICT;

    @Column(columnDefinition = "text")
    private String method;

    @Column(name = "`day`", length = 100)
    private String disposalDay;

    @Column(name = "`time`", length = 200)
    private String disposalTime;

    @Column(name = "local_notes", columnDefinition = "text")
    private String localNotes;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "verified_at")
    private LocalDate verifiedAt;
}
