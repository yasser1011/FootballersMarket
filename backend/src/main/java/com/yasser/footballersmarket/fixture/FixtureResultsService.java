package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FixtureResultsService {

    private final FixtureRepository fixtureRepository;
    // every FixtureFinishedHandler bean, whichever package it lives in
    private final List<FixtureFinishedHandler> finishedHandlers;
    private final Logger logger = LoggerFactory.getLogger(FixtureResultsService.class);

    public FixtureResultsService(FixtureRepository fixtureRepository,
                                 List<FixtureFinishedHandler> finishedHandlers) {
        this.fixtureRepository = fixtureRepository;
        this.finishedHandlers = finishedHandlers;
    }

    // applies one fixture's latest state. the status flip into a finished code is the once-only
    // latch: finished fixtures drop out of the poll, so this runs at most once per fixture with a
    // final result -- which is what makes it safe to hang player stats and settlement off it.
    //
    // not transactional: the fixture save is the only write here, so it is already atomic on its
    // own, and the handlers below each run in their own transaction
    public void applyResult(Fixture fixture, FixtureResponseDto.Response result) {
        String status = FixtureApiMapping.status(result);

        fixture.setStatus(status);
        fixture.setHomeGoals(FixtureApiMapping.homeGoals(result));
        fixture.setAwayGoals(FixtureApiMapping.awayGoals(result));
        fixture.setWinnerTeamId(FixtureApiMapping.resolveWinner(result));
        fixtureRepository.save(fixture);

        if (!FixtureResultConstants.FINISHED_STATUSES.contains(status)) {
            logger.info("fixture results: {} still {} -- will be checked again", fixture.getId(), status);
            return;
        }

        logger.info("fixture results: {} finished {}-{}, winner team {}",
                fixture.getId(), fixture.getHomeGoals(), fixture.getAwayGoals(), fixture.getWinnerTeamId());

        // reactions to a finished fixture: prediction settlement today, and later player stats and
        // prices, last-5 ratings, featured players. the per-player ratings for those arrive in
        // result.players(), which is not plumbed through yet.
        //
        // note the order these run in is Spring's bean order, i.e. unspecified. if the reactions
        // ever need sequencing relative to each other, that is the point to replace this with an
        // explicit mediator rather than sprinkling @Order across packages.
        // best-effort: a handler failing must not undo the result we just wrote, or the fixture
        // would drop back into the poll and keep retrying. each handler runs in its own transaction
        // (see FixtureFinishedHandler) so its rollback cannot reach this one
        for (FixtureFinishedHandler handler : finishedHandlers) {
            try {
                handler.onFixtureFinished(fixture);
            } catch (Exception e) {
                logger.error("fixture results: handler {} failed for fixture {}, result stands: {}",
                        handler.getClass().getSimpleName(), fixture.getId(), e.getMessage(), e);
            }
        }
    }
}
