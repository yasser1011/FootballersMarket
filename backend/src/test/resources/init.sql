
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
CREATE OR REPLACE FUNCTION fn_get_wc_price(base_rating numeric, wc_rating numeric)
RETURNS numeric AS $$
declare
	base integer;
	multiplier numeric;
begin
	if base_rating is null then base := 250;
	elsif base_rating >= 7.2 then base := 1000;
	elsif base_rating >= 6.8 then base := 500;
	else base := 250;
	end if;

	multiplier := power(1.8, coalesce(wc_rating, 6.5) - 6.5);
	multiplier := greatest(0.5, least(4.0, multiplier));

	return round(base * multiplier);
end;
$$ LANGUAGE plpgsql IMMUTABLE;