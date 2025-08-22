import { useState, createContext } from "react";

export const HomePageContext = createContext();

export const HomePageProvider = (props) => {
  const [featuredGame, setFeaturedGame] = useState(null);
  const [featuredPlayers, setFeaturedPlayers] = useState(null);

  return (
    <HomePageContext.Provider
      value={{
        featuredGame,
        setFeaturedGame,
        featuredPlayers,
        setFeaturedPlayers,
      }}
    >
      {props.children}
    </HomePageContext.Provider>
  );
};
