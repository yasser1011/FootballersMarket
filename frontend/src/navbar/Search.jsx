import React, { useEffect, useRef, useState } from "react";
import "./Navbar.css";
import SearchPlayersList from "./SearchPlayersList";
import useOnClickOutside from "./useOnClickOutside";

const Search = () => {
  const [showSearchList, setShowSearchList] = useState(false);

  const searchRef = useRef();

  const [searchInputValue, setSearchInputValue] = useState("");
  const [debouncedInputValue, setDebouncedInputValue] = useState("");

  const handleInputChange = (event) => {
    setSearchInputValue(event.target.value);
  };

  useEffect(() => {
    // if you don't remove old timeout debounce value will get changed every time
    // which will affect query text as it will change and will exceute api every time
    // here we're removing the old timeout which is helpful when you're typing fast
    // only last settimeout will be executed so debounce value will change one time
    if (!searchInputValue) return;
    const timeoutId = setTimeout(() => {
      setDebouncedInputValue(searchInputValue);
      setShowSearchList(true);
    }, 1000);
    // effect will be cleaned when component unmount
    // which is every time searchInputValue is changed
    // and will be executed before running new effect
    return () => clearTimeout(timeoutId);
  }, [searchInputValue]);

  // to remove search block when clicking outside it
  useOnClickOutside(searchRef, () => setShowSearchList(false));

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <form>
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0"
        />
        <div className="search">
          <span className="search-icon material-symbols-outlined">search</span>
          <input
            value={searchInputValue}
            onChange={handleInputChange}
            className="search-input"
            type="search"
            placeholder="Search Player"
          />
        </div>
      </form>

      {showSearchList && debouncedInputValue && (
        <div ref={searchRef} className="search-result-box">
          <SearchPlayersList
            queryText={debouncedInputValue}
            setShowSearchList={setShowSearchList}
            setSearchInputValue={setSearchInputValue}
          />
        </div>
      )}
    </div>
  );
};

export default Search;
