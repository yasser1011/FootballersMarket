import { useState, createContext } from "react";

export const HomePageContext = createContext();

export const HomePageProvider = (props) => {
  const [featuredGame, setFeaturedGame] = useState(null);
  const [featuredPlayers, setFeaturedPlayers] = useState(null);
  const [paginationOptions, setPaginationOptions] = useState({
    disabled: true,
    currentPage: 0,
    rowsPerPage: 0,
    totalCount: 0,
  });
  // WC nationality filter — persists across player-details navigation
  const [wcNationalityTeamId, setWcNationalityTeamId] = useState("");

  return (
    <HomePageContext.Provider
      value={{
        featuredGame,
        setFeaturedGame,
        featuredPlayers,
        setFeaturedPlayers,
        paginationOptions,
        setPaginationOptions,
        wcNationalityTeamId,
        setWcNationalityTeamId,
      }}
    >
      {props.children}
    </HomePageContext.Provider>
  );
};
