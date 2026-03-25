import * as React from "react";
import "./Navbar.css";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Search from "./Search";
import { Link, Outlet } from "react-router-dom";
import HomeIcon from "@mui/icons-material/Home";
import NavUserData from "./NavUserData";

export default function Navbar() {
  return (
    <>
      <AppBar style={{ backgroundColor: "rgb(35, 47, 62)" }} position="static">
        <Toolbar className="navbar-main">
          <div className="nav-home-search-div">
            <Link className="nav-home-icon" to="/">
              <HomeIcon style={{ color: "white" }} />
            </Link>
            <Link className="app-name" to="/">
              <Typography
                variant="h6"
                // noWrap
                // component="div"
                // sx={{ display: { xs: "none", sm: "block" } }}
              >
                Footballers Stock Market
              </Typography>
            </Link>

            {/* <Box sx={{ flexGrow: 1 }} /> */}
            <div style={{ flexGrow: 1 }}>
              <Search />
            </div>
          </div>

          {/* <Box sx={{ flexGrow: 1 }} /> */}
          <div>
            <NavUserData />
          </div>
        </Toolbar>
      </AppBar>

      <Outlet />
    </>
  );
}
