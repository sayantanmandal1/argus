// Argus landing page — tiny progressive enhancement, no dependencies.
//
// Set your GitHub repository here; every download and source link picks it up.
const REPO_URL = "https://github.com/sayantanmandal1/argus";
const RELEASES_URL = REPO_URL + "/releases/latest";
// GitHub serves the newest release's asset from this path with a
// Content-Disposition: attachment header, so linking to it triggers a real
// download without leaving the page. The names must match the stable asset
// names published by .github/workflows/release.yml.
const DOWNLOAD_BASE = REPO_URL + "/releases/latest/download";

const ASSETS = {
  windows: { file: "Argus-Setup-Windows-x64.exe", label: "Download for Windows" },
  macos: { file: "Argus-macOS.dmg", label: "Download for macOS" },
  linux: { file: "Argus-Linux-x86_64.deb", label: "Download for Linux" },
  portable: { file: "Argus-portable.jar", label: "Portable jar (any OS)" },
};

function directUrl(file) {
  return DOWNLOAD_BASE + "/" + encodeURIComponent(file);
}

function detectOS() {
  const nav = navigator;
  const hint =
    nav.userAgentData?.platform ||
    nav.platform ||
    nav.userAgent ||
    "";
  const s = hint.toLowerCase();
  if (s.includes("win")) return "windows";
  if (s.includes("mac") || s.includes("iphone") || s.includes("ipad")) return "macos";
  if (s.includes("linux") || s.includes("android") || s.includes("x11")) return "linux";
  return null;
}

const os = detectOS();

// Repo / "source" links.
document.querySelectorAll("[data-repo]").forEach((a) => {
  a.href = REPO_URL;
  a.target = "_blank";
  a.rel = "noopener";
});

// Smart primary buttons: when we can detect the OS, start a direct download of
// the right installer; otherwise fall back to the releases page.
document.querySelectorAll('[data-release="auto"]').forEach((a) => {
  if (os && ASSETS[os]) {
    a.href = directUrl(ASSETS[os].file);
    a.setAttribute("download", ASSETS[os].file);
    a.textContent = ASSETS[os].label;
  } else {
    a.href = RELEASES_URL;
    a.target = "_blank";
    a.rel = "noopener";
  }
});

// Explicit per-platform direct-download links.
document.querySelectorAll("[data-download]").forEach((a) => {
  const asset = ASSETS[a.dataset.download];
  if (!asset) return;
  a.href = directUrl(asset.file);
  a.setAttribute("download", asset.file);
});

// "All releases" fallback link (data-release present, but not the auto button).
document
  .querySelectorAll("[data-release]:not([data-release='auto'])")
  .forEach((a) => {
    a.href = RELEASES_URL;
    a.target = "_blank";
    a.rel = "noopener";
  });

const yearEl = document.getElementById("year");
if (yearEl) {
  yearEl.textContent = "\u00A9 " + new Date().getFullYear();
}

// --- Visual polish: progressive enhancement, no dependencies ----------------
(function enhance() {
  const root = document.documentElement;
  root.classList.add("js");

  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // Glass navigation once the page is scrolled.
  const nav = document.querySelector(".nav");
  if (nav) {
    const onScroll = () => nav.classList.toggle("scrolled", window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
  }

  // Count-up animation for the hero stats.
  document.querySelectorAll(".stats dt[data-count]").forEach((el) => {
    const target = Number.parseInt(el.dataset.count, 10) || 0;
    const suffix = el.dataset.suffix || "";
    if (reduce || target === 0) {
      el.textContent = target + suffix;
      return;
    }
    const duration = 1100;
    const startTime = performance.now();
    el.textContent = "0" + suffix;
    const step = (now) => {
      const t = Math.min(1, (now - startTime) / duration);
      const eased = 1 - Math.pow(1 - t, 3);
      el.textContent = Math.round(target * eased) + suffix;
      if (t < 1) {
        requestAnimationFrame(step);
      }
    };
    requestAnimationFrame(step);
  });

  const revealTargets = document.querySelectorAll("section, .reveal-group > *");

  if (reduce || !("IntersectionObserver" in window)) {
    revealTargets.forEach((el) => el.classList.add("in"));
    return;
  }

  // Stagger grouped items by their position within the group.
  document.querySelectorAll(".reveal-group").forEach((group) => {
    Array.from(group.children).forEach((child, i) => {
      child.style.transitionDelay = Math.min(i * 70, 420) + "ms";
    });
  });

  const io = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("in");
          io.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.12, rootMargin: "0px 0px -8% 0px" }
  );
  revealTargets.forEach((el) => io.observe(el));

  // Subtle pointer-driven tilt on the hero mockup.
  const visual = document.querySelector(".hero-visual");
  const mock = document.querySelector(".browser-mock");
  if (visual && mock) {
    visual.addEventListener("pointermove", (e) => {
      const r = visual.getBoundingClientRect();
      const dx = (e.clientX - r.left) / r.width - 0.5;
      const dy = (e.clientY - r.top) / r.height - 0.5;
      mock.style.transform = `rotateY(${dx * 8}deg) rotateX(${-dy * 8}deg) translateY(-4px)`;
    });
    visual.addEventListener("pointerleave", () => {
      mock.style.transform = "";
    });
  }
})();
