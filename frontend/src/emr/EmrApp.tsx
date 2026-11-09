import { useEffect, useState } from "react";
import { EncounterDetailPage } from "./EncounterDetailPage";
import { EncounterListPage } from "./EncounterListPage";
import "./emr.css";

const DETAIL_ROUTE = /^#\/encounters\/([0-9a-fA-F-]{36})$/;

function useHash(): string {
  const [hash, setHash] = useState(window.location.hash);
  useEffect(() => {
    const onChange = () => setHash(window.location.hash);
    window.addEventListener("hashchange", onChange);
    return () => window.removeEventListener("hashchange", onChange);
  }, []);
  return hash;
}

export function EmrApp() {
  const match = DETAIL_ROUTE.exec(useHash());
  return (
    <main className="emr">
      <h1>SmartHMS — Khám bệnh và hồ sơ bệnh án</h1>
      {match?.[1] ? (
        <EncounterDetailPage key={match[1]} encounterId={match[1]} />
      ) : (
        <EncounterListPage />
      )}
    </main>
  );
}
