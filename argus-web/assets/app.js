// Argus landing page — tiny progressive enhancement, no dependencies.
//
// Set your GitHub repository here; the download and source links pick it up.
const REPO_URL = "https://github.com/sayantanmandal1/argus";
const RELEASES_URL = REPO_URL + "/releases/latest";

document.querySelectorAll("[data-repo]").forEach((a) => {
  a.href = REPO_URL;
  a.target = "_blank";
  a.rel = "noopener";
});

document.querySelectorAll("[data-release]").forEach((a) => {
  a.href = RELEASES_URL;
  a.target = "_blank";
  a.rel = "noopener";
});

const yearEl = document.getElementById("year");
if (yearEl) {
  yearEl.textContent = "\u00A9 " + new Date().getFullYear();
}
