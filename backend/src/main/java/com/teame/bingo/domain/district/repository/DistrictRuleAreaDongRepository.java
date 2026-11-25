package com.teame.bingo.domain.district.repository;

import java.util.Collection;
import java.util.List;

import com.teame.bingo.domain.district.entity.DistrictRuleAreaDong;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRuleAreaDongRepository extends JpaRepository<DistrictRuleAreaDong, Long> {

    List<DistrictRuleAreaDong> findAllByDistrictRuleAreaIdInOrderByNameEnAscIdAsc(Collection<Long> areaIds);
}
