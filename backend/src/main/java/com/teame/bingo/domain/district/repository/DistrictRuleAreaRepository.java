package com.teame.bingo.domain.district.repository;

import java.util.List;

import com.teame.bingo.domain.district.entity.DistrictRuleArea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRuleAreaRepository extends JpaRepository<DistrictRuleArea, Long> {

    List<DistrictRuleArea> findAllByDistrictRuleIdOrderBySortOrderAscIdAsc(Long ruleId);
}
