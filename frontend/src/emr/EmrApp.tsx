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

export interface EmrAppProps {
  encounterId?: string;
  onBackToList?: () => void;
}

export function EmrApp({ encounterId, onBackToList }: EmrAppProps = {}) {
  const hash = useHash();
  const match = DETAIL_ROUTE.exec(hash);
  const activeEncounterId = encounterId ?? match?.[1];

  return (
    <section className="emr" aria-label="Khám bệnh và hồ sơ bệnh án">
      {activeEncounterId ? (
        <EncounterDetailPage
          key={activeEncounterId}
          encounterId={activeEncounterId}
          onBack={onBackToList}
        />
      ) : (
        <EncounterListPage />
      )}
    </section>
  );
}
