
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