package com.teame.bingo.domain.district.entity;

import com.teame.bingo.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "district")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class District extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "district_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 5, columnDefinition = "char(5)")
    private String code;

    @Column(name = "name_en", nullable = false, unique = true, length = 50)
    private String nameEn;

    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String city = "Seoul";
}
