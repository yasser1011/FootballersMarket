package com.yasser.footballersmarket.featuredplayer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
public class FeaturedPlayerRepositoryTest {
    @Autowired
    private FeaturedPlayerRepository repository;


    @Test
    public void shouldThrowDataIntegrityExceptionGivenTwoRowsWithSamePositionAndDate(){

        FeaturedPlayer entity1 = new FeaturedPlayer("test name", 1,
                "5.5", 1, 1, "home team", "away team");
        FeaturedPlayer entity2 = new FeaturedPlayer("test name", 2,
                "5.5", 1, 1, "home team", "away team");



        assertThatThrownBy(() -> repository.saveAllAndFlush(List.of(entity1, entity2)))
                .isInstanceOf(DataIntegrityViolationException.class);

    }
}
