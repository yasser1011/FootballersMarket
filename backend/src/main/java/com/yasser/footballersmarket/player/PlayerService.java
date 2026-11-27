package com.yasser.footballersmarket.player;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yasser.footballersmarket.pricing.PlayerPricingData;
import com.yasser.footballersmarket.pricing.PriceStrategy;
import com.yasser.footballersmarket.player.dto.PlayerBasic;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.player.dto.PlayerExternalServiceBasicDetails;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsService;
import com.yasser.footballersmarket.playerstats.PlayerRecentMatchService;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStats;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.scores365.dto.PlayerDetailsDto;
import com.yasser.footballersmarket.scores365.dto.PlayerRecentMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final PlayerLeagueStatsService playerLeagueStatsService;
    private final PriceStrategy priceStrategy;
    private final ScoresService scoresService;
    private final PlayerRecentMatchService recentMatchService;
    // for the write after an external call: the methods making that call run outside any
    // transaction, and calling a @Transactional method of this class from them would bypass the
    // proxy and run with none at all
    private final TransactionTemplate transactionTemplate;
    private final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    public PlayerService(PlayerRepository playerRepository,
                         PlayerLeagueStatsService playerLeagueStatsService,
                         PriceStrategy priceStrategy,
                         ScoresService scoresService,
                         PlayerRecentMatchService recentMatchService,
                         PlatformTransactionManager transactionManager) {
        this.playerRepository = playerRepository;
        this.playerLeagueStatsService = playerLeagueStatsService;
        this.priceStrategy = priceStrategy;
        this.scoresService = scoresService;
        this.recentMatchService = recentMatchService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ---- player details: our db first, 365scores only when the player needs it ----
    //
    // NOT_SUPPORTED: these call 365scores, and an http call inside a transaction would hold a db
    // connection for as long as 365 takes to answer

    // the details page, by 365scores id (the home table, my squad and the search bar all open it by it)
    //  - not in db (a search result): built from 365 and priced, nothing saved; 404 if 365 fails too
    //  - fixture-maintained: our stats as they are, never refreshed; last 5 ours, 365's if we lack them
    //  - flagged / sofascore / sofascore_buy: refreshed from 365 and written, then repriced; the
    //    stored data as it is if 365 fails
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PlayerDetailsResDto getPlayerDetails(Integer externalId) {
        PlayerDetailsResDto stored = getPlayerDetailsByExternalId(externalId);
        if (stored == null) return detailsOfUnknownPlayer(externalId);

        if (!Player.refreshesExternally(stored.getUpdatedBy(), stored.getMissedMatches())) {
            stored.setRecentMatches(lastRatingsOf(stored, externalId));
            return stored;
        }

        PlayerDetailsDto external = scoresService.fetchPlayerEntityFromExternalService(externalId);
        if (external == null) {
            logger.error("player not found by external id {} returning db data", externalId);
            return stored;
        }
        stored.setRecentMatches(external.getRecentMatches());
        // does changes on the stored object directly
        applyExternalRefresh(stored, external);
        return stored;
    }

    // the player a buy or sell is priced against, resolved BEFORE the transaction opens (it may call
    // 365, see TransactionService.makeTransaction):
    //  - in db, fixture-maintained or refreshed in the last 3 days: his stored data, no http
    //  - in db, externally refreshed and older than that (e.g. a page left open for days): refreshed
    //    and written first, so he cannot be bought at a price nobody has seen in weeks. 365 down ->
    //    the stored (stale) price is allowed
    //  - not in db (a search result): built from 365 as the details page showed him; 404 if 365 fails
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PlayerDetailsResDto resolveForTransaction(Long playerId, Integer externalId) {
        // the request carries the internal id when the page had one. without it, he may still have
        // been bought (and so created) by someone else since that page loaded
        PlayerDetailsResDto stored = playerId != null
                ? getPlayerDetailsByInternalId(playerId)
                : getPlayerDetailsByExternalId(externalId);

        if (stored == null) {
            PlayerDetailsDto external = scoresService.fetchPlayerEntityFromExternalService(externalId);
            if (external == null) throw new EntityNotFoundException("player not found in external service " + externalId);
            return convertExternalToPlayerDto(externalId, external);
        }

        if (!Player.needsExternalRefresh(stored.getUpdatedBy(), stored.getMissedMatches(),
                stored.getExternallyRefreshedAt(), Instant.now())) {
            return stored;
        }
        PlayerDetailsDto external = scoresService.fetchPlayerEntityFromExternalService(stored.getExternalServicePlayerId());
        if (external == null) {
            logger.warn("player {} stale and the external service failed: transaction priced on stored data", stored.getId());
            return stored;
        }
        applyExternalRefresh(stored, external);
        return stored;
    }

    // the last 5 alone, for a page that already shows the cached home table row
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<PlayerRecentMatch> getLastRatings(Integer externalId) {
        PlayerDetailsResDto stored = getPlayerDetailsByExternalId(externalId);
        return stored != null ? lastRatingsOf(stored, externalId) : scoresService.fetchRecentMatches(externalId);
    }

    // ours when the fixtures give 5 of them, otherwise 365's (empty if 365 fails)
    private List<PlayerRecentMatch> lastRatingsOf(PlayerDetailsResDto stored, Integer externalId) {
        return recentMatchService.fromFixtures(stored.getId(), stored.getUpdatedBy(), stored.getMissedMatches())
                .orElseGet(() -> scoresService.fetchRecentMatches(externalId));
    }

    private PlayerDetailsResDto detailsOfUnknownPlayer(Integer externalId) {
        PlayerDetailsDto external = scoresService.fetchPlayerEntityFromExternalService(externalId);
        if (external == null) {
            throw new EntityNotFoundException("player not found in both db and scores service external id " + externalId);
        }
        logger.info("new player not found in database external id {}", externalId);
        return convertExternalToPlayerDto(externalId, external);
    }

    // writes what 365 reports for an externally refreshed player in ONE transaction -- his league
    // stats (and updatedBy, see updatePlayerLeagueStatsDetails) and, when flagged, the club he has
    // probably moved to -- then mirrors it on the dto and reprices it
    private void applyExternalRefresh(PlayerDetailsResDto stored, PlayerDetailsDto external) {
        logger.info("player refreshed from external service, external id {} internal id {}",
                stored.getExternalServicePlayerId(), stored.getId());
        boolean flagged = stored.isFlagged();
        String clubName = external.getClubName();
        String clubPhotoUrl = scoresService.clubPhotoUrl(external.getClubId());
        boolean hasLeagueStats = external.getLeagueStats() != null;

        transactionTemplate.executeWithoutResult(status -> {
            if (flagged) updatePlayerClubFromExternal(stored.getId(), clubName, clubPhotoUrl);
            // its own instance: saving attaches it to the player and the session, the dto keeps another
            if (hasLeagueStats) {
                updatePlayerLeagueStatsDetails(stored.getId(), scoresService.createLeagueStatsDto(external.getLeagueStats()));
            } else {
                // 365 answered but has no league stats for him: still a refresh, so a buy within
                // the next 3 days does not ask again (updatePlayerLeagueStatsDetails stamps otherwise)
                playerRepository.markExternallyRefreshed(stored.getId(), Instant.now());
            }
        });

        if (flagged) {
            if (clubName != null) stored.setExternalServicePlayerClub(clubName);
            if (clubPhotoUrl != null) stored.setClubPhotoUrl(clubPhotoUrl);
        }
        if (hasLeagueStats) {
            stored.setLeagueStats(scoresService.createLeagueStatsDto(external.getLeagueStats()));
            // league rating just changed: reprice
            priceDto(stored);
        }
    }

    // a player we do not have, as 365 reports him, priced like any other: what the details page
    // shows for a search result and what a buy of him is checked against. 365 without league stats
    // for him -> the default rating, so he still prices sanely
    public PlayerDetailsResDto convertExternalToPlayerDto(Integer externalId, PlayerDetailsDto external) {
        PlayerDetailsResDto playerDto = new PlayerDetailsResDto();
        playerDto.setExternalServicePlayerId(externalId);
        playerDto.setName(external.getName());
        playerDto.setNationality(external.getNationality());
        playerDto.setDateOfBirth(external.getDateOfBirth());
        playerDto.setPosition(external.getPosition());
        playerDto.setRapidApiClub("");
        playerDto.setExternalServicePlayerClub(external.getClubName());
        playerDto.setPhotoUrl(scoresService.playerPhotoUrl(externalId));
        playerDto.setClubPhotoUrl(scoresService.clubPhotoUrl(external.getClubId()));
        playerDto.setLeagueStats(external.getLeagueStats() == null
                ? new PlayerLeagueStats(0, 0, 0, 6.5)
                : scoresService.createLeagueStatsDto(external.getLeagueStats()));
        playerDto.setRecentMatches(external.getRecentMatches());
        priceDto(playerDto);
        return playerDto;
    }

    public PlayerDetailsResDto getPlayerDetailsByInternalId(Long rapidApiId) {
        Player playerLocalDb = playerRepository.findById(rapidApiId)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id " + rapidApiId));
        logger.info("player found in database, player id {}", rapidApiId);
        return convertPlayerEntityToPlayerDto(playerLocalDb);
    }

    public List<PlayerBasic> getPlayersUpdatedBySofascore(){
        logger.info("retrieving players which are updated by sofascore");
        return playerRepository.getPlayersUpdatedBySofascore();
    }
    public List<Integer> getPlayersWithoutRapidId(){
        logger.info("retrieving players which was bought from sofascore search");
        return playerRepository.getPlayersWithoutRapidId();
    }

    public void updatePlayerLeagueStatsDetails(Long rapidId, PlayerLeagueStats updatedLeagueStats){
        logger.info("updating player data in database player id {}", rapidId);
        // PlayerLeagueStats derives its @Id from the @MapsId player relationship. existing row ->
        // set the id so save() merges/updates it. brand-new row (e.g. a world-cup-seeded player who
        // never had league stats) -> leave the id null and set the player so save() persists and
        // @MapsId fills the id from it; setting the id here would make save() merge and fail to
        // derive the identifier ("null identifier").
        if (playerLeagueStatsService.leagueStatsExist(rapidId)) {
            updatedLeagueStats.setPlayerId(rapidId);
        } else {
            updatedLeagueStats.setPlayer(playerRepository.getReferenceById(rapidId));
        }
        playerLeagueStatsService.savePlayerLeagueStats(updatedLeagueStats);
        updatePlayerUpdatedByStatusToSofascore(rapidId);
        // every caller writes what the external service just reported: his data is current as of now
        playerRepository.markExternallyRefreshed(rapidId, Instant.now());
        logger.info("player data updated in database player id {}", rapidId);
    }

    public void deleteRapidDataPlayers(){
        logger.info("deleting players data");
        playerRepository.deleteRapidDataPlayers();
    }
    public void deletePlayerBySofascoreId(Integer playerSofascoreId){
        logger.info("deleting player by sofascore id, {}", playerSofascoreId);
        playerRepository.deletePlayer(playerSofascoreId);
    }

    public void deleteAllPlayersBySofascoreIds(List<Integer> playerSofascoreIds){
        logger.info("deleting all players by sofascore ids");
        playerRepository.deleteAllPlayersBySofascoreIds(playerSofascoreIds);
    }

    public List<Long> getRapidDataPlayerIds(){
        return playerRepository.getRapidDataPlayerIds();
    }

    public List<PlayerExternalServiceBasicDetails> getPlayerExternalServiceBasicDetails(){
        return playerRepository.getPlayerExternalServiceBasicDetails();
    }

    public void savePlayersList(List<Player> playerEntityList){
        playerRepository.saveAll(playerEntityList);
    }

    public void savePlayer(Player playerEntity){
        playerRepository.save(playerEntity);
    }

    // a flagged player has most likely moved to a club we do not cover; the external refresh knows
    // which one, so his club name and badge follow it instead of showing the club he left.
    // null values (external service did not say) leave the stored ones alone
    public void updatePlayerClubFromExternal(Long playerId, String clubName, String clubPhotoUrl) {
        playerRepository.findById(playerId).ifPresent(player -> {
            if (clubName != null) player.setSofascoreClub(clubName);
            if (clubPhotoUrl != null) player.setClubPhotoUrl(clubPhotoUrl);
            playerRepository.save(player);
        });
    }

    public Page<PlayerDetailsResDto> getHomePagePlayerData(int pageNum, boolean worldCup, Long teamId){
        // page num starts with 0
        if (worldCup) {
            // world cup page: sort by tournament rating; teamId optionally filters by national team
            Pageable wcPageData = PageRequest.of(pageNum, 50, Sort.by("wcs.rating").descending().and(Sort.by("id").ascending()));
            return playerRepository.getWorldCupHomePagePlayers(teamId, wcPageData)
                    .map(this::convertPlayerEntityToPlayerDto);
        }
        Pageable pageData = PageRequest.of(pageNum, 50, Sort.by("ls.rating").descending().and(Sort.by("id").ascending()));
        Page<Player> homePagePlayers = playerRepository.getHomePagePlayers(pageData);
        Page<PlayerDetailsResDto> map = homePagePlayers.map(this::convertPlayerEntityToPlayerDto);
        return map;
    }

    public void updatePlayerUpdatedByStatusToSofascore(Long playerId){
        logger.info("updating player updated by flag to sofascore, player id {}", playerId);
        playerRepository.updatePlayerUpdatedByStatusToSofascore(playerId);
    }

    public PlayerDetailsResDto getPlayerDetailsByExternalId(Integer sofascoreId){
        Player oneBySofascoreId = playerRepository.findFirstBySofascoreId(sofascoreId);
        return convertPlayerEntityToPlayerDto(oneBySofascoreId);
    }

    public PlayerDetailsResDto getPlayerByIdAndSofascoreId(Long playerId, Integer sofascoreId){
        Player oneByIdAndSofascoreId = playerRepository.findOneByIdAndSofascoreId(playerId, sofascoreId);
        return convertPlayerEntityToPlayerDto(oneByIdAndSofascoreId);
    }

    public void deleteAll(){
        playerRepository.deleteAll();
    }

    public PlayerDetailsResDto convertPlayerEntityToPlayerDto(Player playerEntity){
        if (playerEntity == null) return null;
        PlayerDetailsResDto playerDto = new PlayerDetailsResDto();
        playerDto.setId(playerEntity.getId());
        playerDto.setExternalServicePlayerId(playerEntity.getSofascoreId());
        playerDto.setName(playerEntity.getName());
        playerDto.setNationality(playerEntity.getNationality());
        playerDto.setDateOfBirth(playerEntity.getDateOfBirth());
        playerDto.setPosition(playerEntity.getPosition());
        playerDto.setRapidApiClub(playerEntity.getRapidApiClub());
        playerDto.setExternalServicePlayerClub(playerEntity.getSofascoreClub());
        playerDto.setCurrentlyInjured(playerEntity.getCurrentlyInjured());
        playerDto.setLeagueStats(playerEntity.getLeagueStats());
        // part of the price (see PlayerDetailsResDto.getAvgRating), so it must be set before priceDto
        playerDto.setChampionsLeagueStats(playerEntity.getChampionsLeagueStats());
        playerDto.setPhotoUrl(playerEntity.getPhotoUrl());
        playerDto.setClubPhotoUrl(playerEntity.getClubPhotoUrl());
        playerDto.setUpdatedBy(playerEntity.getUpdatedBy());
        playerDto.setMissedMatches(playerEntity.getMissedMatches());
        playerDto.setExternallyRefreshedAt(playerEntity.getExternallyRefreshedAt());
        PlayerWorldCupStats wcStats = playerEntity.getWorldCupStats();
        playerDto.setWorldCupBaseRating(wcStats == null ? null : wcStats.getBaseRating());
        playerDto.setWorldCupRating(wcStats == null ? null : wcStats.getRating());
        playerDto.setWorldCupGames(wcStats == null ? null : wcStats.getTotalNumOfGames());
        // exposed tournament stats (incl. the national team via wcStats.team for the flag + name)
        playerDto.setWorldCupStats(wcStats);
        priceDto(playerDto);

        return playerDto;
    }

    // single pricing entry point: reads the dto's current state (incl. world cup inputs)
    // and sets its price via the active strategy. call again after mutating a dto's stats
    public void priceDto(PlayerDetailsResDto dto) {
        dto.setPrice(priceStrategy.calculatePrice(new PlayerPricingData(
                dto.getDateOfBirth(), dto.getAvgRating(),
                dto.getWorldCupBaseRating(), dto.getWorldCupRating(), dto.getWorldCupGames())));
    }

    public Player convertPlayerDtoToPlayerEntity(PlayerDetailsResDto playerDto){
        if (playerDto == null) return null;
        Player player = new Player();
        player.setId(playerDto.getId());
        player.setSofascoreId(playerDto.getExternalServicePlayerId());
        player.setName(playerDto.getName());
        player.setNationality(playerDto.getNationality());
        player.setDateOfBirth(playerDto.getDateOfBirth());
        player.setPosition(playerDto.getPosition());
        player.setRapidApiClub(playerDto.getRapidApiClub());
        player.setSofascoreClub(playerDto.getExternalServicePlayerClub());
        player.setCurrentlyInjured(playerDto.getCurrentlyInjured());
        player.setLeagueStats(playerDto.getLeagueStats());
        player.setPhotoUrl(playerDto.getPhotoUrl());
        player.setClubPhotoUrl(playerDto.getClubPhotoUrl());
        player.setUpdatedBy(playerDto.getUpdatedBy());

        return player;
    }

}
