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

const UserRankingTableBody = ({ selectedUserId, onSelectUser }) => {
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
        // the endpoint returns users unsorted; rank them by score (points) descending so the
        // row number reflects the actual ranking
        const ranked = [...usersRes.data].sort((a, b) => b.points - a.points);
        setUsers(ranked);
        setFetchStatus(FETCH_STATUS.SUCCESS);
        // arrived with a preselected user (e.g. from the home Top Users card): resolve its
        // username so the history panel can title it + highlight the row
        if (selectedUserId && onSelectUser) {
          const preselected = ranked.find(
            (u) => String(u.userId) === String(selectedUserId),
          );
          if (preselected) onSelectUser(preselected);
        }
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
        {users.map((user, idx) => {
          const isSelected = String(user.userId) === String(selectedUserId);
          return (
            <TableRow
              key={user.userId}
              hover
              selected={isSelected}
              onClick={() => onSelectUser && onSelectUser(user)}
              style={{ cursor: "pointer" }}
            >
              <TableCell>{idx + 1}</TableCell>
              <TableCell>{user.username}</TableCell>
              <TableCell>{user.points}</TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    )
  );
};

export default UserRankingTableBody;
