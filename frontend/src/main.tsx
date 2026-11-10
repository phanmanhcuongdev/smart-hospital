import React from "react";
import ReactDOM from "react-dom/client";
import { EmrApp } from "./emr/EmrApp";
import "./styles.css";
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <EmrApp />
  </React.StrictMode>,
);
