package com.yasser.footballersmarket.rapidapi.dto;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought;
import com.yasser.footballersmarket.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

public class PlayersDataSavingDto {
    private final List<Transaction> transactions;
    private final List<PlayerUserCurrentBought> playersCurrentlyBoughtByUsers;
    private final List<Integer> playerExtIdsToBeDeleted;

    private final List<PlayerLeagueStats> playerLeagueStatsList;
    private final List<Player> playersEntityList;

    public PlayersDataSavingDto() {
        this.transactions = new ArrayList<>();
        this.playersCurrentlyBoughtByUsers = new ArrayList<>();
        this.playerExtIdsToBeDeleted = new ArrayList<>();
        this.playerLeagueStatsList = new ArrayList<>();
        this.playersEntityList = new ArrayList<>();
    }

    public void addTransactionToList(Transaction transaction){
        this.transactions.add(transaction);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void addPlayerToCurrentBoughtPlayersList(PlayerUserCurrentBought currentBought){
        this.playersCurrentlyBoughtByUsers.add(currentBought);
    }

    public List<PlayerUserCurrentBought> getPlayersCurrentlyBoughtByUsers() {
        return playersCurrentlyBoughtByUsers;
    }

    public void addExtIdsToExtPlayerIdsList(Integer playerExtId){
        this.playerExtIdsToBeDeleted.add(playerExtId);
    }

    public List<Integer> getPlayerExtIdsToBeDeleted() {
        return playerExtIdsToBeDeleted;
    }

    public void addPlayerLeagueStatsToLeagueStatsList(PlayerLeagueStats leagueStats){
        this.playerLeagueStatsList.add(leagueStats);
    }

    public List<PlayerLeagueStats> getPlayerLeagueStatsList() {
        return playerLeagueStatsList;
    }

    public void addPlayerToPlayersList(Player player){
        this.playersEntityList.add(player);
    }

    public List<Player> getPlayersEntityList() {
        return playersEntityList;
    }
}
