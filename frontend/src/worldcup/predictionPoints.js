// must mirror WorldCupConstants.EXACT_SCORE_BONUS on the backend
export const EXACT_SCORE_BONUS = 90;

// splits a settled prediction's total awardedPoints into its components:
//   result = the outcome (win/draw) points, exact = exact-score bonus, bonus = manual lever.
// awardedPoints already includes both exact and bonus, so result is the remainder.
// returns null when the prediction isn't settled yet (awardedPoints == null).
export const pointsBreakdown = (item) => {
  const total = item.awardedPoints;
  if (total == null) return null;
  const exact = item.exactScore ? EXACT_SCORE_BONUS : 0;
  const bonus = item.bonusPoints > 0 ? item.bonusPoints : 0;
  const result = total - exact - bonus;
  return { result, exact, bonus, total, hasParts: exact > 0 || bonus > 0 };
};

// "+120" / "-90"
export const signed = (n) => (n >= 0 ? `+${n}` : `${n}`);
