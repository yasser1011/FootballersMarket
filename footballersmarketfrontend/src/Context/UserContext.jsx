import { useState, useEffect, createContext } from "react";
import axios from "axios";
import { useOnMountUnsafe } from "../customhooks/useOnMountUnsafe";
import { apiBaseUrl } from "../config/Config";

export const UserContext = createContext();

export const UserProvider = (props) => {
  const [userData, setUserData] = useState({ user: null, token: null });
  const [userLoading, setUserLoading] = useState(true);

  const fetchUser = async () => {
    let token = localStorage.getItem("auth-token");

    if (token == null) {
      localStorage.setItem("auth-token", "");
      token = "";
      setUserLoading(false);
      return;
    }

    try {
      const userRes = await axios.get(`${apiBaseUrl}/auth/user-by-token`, {
        headers: { Authorization: token },
      });

      setUserData({ token: token, user: userRes.data });
      setUserLoading(false);
    } catch (err) {
      setUserLoading(false);
      // console.log(err);
    }
  };

  useOnMountUnsafe(() => {
    fetchUser();
  }, []);

  return (
    <UserContext.Provider value={{ userData, setUserData, userLoading }}>
      {props.children}
    </UserContext.Provider>
  );
};
