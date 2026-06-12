package com.yasser.footballersmarket.pricing;

import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import com.yasser.footballersmarket.worldcup.WorldCupPriceCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// tripwire for the deliberate java <-> sql duplication of the world cup price formula:
// fn_get_wc_price (used by the ranking queries) must agree with WorldCupPriceCalculator
// for every input. editing one side without the other fails this test.
class FnGetWcPriceParityTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void sqlFunctionMatchesJavaCalculatorAcrossTheInputSpace() {
        Double[] baseRatings = {null, 5.5, 6.0, 6.5, 6.79, 6.80, 7.0, 7.19, 7.20, 7.5, 8.2};
        Double[] wcRatings = {null, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.9};

        for (Double base : baseRatings) {
            for (Double wc : wcRatings) {
                BigDecimal sqlPrice = jdbcTemplate.queryForObject(
                        "select fn_get_wc_price(cast(? as numeric), cast(? as numeric))",
                        BigDecimal.class, base, wc);
                assertThat(sqlPrice.intValue())
                        .as("base %s, wc %s", base, wc)
                        .isEqualTo(WorldCupPriceCalculator.currentPrice(base, wc));
            }
        }
    }
}
