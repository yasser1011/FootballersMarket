package com.yasser.footballersmarket.playerusercurrentbought;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerUserCurrentBoughtRepository extends JpaRepository<PlayerUserCurrentBought, Long> {
//    @Query("select pu from PlayerUserCurrentBought pu where pu.userId = ?1 and pu.playerId = ?2")
    @Query("select new com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought(pu.id, pu.userId, pu.playerId, pu.buyPrice) from PlayerUserCurrentBought pu where pu.userId = ?1 and pu.playerId = ?2")
//    PlayerUserCurrentBoughtDao findOneByUserIdAndPlayerId(Long userId, Long playerId);
    PlayerUserCurrentBought findOneByUserIdAndPlayerId(Long userId, Long playerId);

    @Query("select pu from PlayerUserCurrentBought pu join pu.player p where p.sofascoreId = ?1")
    List<PlayerUserCurrentBought> getPlayerCurrBoughtTransactions(Integer playerSofascoreId);

    @Query("select new com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought(pu.id, pu.buyPrice, pu.userId, pu.playerId, pu.player, pu.boughtAt) from PlayerUserCurrentBought pu where pu.userId = ?1")
    List<PlayerUserCurrentBought> getUserCurrBoughtPlayers(Long userId);

    @Query("select pu.boughtAt from PlayerUserCurrentBought pu where pu.userId = ?1 and pu.playerId = ?2")
    java.time.LocalDateTime getBoughtAt(Long userId, Long playerId);

    @Query("select new com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought(pu.id, pu.userId, pu.playerId, pu.buyPrice) from PlayerUserCurrentBought pu where pu.userId = ?1")
    List<PlayerUserCurrentBought> getBasicUserCurrBoughtPlayers(Long userId);

    @Modifying
    @Query("delete from PlayerUserCurrentBought pu where pu.player in (select p from Player p where p.sofascoreId = ?1)")
    void deletePlayerCurrBoughtTransactions(Integer playerSofascoreId);

    @Modifying
    @Query("delete from PlayerUserCurrentBought pu where pu.player in (select p from Player p where p.sofascoreId in ?1)")
    void deleteAllPlayersCurrBoughtTransactions(List<Integer> playerSofascoreId);

    @Modifying
    @Query("delete from PlayerUserCurrentBought pu where pu.userId = ?1 and pu.playerId = ?2")
    void deletePlayerCurrBoughtTransaction(Long userId, Long playerId);

}
