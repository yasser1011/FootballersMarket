import React from "react";
import {
  CircularProgress,
  TableBody,
  TableCell,
  TableRow,
} from "@mui/material";
import { useState } from "react";
import { useOnMountUnsafe } from "../customhooks/useOnMountUnsafe";
import axios from "axios";
import { apiBaseUrl } from "../config/Config";

const UserRankingTableBody = () => {
  const FETCH_STATUS = {
    ERROR: "error",
    SUCCESS: "success",
    LOADING: "loading",
  };

  const [users, setUsers] = useState([]);
  const [fetchStatus, setFetchStatus] = useState(FETCH_STATUS.LOADING);

  const fetchUsers = async () => {
    try {
      let usersRes = await axios.get(`${apiBaseUrl}/users/rankings`);
      if (usersRes && usersRes.data) {
        setUsers(usersRes.data);
        setFetchStatus(FETCH_STATUS.SUCCESS);
      }
    } catch (error) {
      setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  useOnMountUnsafe(() => {
    fetchUsers();
  }, []);

  if (fetchStatus === FETCH_STATUS.LOADING) {
    return (
      <>
        <TableBody>
          <TableRow>
            <TableCell align="center" colSpan={3}>
              <CircularProgress />
            </TableCell>
          </TableRow>
        </TableBody>
      </>
    );
  }

  if (fetchStatus === FETCH_STATUS.ERROR) {
    return (
      <>
        <TableBody>
          <TableRow>
            <TableCell align="center" colSpan={3}>
              Error Please Try Again Later
            </TableCell>
          </TableRow>
        </TableBody>
      </>
    );
  }
  return (
    users.length > 0 && (
      <TableBody>
        {users.map((user, idx) => (
          <TableRow key={user.id}>
            <TableCell>{idx + 1}</TableCell>
            <TableCell>{user.username}</TableCell>
            <TableCell>{user.points}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    )
  );
};

export default UserRankingTableBody;
