# 📡 Procucev AI — REST API Specification & Endpoint Reference

Base URL: `http://localhost:8080` (or `https://<deployed-url>`)

---

## 1. Transcription CRUD Endpoints

### 1.1 List Transcriptions (Paginated & Filterable)
- **Method:** `GET`
- **Path:** `/api/transcriptions`
- **Query Parameters:**
  - `keyword` *(optional)*: Filter by title, content, or notes.
  - `language` *(optional)*: Filter by language (e.g. `English`, `Hinglish`).
  - `favorite` *(optional, boolean)*: `true` to filter favorites only.
  - `page` *(default: 0)*: Zero-indexed page number.
  - `size` *(default: 10)*: Number of items per page.
  - `sortBy` *(default: "createdAt")*: Field to sort by.
  - `sortDir` *(default: "desc")*: Sort direction (`asc` or `desc`).

```bash
curl -X GET "http://localhost:8080/api/transcriptions?page=0&size=5&sortDir=desc" \
     -H "Accept: application/json"
```

---

### 1.2 Create Transcription
- **Method:** `POST`
- **Path:** `/api/transcriptions`
- **Headers:** `Content-Type: application/json`

```bash
curl -X POST "http://localhost:8080/api/transcriptions" \
     -H "Content-Type: application/json" \
     -d '{
       "title": "Daily Engineering Standup",
       "content": "Aaj hum log WebSocket latency optimize karenge and check the database migrations.",
       "notes": "Action items for deployment sprint.",
       "tags": "standup,websocket,engineering",
       "provider": "browser-live-speech"
     }'
```

---

### 1.3 Get Single Transcription
- **Method:** `GET`
- **Path:** `/api/transcriptions/{id}`

```bash
curl -X GET "http://localhost:8080/api/transcriptions/1"
```

---

### 1.4 Update Transcription
- **Method:** `PUT`
- **Path:** `/api/transcriptions/{id}`

```bash
curl -X PUT "http://localhost:8080/api/transcriptions/1" \
     -H "Content-Type: application/json" \
     -d '{
       "title": "Updated Meeting Notes",
       "content": "Updated transcription text content.",
       "notes": "Reviewed and finalized.",
       "tags": "finalized,meeting"
     }'
```

---

### 1.5 Toggle Favorite
- **Method:** `PATCH`
- **Path:** `/api/transcriptions/{id}/favorite`

```bash
curl -X PATCH "http://localhost:8080/api/transcriptions/1/favorite"
```

---

### 1.6 Delete Transcription
- **Method:** `DELETE`
- **Path:** `/api/transcriptions/{id}`

```bash
curl -X DELETE "http://localhost:8080/api/transcriptions/1"
```

---

### 1.7 Batch Delete
- **Method:** `POST`
- **Path:** `/api/transcriptions/batch-delete`

```bash
curl -X POST "http://localhost:8080/api/transcriptions/batch-delete" \
     -H "Content-Type: application/json" \
     -d '{"ids": [1, 2, 3]}'
```

---

### 1.8 Export Transcriptions
- **Method:** `GET`
- **Path:** `/api/transcriptions/export`
- **Query Parameters:** `format` (`json`, `csv`, `txt`, `srt`)

```bash
curl -X GET "http://localhost:8080/api/transcriptions/export?format=csv" -O
```

---

## 2. Speech & AI Refinement Endpoints

### 2.1 Transcribe Audio File
- **Method:** `POST`
- **Path:** `/api/speech/transcribe-audio`
- **Headers:** `X-Gemini-API-Key` *(optional)*

```bash
curl -X POST "http://localhost:8080/api/speech/transcribe-audio" \
     -H "X-Gemini-API-Key: AIzaSyYourKeyHere" \
     -F "file=@sample.webm" \
     -F "language=hinglish"
```

---

### 2.2 AI Hinglish Refinement
- **Method:** `POST`
- **Path:** `/api/speech/polish`
- **Modes:** `clean`, `devanagari`, `romanized`, `english_translation`, `punctuate`

```bash
curl -X POST "http://localhost:8080/api/speech/polish" \
     -H "Content-Type: application/json" \
     -H "X-Gemini-API-Key: AIzaSyYourKeyHere" \
     -d '{
       "text": "kal meeting me hum decide karenge ki kab deploy karna hai",
       "mode": "devanagari"
     }'
```
