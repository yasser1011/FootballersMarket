package com.yasser.footballersmarket.fixture;

// something that must react when a fixture reaches a final result: settle predictions, update
// player stats, reprice, refresh featured players.
//
// declared HERE, in the package that triggers it, so this package never imports the packages that
// react -- they import this. that keeps the dependency one-way at compile time while the call still
// runs the other way at runtime.
//
// IMPLEMENTORS MUST annotate this method @Transactional(propagation = REQUIRES_NEW). the caller
// catches and logs a failing handler so the fixture's result still stands, and that only works if
// the handler ran in its own transaction: a handler that joined the caller's would mark the shared
// transaction rollback-only, and the catch would not clear it -- the fixture's commit would then
// fail with UnexpectedRollbackException.
public interface FixtureFinishedHandler {

    void onFixtureFinished(Fixture fixture);
}
