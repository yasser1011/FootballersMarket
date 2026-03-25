package com.yasser.footballersmarket.transaction;

import com.yasser.footballersmarket.player.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("select t from Transaction t join t.player p where p.sofascoreId = ?1")
    List<Transaction> getPlayerTransactions(Integer playerSofascoreId);

    @Modifying
    @Query("delete from Transaction t where t.player in (select p from Player p where p.sofascoreId = ?1)")
    void deletePlayerTransactions(Integer playerSofascoreId);

    @Modifying
    @Query("delete from Transaction t where t.player in (select p from Player p where p.sofascoreId in ?1)")
    void deleteAllPlayersTransactions(List<Integer> playerSofascoreIds);

    @Query("SELECT t FROM Transaction t")
    Page<Transaction> getTransactionsPage(Pageable pageable);

}
