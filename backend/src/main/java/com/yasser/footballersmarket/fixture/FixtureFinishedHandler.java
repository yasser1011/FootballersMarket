package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;

// something that must react when a fixture reaches a final result: settle predictions, update
// player stats, reprice, refresh featured players.
//
// declared HERE, in the package that triggers it, so this package never imports the packages that
// react -- they import this. that keeps the dependency one-way at compile time while the call still
// runs the other way at runtime.
//
// IMPLEMENTORS MUST run in their own transaction. the caller catches and logs a failing handler so
// the fixture's result still stands, and that only works if the handler did not join the caller's
// transaction: joining would mark the shared transaction rollback-only, the catch would not clear
// it, and the fixture's commit would then fail with UnexpectedRollbackException.
//
// today applyResult has no transaction, so a plain @Transactional on the handler already starts a
// fresh one. if a transaction is ever added to applyResult (or anything above it), every handler
// must switch to @Transactional(propagation = REQUIRES_NEW).
public interface FixtureFinishedHandler {

    // result is the api payload the fixture was just finished from. it carries what a reaction may
    // need beyond the score -- per-player minutes and ratings in result.players() -- which the
    // Fixture row does not store. a handler that only needs the score can ignore it
    void onFixtureFinished(Fixture fixture, FixtureResponseDto.Response result);
}
