package com.teame.bingo.domain.district.repository;

import java.util.List;

import com.teame.bingo.domain.district.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<District, Long> {

    List<District> findAllByOrderByNameEnAsc();
}
