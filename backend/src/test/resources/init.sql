
CREATE FUNCTION fn_get_price(age integer, rating numeric, goals integer, assists integer) RETURNS numeric AS $$

	declare offsetValue integer := 100;
				ageW numeric := 0.35;
				ratingW numeric := 0.65;
				goalsW numeric := 0.2;
				assistsW numeric := 0.1;

				v numeric;
				ratingV numeric;
				goalsV numeric;
				assistsV numeric;
				v1 numeric;

	begin

		if (age < 23 or age > 33) then
			ageW := 0.55;
			ratingW := 0.45;

		end if;

		v = round(POWER(cast(1 as numeric) / age, 3) * ageW * 100000 * offsetValue, 2);
		ratingV = round(POWER(rating, 1.1) * ratingW * offsetValue, 2);
		goalsV = round(POWER(goals, 0.8) * goalsW * offsetValue, 2);
		assistsV = round(POWER(assists, 0.8) * assistsW * offsetValue, 2);

		v1 = v + ratingV;
		return round(v1);

	end;

$$ LANGUAGE plpgsql;

-- world cup pricing, mirror of WorldCupPriceCalculator.java / WorldCupConstants.java.
-- kept in sync by FnGetWcPriceParityTest
CREATE OR REPLACE FUNCTION fn_get_wc_price(base_rating numeric, wc_rating numeric, games_played integer)
RETURNS numeric AS $$
declare
	base integer;
	delta numeric;
begin
	if base_rating is null then base := 300;
	elsif base_rating >= 7.2 then base := 600;
	elsif base_rating >= 6.8 then base := 450;
	else base := 300;
	end if;

	-- form bonus: clamp(wcRating - 6.5, -1.5, +3.0) * 300 ; games bonus: gamesPlayed * 200
	delta := greatest(-1.5, least(3.0, coalesce(wc_rating, 6.5) - 6.5));

	return greatest(100, round(base + delta * 300 + coalesce(games_played, 0) * 200));
end;
$$ LANGUAGE plpgsql IMMUTABLE;