import React, { useContext } from "react";
import HomeTable from "./HomeTable";
import HomeHeaderCards from "../cardssection/HomeHeaderCards";
import { styled } from "@mui/material/styles";
import TableCell, { tableCellClasses } from "@mui/material/TableCell";
import FirstPageRoundedIcon from "@mui/icons-material/FirstPageRounded";
import LastPageRoundedIcon from "@mui/icons-material/LastPageRounded";
import ChevronLeftRoundedIcon from "@mui/icons-material/ChevronLeftRounded";
import ChevronRightRoundedIcon from "@mui/icons-material/ChevronRightRounded";
import {
  Paper,
  Table,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
} from "@mui/material";
import "./Home.css";
import { useState } from "react";
import { apiBaseUrl } from "../config/Config";
import { HomePageContext } from "../Context/HomePageContext";

export const StyledTableCell = styled(TableCell)(({ theme }) => ({
  [`&.${tableCellClasses.head}`]: {
    // backgroundColor: theme.palette.common.black,
    backgroundColor: "rgb(35, 47, 62)",
    color: theme.palette.common.white,
  },
  [`&.${tableCellClasses.body}`]: {
    fontSize: 14,
  },
}));

const Home = () => {
  // const [paginationOptions, setPaginationOptions] = useState({
  //   disabled: true,
  //   currentPage: 0,
  //   rowsPerPage: 0,
  //   totalCount: 0,
  // });

  const { paginationOptions, setPaginationOptions } =
    useContext(HomePageContext);

  const handleChangePage = (event, newPage) => {
    setPaginationOptions((currOptions) => {
      return {
        ...currOptions,
        currentPage: newPage,
      };
    });
  };
  return (
    <div>
      <HomeHeaderCards />
      <div className="table-container">
        <div style={{ width: "100%" }}>
          <TableContainer style={{ minHeight: "10rem" }} component={Paper}>
            <Table sx={{ minWidth: 700 }} aria-label="customized table">
              <colgroup>
                <col style={{ width: "20%" }} />
                <col style={{ width: "15%" }} />
                <col style={{ width: "10%" }} />
                <col style={{ width: "10%" }} />
                <col style={{ width: "10%" }} />
                <col style={{ width: "5%" }} />
                <col style={{ width: "5%" }} />
                <col style={{ width: "5%" }} />
                <col style={{ width: "10%" }} />
                <col style={{ width: "10%" }} />
              </colgroup>
              <TableHead>
                <TableRow>
                  <StyledTableCell># Player</StyledTableCell>
                  <StyledTableCell align="center">Club</StyledTableCell>
                  <StyledTableCell align="center">Nationality</StyledTableCell>
                  <StyledTableCell align="center">Age</StyledTableCell>
                  <StyledTableCell align="center">Position</StyledTableCell>
                  <StyledTableCell align="center">Rating</StyledTableCell>
                  <StyledTableCell align="center">Goals</StyledTableCell>
                  <StyledTableCell align="center">Asissts</StyledTableCell>
                  <StyledTableCell align="center">Price</StyledTableCell>
                  <StyledTableCell align="center"></StyledTableCell>
                </TableRow>
              </TableHead>
              <HomeTable
                fetchPlayersUrl={`${apiBaseUrl}/players?page=${paginationOptions.currentPage}`}
                setPaginationOptions={setPaginationOptions}
                paginationOptions={paginationOptions}
              />
              <tfoot>
                <tr>
                  <TablePagination
                    disabled={paginationOptions.disabled}
                    style={{
                      display: "flex",
                      width: "290px",
                    }}
                    count={paginationOptions.totalCount}
                    page={paginationOptions.currentPage}
                    rowsPerPageOptions={[]}
                    rowsPerPage={paginationOptions.rowsPerPage}
                    showFirstButton
                    showLastButton
                    onPageChange={handleChangePage}
                  />
                </tr>
              </tfoot>
            </Table>
          </TableContainer>
        </div>
      </div>
    </div>
  );
};

export default Home;
