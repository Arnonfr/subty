# API Reference - Subtitles & Movie Data

This document summarizes the technical details for integrating subtitle and movie data services.

## 1. OpenSubtitles.com (Recommended)
The most comprehensive subtitle database.
- **Base URL**: `https://api.opensubtitles.com/api/v1/`
- **Authentication**: Requires an `Api-Key` header. Get it from [OpenSubtitles API Consumers](https://www.opensubtitles.com/en/consumers).
- **Required Headers**:
  - `Api-Key`: `{YOUR_API_KEY}`
  - `User-Agent`: `YourAppName v1.0` (Must be unique)
- **Key Endpoints**:
  - `GET /subtitles`: Search for subtitles. Parameters include `query`, `imdb_id`, `tmdb_id`, `moviehash`, `languages`.
  - `POST /download`: Get the download link (Requires identifying the file ID from the search result).

## 2. TMDB (The Movie Database)
Best for movie/TV show metadata, posters, and autocomplete.
- **Base URL**: `https://api.themoviedb.org/3/`
- **Authentication**: `api_key` query param or `Authorization: Bearer {TOKEN}` header.
- **Key Endpoints**:
  - `GET /search/movie`: Autocomplete/Search. Returns `id` and `poster_path`.
  - `GET /movie/{movie_id}/external_ids`: Get `imdb_id` (Crucial for OpenSubtitles).
- **Image URLs**: `https://image.tmdb.org/t/p/w500/{poster_path}` (w500, w200, original, etc.)

## 3. SubDB
Hash-based search, extremely accurate for specific video files.
- **Base URL**: `http://api.thesubdb.com/`
- **Authentication**: No API Key required.
- **Mandatory**: Must set a `User-Agent` identifying your app.
- **Logic**: You must calculate a 128KB hash of the video file (the first and last 64KB).

## 4. Podnapisi
- **Base URL**: `https://www.podnapisi.net/`
- **Format**: JSON Search via `Accept: application/json` header.
- **Logic**: Good for European content.

## 5. Addic7ed & Subscene
These do not have official APIs.
- **Method**: Scraping only (not recommended for production).
- **Subscene Alternative**: OpenSubtitles often has the same content.
