import { AccountCircle, Padding } from "@mui/icons-material";
import {
  Box,
  Button,
  CircularProgress,
  IconButton,
  Typography,
} from "@mui/material";
import React, { useContext, useState } from "react";
import { UserContext } from "../Context/UserContext";
import AuthModal from "../auth/AuthModal";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useRef } from "react";

const NavUserData = () => {
  const navUserLinksRef = useRef(null);

  const { userData, setUserData, userLoading } = useContext(UserContext);

  // keep WC mode across the nav links (the /worldcup prefix is the source of truth)
  const { pathname } = useLocation();
  const wcPrefix = pathname.startsWith("/worldcup") ? "/worldcup" : "";
  const rankingsPath = `${wcPrefix}/rankings`;
  const myTeamPath = `${wcPrefix}/myteam`;

  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authModalType, setAuthModalType] = useState(null);

  const authModalTypes = { SIGNIN: "SIGNIN", SIGNUP: "SIGNUP" };

  const navigate = useNavigate();

  const handleAuthOpen = (authType) => {
    setAuthModalType(authType);
    setAuthModalOpen(true);
  };

  const handleLogout = () => {
    setUserData({ token: null, user: null });
    localStorage.setItem("auth-token", "");
  };

  if (userLoading) {
    return (
      <>
        <Box sx={{ flexGrow: 1 }} />
        <CircularProgress size="2rem" />
      </>
    );
  }

  if (userData.user) {
    return (
      <>
        <a
          href="#"
          className="toggle-button"
          onClick={() => navUserLinksRef.current.classList.toggle("active")}
        >
          <span className="bar"></span>
          <span className="bar"></span>
          <span className="bar"></span>
        </a>
        <div ref={navUserLinksRef} className="navbar-user-data">
          <Link
            style={{
              textDecoration: "none",
              color: "white",
            }}
            to={myTeamPath}
            className="hover-pointer"
          >
            <Typography>My Squad</Typography>
          </Link>

          <Link
            style={{
              textDecoration: "none",
              color: "white",
            }}
            to={rankingsPath}
            className="hover-pointer"
          >
            <Typography>Rankings</Typography>
          </Link>

          <Typography className="hover-pointer" onClick={handleLogout}>
            Signout
          </Typography>
          <Typography>{userData.user.points} Pts</Typography>

          <div style={{ display: "flex", alignItems: "center" }}>
            <Typography>{userData.user.username}</Typography>
            <AccountCircle />
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      {authModalOpen && (
        <AuthModal
          authModalOpen={authModalOpen}
          setAuthModalOpen={setAuthModalOpen}
          authModalTypes={authModalTypes}
          authModalType={authModalType}
          setAuthModalType={setAuthModalType}
        />
      )}
      <a
        href="#"
        className="toggle-button"
        onClick={() => navUserLinksRef.current.classList.toggle("active")}
      >
        <span className="bar"></span>
        <span className="bar"></span>
        <span className="bar"></span>
      </a>
      <div ref={navUserLinksRef} className="navbar-user-data">
        <Link
          style={{
            textDecoration: "none",
            color: "white",
          }}
          to={rankingsPath}
          className="hover-pointer"
        >
          <Typography>Rankings</Typography>
        </Link>
        <Typography
          className="hover-pointer"
          onClick={() => handleAuthOpen(authModalTypes.SIGNIN)}
        >
          Signin
        </Typography>

        <Typography
          className="hover-pointer"
          onClick={() => handleAuthOpen(authModalTypes.SIGNUP)}
        >
          Signup
        </Typography>
        {/* <Button
        color="inherit"
        onClick={() => handleAuthOpen(authModalTypes.SIGNUP)}
      >
        Signup
      </Button> */}
      </div>
    </>
  );
};

export default NavUserData;
