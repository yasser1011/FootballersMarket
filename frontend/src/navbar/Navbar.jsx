import * as React from "react";
import "./Navbar.css";
import "../worldcup/WorldCup.css";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Search from "./Search";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import HomeIcon from "@mui/icons-material/Home";
import NavUserData from "./NavUserData";
import WorldCupToggle from "../worldcup/WorldCupToggle";

export default function Navbar() {
  const location = useLocation();
  const navigate = useNavigate();
  const worldCupMode = location.pathname.startsWith("/worldcup");
  const homePath = worldCupMode ? "/worldcup" : "/";

  return (
    <>
      <AppBar
        style={{ backgroundColor: "var(--app-primary)" }}
        position="static"
      >
        <Toolbar className="navbar-main">
          <div className="nav-left">
            <Link className="nav-home-icon" to={homePath}>
              <HomeIcon style={{ color: "white" }} />
            </Link>
            <Link className="app-name" to={homePath}>
              <Typography variant="h6">Footballers Stock Market</Typography>
            </Link>
            <WorldCupToggle
              checked={worldCupMode}
              onToggle={() => navigate(worldCupMode ? "/" : "/worldcup")}
            />
          </div>

          <div className="nav-center">
            <Search />
          </div>

          <div className="nav-right">
            <NavUserData />
          </div>
        </Toolbar>
      </AppBar>

      <Outlet />
    </>
  );
}
