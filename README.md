# 📚 Pianificazione delle Ore di Studio – Ottimizzazione con Gurobi

## Descrizione

Questo progetto affronta un problema di **ottimizzazione della pianificazione dello studio** per un gruppo di studenti di Ingegneria. L’obiettivo è determinare la distribuzione ottimale del tempo di studio su più materie, rispettando vincoli legati a durata giornaliera, numero di materie per giorno, ore minime per materia e massimizzazione dello studio di una materia preferita.

Il problema è stato modellato come un **modello di Programmazione Lineare Intera (MPLI)** e risolto tramite **Gurobi Optimizer**.

---

## 🧠 Obiettivi

- Pianificare `d` giorni di studio su `n` materie diverse
- Ogni materia `i` deve essere studiata almeno `ti` ore totali
- Se una materia è studiata in un giorno, deve esserlo per almeno `τi` ore
- Non si vogliono studiare più di `l` materie al giorno
- Non si vogliono superare `tmax` ore giornaliere di studio
- Massimizzare le ore totali dedicate a una materia preferita `k`

---

## 🧰 Tecnologie utilizzate

- **Gurobi Optimizer** (con Gurobi Python API)
- **Modellazione PL/PLI**
- File `.txt` per lettura parametri del problema

---

## ✳️ Modellazione e Vincoli Principali

- **Vincolo 1**: somma delle ore per ogni materia ≥ `ti`
- **Vincolo 2**: se studiata, materia `i` deve essere studiata almeno `τi` ore
- **Vincolo 3**: disgiunzione `xij ≤ M*yij`
- **Vincolo 4**: massimo `l` materie al giorno
- **Vincolo 5**: massimo `tmax` ore giornaliere

### Funzione obiettivo:
Massimizzare la somma delle ore di studio sulla materia `k`.

---

## 🔄 Estensioni al modello

- **Vincolo 6**: Non studiare la stessa materia per più di due giorni consecutivi
- **Vincolo 7**: Studiare una materia `a` solo se il giorno prima non si è studiato né `b` né `c`

---

## 🧪 Analisi del rilassamento (MPL)

- Analisi dei vincoli attivi all’ottimo
- Verifica della degenerazione della soluzione
- Controllo unicità della soluzione
- Confronto tra soluzione intera e rilassata

---

