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

  return (
    <HomePageContext.Provider
      value={{
        featuredGame,
        setFeaturedGame,
        featuredPlayers,
        setFeaturedPlayers,
        paginationOptions,
        setPaginationOptions,
      }}
    >
      {props.children}
    </HomePageContext.Provider>
  );
};
