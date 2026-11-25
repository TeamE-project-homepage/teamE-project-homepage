package com.teame.bingo.domain.district.repository;

import java.util.Optional;

import com.teame.bingo.domain.district.entity.DistrictRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRuleRepository extends JpaRepository<DistrictRule, Long> {

    Optional<DistrictRule> findByDistrictIdAndDisposalTypeId(Long districtId, Long disposalTypeId);
}
