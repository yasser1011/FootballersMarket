package com.yasser.footballersmarket.worldcup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorldCupFixtureRepository extends JpaRepository<WorldCupFixture, Long> {
}
