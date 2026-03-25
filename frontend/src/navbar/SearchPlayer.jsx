import axios from "axios";
import React, { useState } from "react";
import CircularProgress from "@mui/material/CircularProgress";
import { useNavigate } from "react-router-dom";
import { apiBaseUrl } from "../config/Config";

const SearchPlayer = ({
  searchPlayer,
  setShowSearchList,
  setSearchInputValue,
}) => {
  // check if sofascore id is found in local db
  // - if player is found and updated flag is true return it and route to players/rp/:id
  // - if player is found and updated flag is false return data from sofascore in backend
  // and update his data in db and flag updated by 'sofascore'
  // - if not found route to players/sf/:id

  const FETCH_STATUS = {
    ERROR: "error",
    SUCCESS: "success",
    LOADING: "loading",
    IDLE: "idle",
  };

  const [fetchStatus, setFetchStatus] = useState(FETCH_STATUS.IDLE);

  const navigate = useNavigate();

  const checkSearchPlayerStatus = async () => {
    //check player in db by his sofascore id
    // /api/players/sofascore?sofascoreId=searchPlayer.id
    setFetchStatus(FETCH_STATUS.LOADING);
    try {
      let playerSofascoreId = searchPlayer.id;
      const playerFoundRes = await axios.get(
        `${apiBaseUrl}/players/sofascore?sofascoreId=${playerSofascoreId}`
      );
      if (!playerFoundRes || !playerFoundRes.data) {
        setFetchStatus(FETCH_STATUS.ERROR);
        return;
      }
      setFetchStatus(FETCH_STATUS.SUCCESS);
      setShowSearchList(false);
      setSearchInputValue("");
      let playerDetailsRes = playerFoundRes.data;
      // console.log(playerDetailsRes);
      if (playerFoundRes.data.status == -1) {
        // player not found in db
        // get his data from external service
        // just route to player details page url ss
        navigate(`/players/sf/${playerSofascoreId}`);
      } else {
        navigate(`/players/rp/${playerDetailsRes.player.id}`, {
          state: { player: playerDetailsRes.player, srcLocation: "search" },
        });
      }
    } catch (error) {
      console.log(error);
      setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  return (
    <div className="search-player-row-div" onClick={checkSearchPlayerStatus}>
      <img
        src={searchPlayer.img}
        alt={searchPlayer.name}
        className="search-player-row-img"
        width={40}
        height={40}
      />
      <div className="search-player-row-info">
        <div className="search-player-row-name-style">{searchPlayer.name}</div>
        <div className="search-player-row-club-info">
          <img
            src={searchPlayer.teamImg}
            alt={searchPlayer.teamName}
            width={16}
            height={16}
          />
          <span className="search-player-row-clubname-style">
            {searchPlayer.teamName}
          </span>
        </div>
      </div>
      {fetchStatus === FETCH_STATUS.LOADING ? (
        <div style={{ marginLeft: "40px" }}>
          <CircularProgress size="1rem" />
        </div>
      ) : null}
      {fetchStatus === FETCH_STATUS.ERROR ? (
        <div style={{ marginLeft: "40px", color: "black" }}>error</div>
      ) : null}
    </div>
  );
};

export default SearchPlayer;
