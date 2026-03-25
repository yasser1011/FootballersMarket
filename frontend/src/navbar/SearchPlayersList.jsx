import React, { useEffect, useState } from "react";
import SearchPlayer from "./SearchPlayer";
import axios from "axios";
import { useOnMountUnsafe } from "../customhooks/useOnMountUnsafe";
import { CircularProgress } from "@mui/material";
import { apiBaseUrl } from "../config/Config";

const SearchPlayersList = ({
  queryText,
  setShowSearchList,
  setSearchInputValue,
}) => {
  const FETCH_STATUS = {
    ERROR: "error",
    SUCCESS: "success",
    LOADING: "loading",
  };

  const [searchPlayers, setSearchPlayers] = useState([]);
  const [fetchStatus, setFetchStatus] = useState(FETCH_STATUS.LOADING);

  let ranOnce = false;

  const fetchSearchPlayers = async () => {
    try {
      const url = `${apiBaseUrl}/ss/players/search?playerName=${queryText}`;
      const playersRes = await axios.get(url);
      if (playersRes && playersRes.data) {
        setSearchPlayers(playersRes.data);
        setFetchStatus(FETCH_STATUS.SUCCESS);
      }
    } catch (error) {
      setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  useEffect(() => {
    if (!ranOnce) {
      fetchSearchPlayers();
      ranOnce = true;
    }
  }, [queryText]);

  if (queryText && queryText.length <= 1) {
    return (
      <div style={{ color: "black" }}>Please enter more than 1 character</div>
    );
  }

  if (fetchStatus === FETCH_STATUS.LOADING) {
    return (
      <div style={{ color: "black" }}>
        <CircularProgress size={"1rem"} />
      </div>
    );
  }

  if (searchPlayers.length == 0) {
    return <div style={{ color: "black" }}>No Player found</div>;
  }

  return (
    <>
      {searchPlayers.map((searchPlayer) => (
        <SearchPlayer
          key={searchPlayer.id}
          searchPlayer={searchPlayer}
          setShowSearchList={setShowSearchList}
          setSearchInputValue={setSearchInputValue}
        />
      ))}
    </>
  );
};

export default SearchPlayersList;
