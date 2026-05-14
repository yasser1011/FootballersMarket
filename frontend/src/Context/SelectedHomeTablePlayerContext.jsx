import { useState, createContext } from "react";

export const SelectedHomeTablePlayerContext = createContext();

export const SelectedHomeTablePlayerProvider = (props) => {
  const [selectedHomeTablePlayer, setSelectedHomeTablePlayer] = useState(null);

  return (
    <SelectedHomeTablePlayerContext.Provider
      value={{ selectedHomeTablePlayer, setSelectedHomeTablePlayer }}
    >
      {props.children}
    </SelectedHomeTablePlayerContext.Provider>
  );
};
