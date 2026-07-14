# argus-web

The marketing / landing site for **Argus** — a from-scratch search engine and browser engine in Java.
It is a static site (HTML, CSS, and a few lines of vanilla JavaScript) with no build step, so it
deploys to Vercel (or any static host) as-is.

## Structure

```
argus-web/
  index.html          # the single-page site
  assets/
    styles.css        # theme + layout
    app.js            # sets the GitHub/release links, footer year
    screenshot-*.png  # real output from the Argus rendering engine
  vercel.json         # clean URLs + security headers + asset caching
```

## Configure

Open `assets/app.js` and set your repository:

```js
const REPO_URL = "https://github.com/your-username/argus";
```

The download and "source" buttons are wired to `${REPO_URL}` and `${REPO_URL}/releases/latest`.

## Deploy to Vercel

1. Push this repository to GitHub.
2. In Vercel, **Add New → Project** and import the repo.
3. Set the **Root Directory** to `argus-web`.
4. Framework preset: **Other** (no build command, output directory is the root).
5. Deploy.

The screenshots under `assets/` are genuine renders produced by the Argus engine (a live HTTP site,
a JavaScript-built page, and a styled box-model page) — not mockups.
