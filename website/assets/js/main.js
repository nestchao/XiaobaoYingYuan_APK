/* ==========================================================================
   小宝影院 · Aurora — landing page interactions
   No dependencies. Everything below is progressive enhancement.
   ========================================================================== */
(() => {
  "use strict";

  const root = document.documentElement;
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const finePointer = window.matchMedia("(hover: hover) and (pointer: fine)").matches;

  const $ = (sel, ctx = document) => ctx.querySelector(sel);
  const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));
  const clamp = (v, min, max) => Math.min(max, Math.max(min, v));
  const lerp = (a, b, t) => a + (b - a) * t;

  /* ── Theme ────────────────────────────────────────────────────────────── */
  const THEME_KEY = "xiaobao-theme";
  const metaTheme = document.querySelector('meta[name="theme-color"]');
  const themeToggle = $("#themeToggle");
  const themeSwitch = $("#themeSwitch");
  const themeSwitchLabel = $("#themeSwitchLabel");

  const storedTheme = (() => {
    try {
      return localStorage.getItem(THEME_KEY);
    } catch (e) {
      return null;
    }
  })();

  let currentTheme =
    storedTheme || (window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark");

  function applyTheme(theme, persist = true) {
    currentTheme = theme;
    root.dataset.theme = theme;
    if (metaTheme) metaTheme.setAttribute("content", theme === "dark" ? "#06090F" : "#F4F6FB");
    if (themeSwitch) {
      themeSwitch.setAttribute("aria-checked", String(theme === "dark"));
    }
    if (themeSwitchLabel) themeSwitchLabel.textContent = theme === "dark" ? "深色" : "浅色";
    if (themeToggle) {
      themeToggle.setAttribute("aria-label", theme === "dark" ? "切换到浅色主题" : "切换到深色主题");
    }
    if (persist) {
      try {
        localStorage.setItem(THEME_KEY, theme);
      } catch (e) {
        /* private mode */
      }
    }
    window.dispatchEvent(new CustomEvent("themechange", { detail: { theme } }));
  }

  applyTheme(currentTheme, false);

  function toggleTheme(originEvent) {
    const next = currentTheme === "dark" ? "light" : "dark";
    const prefersReduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    if (document.startViewTransition && !prefersReduced) {
      const x = originEvent && originEvent.clientX ? originEvent.clientX : window.innerWidth / 2;
      const y = originEvent && originEvent.clientY ? originEvent.clientY : window.innerHeight / 2;
      root.style.setProperty("--vt-x", `${x}px`);
      root.style.setProperty("--vt-y", `${y}px`);
      const transition = document.startViewTransition(() => applyTheme(next));
      transition.finished.finally(() => {
        root.style.removeProperty("--vt-x");
        root.style.removeProperty("--vt-y");
      });
    } else {
      applyTheme(next);
    }
  }

  themeToggle && themeToggle.addEventListener("click", (e) => toggleTheme(e));
  themeSwitch && themeSwitch.addEventListener("click", (e) => toggleTheme(e));

  // Follow the OS only while the visitor hasn't chosen a theme explicitly.
  window.matchMedia("(prefers-color-scheme: light)").addEventListener("change", (e) => {
    if (!storedTheme && !localStorage.getItem(THEME_KEY)) {
      applyTheme(e.matches ? "light" : "dark", false);
    }
  });

  /* ── Aurora canvas ────────────────────────────────────────────────────── */
  const canvas = $("#aurora");
  const ctx = canvas ? canvas.getContext("2d") : null;
  let cw = 0;
  let ch = 0;
  let dpr = 1;
  let themeColors = null;
  let pointerX = 0.5;
  let pointerY = 0.5;
  let smoothX = 0.5;
  let smoothY = 0.5;
  let rafId = 0;
  let startTime = performance.now();

  function readGlowColors() {
    const cs = getComputedStyle(root);
    return {
      a: cs.getPropertyValue("--glow-a").trim() || "rgba(61,90,254,0.55)",
      b: cs.getPropertyValue("--glow-b").trim() || "rgba(139,92,246,0.45)",
      c: cs.getPropertyValue("--glow-c").trim() || "rgba(34,211,238,0.22)",
      light: root.dataset.theme === "light",
    };
  }

  function resizeCanvas() {
    if (!canvas || !ctx) return;
    dpr = Math.min(window.devicePixelRatio || 1, 1.75);
    cw = window.innerWidth;
    ch = window.innerHeight;
    canvas.width = Math.floor(cw * dpr);
    canvas.height = Math.floor(ch * dpr);
    canvas.style.width = `${cw}px`;
    canvas.style.height = `${ch}px`;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    themeColors = readGlowColors();
    if (reduceMotion) drawFrame(0);
  }

  function blob(x, y, r, color) {
    if (!ctx) return;
    const g = ctx.createRadialGradient(x, y, 0, x, y, r);
    g.addColorStop(0, color);
    g.addColorStop(1, "rgba(0,0,0,0)");
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.fill();
  }

  function drawFrame(t) {
    if (!ctx || !themeColors) return;
    const time = t / 1000;
    ctx.clearRect(0, 0, cw, ch);
    ctx.globalCompositeOperation = themeColors.light ? "source-over" : "lighter";

    const base = Math.max(cw, ch);
    const px = (smoothX - 0.5) * 90;
    const py = (smoothY - 0.5) * 70;

    const a1x = cw * (0.22 + Math.sin(time * 0.11) * 0.06) + px;
    const a1y = ch * (0.12 + Math.cos(time * 0.09) * 0.05) + py;
    blob(a1x, a1y, base * 0.62, themeColors.a);

    const b1x = cw * (0.82 + Math.cos(time * 0.08) * 0.07) - px * 0.6;
    const b1y = ch * (0.24 + Math.sin(time * 0.13) * 0.06) + py * 0.8;
    blob(b1x, b1y, base * 0.55, themeColors.b);

    const c1x = cw * (0.58 + Math.sin(time * 0.07 + 2) * 0.08) + px * 0.4;
    const c1y = ch * (0.86 + Math.cos(time * 0.1 + 1) * 0.05);
    blob(c1x, c1y, base * 0.5, themeColors.c);

    ctx.globalCompositeOperation = "source-over";
  }

  function loop(now) {
    if (!document.hidden && !reduceMotion) {
      smoothX = lerp(smoothX, pointerX, 0.045);
      smoothY = lerp(smoothY, pointerY, 0.045);
      drawFrame(now - startTime);
    }
    rafId = requestAnimationFrame(loop);
  }

  if (canvas && ctx) {
    resizeCanvas();
    if (reduceMotion) {
      drawFrame(0);
    } else {
      rafId = requestAnimationFrame(loop);
    }
    window.addEventListener("resize", resizeCanvas, { passive: true });
    window.addEventListener(
      "pointermove",
      (e) => {
        pointerX = e.clientX / Math.max(1, window.innerWidth);
        pointerY = e.clientY / Math.max(1, window.innerHeight);
      },
      { passive: true }
    );
    window.addEventListener("themechange", () => {
      themeColors = readGlowColors();
      if (reduceMotion) drawFrame(0);
    });
    document.addEventListener("visibilitychange", () => {
      if (document.hidden) {
        cancelAnimationFrame(rafId);
      } else if (!reduceMotion) {
        startTime = performance.now();
        rafId = requestAnimationFrame(loop);
      }
    });
  }

  /* ── Reveal on scroll ─────────────────────────────────────────────────── */
  const revealEls = $$("[data-reveal]");
  // Stagger siblings that reveal together.
  const byParent = new Map();
  revealEls.forEach((el) => {
    const parent = el.parentElement;
    if (!byParent.has(parent)) byParent.set(parent, []);
    byParent.get(parent).push(el);
  });
  byParent.forEach((group) => {
    if (group.length < 2) return;
    group.forEach((el, i) => {
      if (!el.style.getPropertyValue("--d")) {
        el.style.setProperty("--d", `${Math.min(i * 80, 360)}ms`);
      }
    });
  });

  if ("IntersectionObserver" in window && !reduceMotion) {
    const revealIO = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("in-view");
            revealIO.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -6% 0px" }
    );
    revealEls.forEach((el) => revealIO.observe(el));
  } else {
    revealEls.forEach((el) => el.classList.add("in-view"));
  }

  /* ── Navigation ───────────────────────────────────────────────────────── */
  const nav = $("#nav");
  const navBurger = $("#navBurger");
  const mobileMenu = $("#mobileMenu");
  const scrollProgress = $("#scrollProgress");
  const toTop = $("#toTop");
  const navLinks = $$("[data-nav]");

  let lastScrollY = window.scrollY;
  let navTicking = false;

  function onScroll() {
    const y = window.scrollY;
    nav && nav.classList.toggle("is-scrolled", y > 24);
    if (nav && !mobileMenu?.hidden) nav.classList.remove("is-hidden");
    else if (nav) {
      if (y > 220 && y > lastScrollY + 4) nav.classList.add("is-hidden");
      else if (y < lastScrollY - 4) nav.classList.remove("is-hidden");
    }
    lastScrollY = y;

    if (scrollProgress) {
      const max = document.documentElement.scrollHeight - window.innerHeight;
      scrollProgress.style.width = `${max > 0 ? clamp((y / max) * 100, 0, 100) : 0}%`;
    }
    if (toTop) toTop.classList.toggle("show", y > 640);
    if (y < 260) navLinks.forEach((link) => link.classList.remove("active"));
    navTicking = false;
  }

  window.addEventListener(
    "scroll",
    () => {
      if (!navTicking) {
        navTicking = true;
        requestAnimationFrame(onScroll);
      }
    },
    { passive: true }
  );
  onScroll();

  toTop && toTop.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));

  function setMenu(open) {
    if (!mobileMenu || !navBurger) return;
    mobileMenu.hidden = !open;
    navBurger.setAttribute("aria-expanded", String(open));
    navBurger.innerHTML = open
      ? '<svg class="icon" aria-hidden="true"><use href="#i-close" /></svg>'
      : '<svg class="icon" aria-hidden="true"><use href="#i-menu" /></svg>';
    navBurger.setAttribute("aria-label", open ? "关闭菜单" : "打开菜单");
  }

  navBurger &&
    navBurger.addEventListener("click", () => setMenu(mobileMenu ? mobileMenu.hidden : false));
  $$("#mobileMenu a").forEach((a) => a.addEventListener("click", () => setMenu(false)));
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") setMenu(false);
  });

  // Active section highlighting.
  const sections = navLinks
    .map((link) => {
      const id = link.getAttribute("href");
      return id && id.startsWith("#") ? document.querySelector(id) : null;
    })
    .filter(Boolean);

  if ("IntersectionObserver" in window && sections.length) {
    const sectionIO = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          const id = `#${entry.target.id}`;
          navLinks.forEach((link) =>
            link.classList.toggle("active", link.getAttribute("href") === id)
          );
        });
      },
      { rootMargin: "-42% 0px -52% 0px", threshold: 0 }
    );
    sections.forEach((s) => sectionIO.observe(s));
  }

  /* ── Magnetic buttons ─────────────────────────────────────────────────── */
  if (finePointer && !reduceMotion) {
    $$(".magnetic").forEach((el) => {
      el.addEventListener("pointermove", (e) => {
        const r = el.getBoundingClientRect();
        const x = (e.clientX - r.left - r.width / 2) / (r.width / 2);
        const y = (e.clientY - r.top - r.height / 2) / (r.height / 2);
        el.style.setProperty("--mx", String(clamp(x, -1, 1) * 4));
        el.style.setProperty("--my", String(clamp(y, -1, 1) * 3));
      });
      el.addEventListener("pointerleave", () => {
        el.style.setProperty("--mx", "0");
        el.style.setProperty("--my", "0");
      });
    });
  }

  /* ── Bento spotlight ──────────────────────────────────────────────────── */
  if (finePointer) {
    $$(".card").forEach((card) => {
      card.addEventListener("pointermove", (e) => {
        const r = card.getBoundingClientRect();
        card.style.setProperty("--sx", `${e.clientX - r.left}px`);
        card.style.setProperty("--sy", `${e.clientY - r.top}px`);
      });
    });
  }

  /* ── Hero device tilt ─────────────────────────────────────────────────── */
  const heroVisual = $(".hero-visual");
  const heroPhone = $("#heroPhone");
  if (heroVisual && heroPhone && finePointer && !reduceMotion) {
    let targetRX = 0;
    let targetRY = 0;
    let curRX = 0;
    let curRY = 0;
    let tiltRaf = 0;

    const tiltLoop = () => {
      curRX = lerp(curRX, targetRX, 0.12);
      curRY = lerp(curRY, targetRY, 0.12);
      heroPhone.style.transform = `rotateX(${curRX.toFixed(3)}deg) rotateY(${curRY.toFixed(3)}deg)`;
      if (Math.abs(curRX - targetRX) > 0.01 || Math.abs(curRY - targetRY) > 0.01) {
        tiltRaf = requestAnimationFrame(tiltLoop);
      } else {
        tiltRaf = 0;
      }
    };
    const kick = () => {
      if (!tiltRaf) tiltRaf = requestAnimationFrame(tiltLoop);
    };

    heroVisual.addEventListener("pointermove", (e) => {
      const r = heroVisual.getBoundingClientRect();
      const nx = (e.clientX - r.left) / r.width - 0.5;
      const ny = (e.clientY - r.top) / r.height - 0.5;
      targetRY = nx * 9;
      targetRX = -ny * 7;
      kick();
    });
    heroVisual.addEventListener("pointerleave", () => {
      targetRX = 0;
      targetRY = 0;
      kick();
    });
  }

  /* ── Hero screenshot carousel ─────────────────────────────────────────── */
  const heroShotsWrap = $("#heroShots");
  const heroDotsWrap = $("#heroDots");
  const heroLabel = $("#heroShotLabel");
  const heroPrev = $("#heroPrev");
  const heroNext = $("#heroNext");

  if (heroShotsWrap && heroDotsWrap) {
    const heroShots = $$(".shot", heroShotsWrap);
    let heroIndex = 0;
    let heroTimer = 0;
    let heroPaused = false;
    const HERO_INTERVAL = 4600;

    heroShots.forEach((shot, i) => {
      const dot = document.createElement("button");
      dot.type = "button";
      dot.setAttribute("role", "tab");
      dot.setAttribute("aria-label", `第 ${i + 1} 张截图`);
      dot.setAttribute("aria-selected", String(i === 0));
      if (i === 0) dot.classList.add("is-active");
      dot.addEventListener("click", () => {
        goHero(i);
        restartHero();
      });
      heroDotsWrap.appendChild(dot);
    });
    const heroDots = $$("button", heroDotsWrap);

    function goHero(index) {
      heroIndex = (index + heroShots.length) % heroShots.length;
      heroShots.forEach((shot, i) => shot.classList.toggle("is-active", i === heroIndex));
      heroDots.forEach((dot, i) => {
        dot.classList.toggle("is-active", i === heroIndex);
        dot.setAttribute("aria-selected", String(i === heroIndex));
      });
      if (heroLabel) {
        heroLabel.textContent = heroShots[heroIndex].dataset.label || "";
      }
    }

    function nextHero() {
      goHero(heroIndex + 1);
    }
    function restartHero() {
      clearInterval(heroTimer);
      if (heroPaused || reduceMotion) return;
      heroTimer = window.setInterval(nextHero, HERO_INTERVAL);
    }

    heroPrev && heroPrev.addEventListener("click", () => { goHero(heroIndex - 1); restartHero(); });
    heroNext && heroNext.addEventListener("click", () => { goHero(heroIndex + 1); restartHero(); });

    const heroFrame = $(".hero-phone");
    if (heroFrame) {
      heroFrame.addEventListener("pointerenter", () => { heroPaused = true; clearInterval(heroTimer); });
      heroFrame.addEventListener("pointerleave", () => { heroPaused = false; restartHero(); });
    }

    // Touch swipe.
    let touchStartX = null;
    heroShotsWrap.addEventListener("touchstart", (e) => { touchStartX = e.touches[0].clientX; }, { passive: true });
    heroShotsWrap.addEventListener("touchend", (e) => {
      if (touchStartX == null) return;
      const dx = e.changedTouches[0].clientX - touchStartX;
      if (Math.abs(dx) > 40) {
        goHero(heroIndex + (dx < 0 ? 1 : -1));
        restartHero();
      }
      touchStartX = null;
    });

    document.addEventListener("visibilitychange", () => {
      if (document.hidden) clearInterval(heroTimer);
      else restartHero();
    });

    restartHero();
  }

  /* ── Showcase ─────────────────────────────────────────────────────────── */
  const SCREEN_ORDER = ["home", "detail", "player", "category", "search", "history", "settings"];
  const SCREENS = {
    home: {
      title: "首页 · 影院级开场",
      desc: "自动轮播的无限英雄海报、视差图层、继续观看与内容轨道。下拉即可刷新，首屏即是精选。",
      tags: ["英雄轮播", "视差图层", "继续观看", "下拉刷新"],
      phone: "phone-home-light.webp",
      tablet: "tablet-home-light.webp",
    },
    detail: {
      title: "详情 · 海报优先",
      desc: "全幅视差背景、悬浮玻璃返回键、评分与元信息胶囊、可展开简介，以及自适应的选集网格。",
      tags: ["视差背景", "玻璃返回键", "可展开简介", "选集网格"],
      phone: "phone-detail-light.webp",
      tablet: "tablet-detail-light.webp",
    },
    player: {
      title: "播放器 · 烟熏玻璃控件",
      desc: "深色玻璃控制面板、自绘缓冲进度、倍速与选集抽屉、亮度/音量手势，以及自动连播倒计时。",
      tags: ["自绘进度条", "选集抽屉", "手势控制", "自动连播"],
      phone: "phone-player.webp",
      tablet: "tablet-player.webp",
    },
    category: {
      title: "分类 · 流畅筛选",
      desc: "动画筛选胶囊、Paging 3 无限滚动、骨架屏与空/错状态；平板自动扩展列数。",
      tags: ["筛选胶囊", "无限滚动", "骨架屏", "自适应列数"],
      phone: "phone-category.webp",
      tablet: "tablet-category-light.webp",
    },
    search: {
      title: "搜索 · 发现之旅",
      desc: "自定义极光搜索框、最近与热门搜索、渐变排名徽章、无限加载与输入法避让。",
      tags: ["极光搜索框", "热门排名", "无限加载", "输入法避让"],
      phone: "phone-search.webp",
      tablet: "tablet-search-light.webp",
    },
    history: {
      title: "历史 · 记录每一次心动",
      desc: "按日期分组、渐变进度、滑动删除与撤销、清除全部确认，进度一目了然。",
      tags: ["日期分组", "滑动删除", "撤销", "进度环"],
      phone: "phone-history.webp",
      tablet: "tablet-history-light.webp",
    },
    settings: {
      title: "设置 · 主题工作室",
      desc: "跟随系统/浅色/深色实时预览，自动连播开关与关于信息，设置即刻生效。",
      tags: ["主题预览", "即时生效", "自动连播", "关于"],
      phone: "phone-settings.webp",
      tablet: "tablet-settings-dark.webp",
    },
  };

  const screenTabs = $("#screenTabs");
  const showcasePhone = $("#showcasePhone");
  const showcaseTablet = $("#showcaseTablet");
  const showcasePhoneShots = $("#showcaseShots");
  const showcaseTabletShots = $("#showcaseTabletShots");
  const scTitle = $("#scTitle");
  const scDesc = $("#scDesc");
  const scTags = $("#scTags");
  const scIndex = $("#scIndex");
  const scProgress = $("#scProgress");
  const scCopy = $("#showcaseCopy");
  const scPrev = $("#scPrev");
  const scNext = $("#scNext");
  const showcaseEl = $(".showcase");
  const scHint = $(".sc-hint");
  const deviceButtons = $$(".ds-btn");

  let currentScreen = "home";
  let currentDevice = "phone";
  let showcaseAutoStopped = false;
  let showcaseVisible = false;
  let showcaseTimer = 0;
  let progressStart = 0;
  let progressRaf = 0;
  const SHOWCASE_INTERVAL = 5200;

  const phoneImgs = {};
  const tabletImgs = {};

  function buildShowcaseImages() {
    if (showcasePhoneShots) {
      SCREEN_ORDER.forEach((key) => {
        const img = document.createElement("img");
        img.className = "shot" + (key === currentScreen ? " is-active" : "");
        img.src = `assets/img/screens/${SCREENS[key].phone}`;
        img.alt = `小宝影院${SCREENS[key].title.split(" · ")[0]}界面`;
        img.width = 760;
        img.height = 1351;
        img.loading = key === currentScreen ? "eager" : "lazy";
        img.decoding = "async";
        showcasePhoneShots.appendChild(img);
        phoneImgs[key] = img;
      });
    }
    if (showcaseTabletShots) {
      SCREEN_ORDER.forEach((key) => {
        const img = document.createElement("img");
        img.className = "shot" + (key === currentScreen ? " is-active" : "");
        img.src = `assets/img/screens/${SCREENS[key].tablet}`;
        img.alt = `小宝影院平板${SCREENS[key].title.split(" · ")[0]}界面`;
        img.width = 1440;
        img.height = 810;
        img.loading = "lazy";
        img.decoding = "async";
        showcaseTabletShots.appendChild(img);
        tabletImgs[key] = img;
      });
    }
  }

  function updateShowcaseCopy(key) {
    const data = SCREENS[key];
    if (!data) return;
    if (scCopy) scCopy.classList.add("is-switching");
    window.setTimeout(() => {
      if (scTitle) scTitle.textContent = data.title;
      if (scDesc) scDesc.textContent = data.desc;
      if (scTags) {
        scTags.innerHTML = "";
        data.tags.forEach((tag, i) => {
          const li = document.createElement("li");
          li.textContent = tag;
          li.style.animationDelay = `${i * 60}ms`;
          scTags.appendChild(li);
        });
      }
      if (scIndex) {
        scIndex.textContent = `${String(SCREEN_ORDER.indexOf(key) + 1).padStart(2, "0")} / ${String(
          SCREEN_ORDER.length
        ).padStart(2, "0")}`;
      }
      if (scCopy) scCopy.classList.remove("is-switching");
    }, 160);
  }

  function showScreen(key, userAction = false) {
    if (!SCREENS[key]) return;
    currentScreen = key;
    Object.entries(phoneImgs).forEach(([k, img]) => img.classList.toggle("is-active", k === key));
    Object.entries(tabletImgs).forEach(([k, img]) => img.classList.toggle("is-active", k === key));
    $$(".tab", screenTabs || document).forEach((tab) => {
      const active = tab.dataset.screen === key;
      tab.classList.toggle("is-active", active);
      tab.setAttribute("aria-selected", String(active));
      tab.tabIndex = active ? 0 : -1;
    });
    updateShowcaseCopy(key);
    if (userAction) stopShowcaseAuto();
    restartProgress();
  }

  function stopShowcaseAuto() {
    showcaseAutoStopped = true;
    clearInterval(showcaseTimer);
    cancelAnimationFrame(progressRaf);
    if (scHint) scHint.textContent = "手动浏览模式 · 使用箭头切换";
    if (scProgress) scProgress.style.width = "0%";
  }

  function tickProgress(now) {
    if (!showcaseVisible || showcaseAutoStopped) return;
    if (!progressStart) progressStart = now;
    const elapsed = now - progressStart;
    const pct = clamp((elapsed / SHOWCASE_INTERVAL) * 100, 0, 100);
    if (scProgress) scProgress.style.width = `${pct}%`;
    if (elapsed >= SHOWCASE_INTERVAL) {
      progressStart = now;
      const idx = SCREEN_ORDER.indexOf(currentScreen);
      showScreen(SCREEN_ORDER[(idx + 1) % SCREEN_ORDER.length]);
    }
    progressRaf = requestAnimationFrame(tickProgress);
  }

  function restartProgress() {
    cancelAnimationFrame(progressRaf);
    progressStart = 0;
    if (showcaseVisible && !showcaseAutoStopped && !reduceMotion) {
      progressRaf = requestAnimationFrame(tickProgress);
    }
  }

  function setDevice(device, userAction = false) {
    currentDevice = device;
    deviceButtons.forEach((btn) => btn.classList.toggle("is-active", btn.dataset.device === device));
    if (showcasePhone) showcasePhone.hidden = device !== "phone";
    if (showcaseTablet) showcaseTablet.hidden = device !== "tablet";
    if (showcaseEl) showcaseEl.dataset.device = device;
    if (userAction) stopShowcaseAuto();
  }

  if (screenTabs && showcaseEl) {
    buildShowcaseImages();
    showScreen("home");
    setDevice("phone");

    $$(".tab", screenTabs).forEach((tab, i, all) => {
      tab.tabIndex = i === 0 ? 0 : -1;
      tab.addEventListener("click", () => showScreen(tab.dataset.screen, true));
      tab.addEventListener("keydown", (e) => {
        if (e.key !== "ArrowRight" && e.key !== "ArrowLeft") return;
        e.preventDefault();
        const dir = e.key === "ArrowRight" ? 1 : -1;
        const next = all[(i + dir + all.length) % all.length];
        next.focus();
        showScreen(next.dataset.screen, true);
      });
    });

    deviceButtons.forEach((btn) =>
      btn.addEventListener("click", () => setDevice(btn.dataset.device, true))
    );

    scPrev && scPrev.addEventListener("click", () => {
      const idx = SCREEN_ORDER.indexOf(currentScreen);
      showScreen(SCREEN_ORDER[(idx - 1 + SCREEN_ORDER.length) % SCREEN_ORDER.length], true);
    });
    scNext && scNext.addEventListener("click", () => {
      const idx = SCREEN_ORDER.indexOf(currentScreen);
      showScreen(SCREEN_ORDER[(idx + 1) % SCREEN_ORDER.length], true);
    });

    if ("IntersectionObserver" in window) {
      const showcaseIO = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            showcaseVisible = entry.isIntersecting;
            if (showcaseVisible && !showcaseAutoStopped) {
              restartProgress();
            } else {
              cancelAnimationFrame(progressRaf);
            }
          });
        },
        { threshold: 0.25 }
      );
      showcaseIO.observe(showcaseEl);
    } else {
      showcaseVisible = true;
      restartProgress();
    }
  }

  /* ── Press demos ──────────────────────────────────────────────────────── */
  function bindPress(el, readout, format) {
    if (!el) return;
    const setPressed = (pressed) => {
      el.classList.toggle("is-pressed", pressed);
      if (readout) readout.textContent = format(pressed);
    };
    el.addEventListener("pointerdown", (e) => {
      try {
        el.setPointerCapture?.(e.pointerId);
      } catch (err) {
        /* synthetic pointer or unsupported */
      }
      setPressed(true);
    });
    el.addEventListener("pointerup", () => setPressed(false));
    el.addEventListener("pointercancel", () => setPressed(false));
    el.addEventListener("pointerleave", () => setPressed(false));
    el.addEventListener("keydown", (e) => {
      if (e.key === " " || e.key === "Enter") setPressed(true);
    });
    el.addEventListener("keyup", () => setPressed(false));
    el.addEventListener("blur", () => setPressed(false));
  }

  bindPress($("#pressDemo"), $("#pressValue"), (p) => (p ? "0.955" : "1.000"));
  bindPress($("#pressPill"), $("#pressReadout"), (p) =>
    p ? "scale · 0.955" : "scale · 1.000"
  );

  /* ── Seek demo ────────────────────────────────────────────────────────── */
  const seek = $("#seek");
  const seekFill = $("#seekFill");
  const seekThumb = $("#seekThumb");
  const seekCurrent = $("#seekCurrent");
  const SEEK_TOTAL = 1 * 3600 + 34 * 60 + 12; // 01:34:12

  function formatSeekTime(seconds) {
    const s = Math.max(0, Math.floor(seconds));
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${String(m).padStart(2, "0")}:${String(sec).padStart(2, "0")}`;
  }

  function setSeekPercent(pct, animateThumb = false) {
    const p = clamp(pct, 0, 100);
    if (seekFill) seekFill.style.width = `${p}%`;
    if (seekThumb) seekThumb.style.left = `${p}%`;
    if (seekCurrent) seekCurrent.textContent = formatSeekTime((p / 100) * SEEK_TOTAL);
    if (seek) seek.setAttribute("aria-valuenow", String(Math.round(p)));
  }

  if (seek) {
    let dragging = false;
    setSeekPercent(36);

    const updateFromEvent = (e) => {
      const r = seek.getBoundingClientRect();
      const pct = ((e.clientX - r.left) / r.width) * 100;
      setSeekPercent(pct);
    };

    seek.addEventListener("pointerdown", (e) => {
      dragging = true;
      seek.classList.add("is-dragging");
      try {
        seek.setPointerCapture?.(e.pointerId);
      } catch (err) {
        /* synthetic pointer or unsupported */
      }
      updateFromEvent(e);
    });
    seek.addEventListener("pointermove", (e) => {
      if (dragging) updateFromEvent(e);
    });
    const endDrag = () => {
      dragging = false;
      seek.classList.remove("is-dragging");
    };
    seek.addEventListener("pointerup", endDrag);
    seek.addEventListener("pointercancel", endDrag);
    seek.addEventListener("keydown", (e) => {
      const now = Number(seek.getAttribute("aria-valuenow") || 36);
      if (e.key === "ArrowRight" || e.key === "ArrowUp") {
        e.preventDefault();
        setSeekPercent(now + 3);
      } else if (e.key === "ArrowLeft" || e.key === "ArrowDown") {
        e.preventDefault();
        setSeekPercent(now - 3);
      }
    });
  }

  /* ── Tablet tilt + theme preview ──────────────────────────────────────── */
  const adaptiveVisual = $(".adaptive-visual");
  const tiltTablet = $("#tiltTablet");
  if (adaptiveVisual && tiltTablet && finePointer && !reduceMotion) {
    let targetRX = 2;
    let targetRY = -7;
    let curRX = 2;
    let curRY = -7;
    let raf = 0;
    tiltTablet.style.transform = "rotateX(2deg) rotateY(-7deg)";
    const loopTilt = () => {
      curRX = lerp(curRX, targetRX, 0.1);
      curRY = lerp(curRY, targetRY, 0.1);
      tiltTablet.style.transform = `rotateX(${curRX.toFixed(3)}deg) rotateY(${curRY.toFixed(3)}deg)`;
      if (Math.abs(curRX - targetRX) > 0.01 || Math.abs(curRY - targetRY) > 0.01) {
        raf = requestAnimationFrame(loopTilt);
      } else {
        raf = 0;
      }
    };
    const kickTilt = () => {
      if (!raf) raf = requestAnimationFrame(loopTilt);
    };
    adaptiveVisual.addEventListener("pointermove", (e) => {
      const r = adaptiveVisual.getBoundingClientRect();
      const nx = (e.clientX - r.left) / r.width - 0.5;
      const ny = (e.clientY - r.top) / r.height - 0.5;
      targetRY = nx * 11;
      targetRX = -ny * 8;
      kickTilt();
    });
    adaptiveVisual.addEventListener("pointerleave", () => {
      targetRX = 2;
      targetRY = -7;
      kickTilt();
    });
  }

  const tabletShots = $$(".tablet-shot");
  $$("[data-tablet-theme]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const theme = btn.dataset.tabletTheme;
      $$("[data-tablet-theme]").forEach((b) =>
        b.classList.toggle("is-active", b === btn)
      );
      tabletShots.forEach((img) => {
        const isDark = img.src.includes("dark");
        img.classList.toggle("is-active", theme === "dark" ? isDark : !isDark);
      });
    });
  });

  /* ── Counters ─────────────────────────────────────────────────────────── */
  const counters = $$("[data-count]");
  if (counters.length) {
    const animateCounter = (el) => {
      const target = Number(el.dataset.count || 0);
      const suffix = el.dataset.suffix || "";
      const duration = 1400;
      const start = performance.now();
      const easeOut = (t) => 1 - Math.pow(1 - t, 4);
      const step = (now) => {
        const p = clamp((now - start) / duration, 0, 1);
        el.textContent = `${Math.round(easeOut(p) * target)}${suffix}`;
        if (p < 1) requestAnimationFrame(step);
      };
      requestAnimationFrame(step);
    };

    if ("IntersectionObserver" in window && !reduceMotion) {
      const counterIO = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              animateCounter(entry.target);
              counterIO.unobserve(entry.target);
            }
          });
        },
        { threshold: 0.5 }
      );
      counters.forEach((c) => counterIO.observe(c));
    } else {
      counters.forEach((c) => {
        c.textContent = `${c.dataset.count}${c.dataset.suffix || ""}`;
      });
    }
  }

  /* ── FAQ accordion ────────────────────────────────────────────────────── */
  $$(".faq-item").forEach((item) => {
    const btn = $(".faq-q", item);
    const panel = $(".faq-a", item);
    btn &&
      btn.addEventListener("click", () => {
        const isOpen = item.classList.contains("open");
        $$(".faq-item").forEach((other) => {
          other.classList.remove("open");
          const otherBtn = $(".faq-q", other);
          otherBtn && otherBtn.setAttribute("aria-expanded", "false");
        });
        if (!isOpen) {
          item.classList.add("open");
          btn.setAttribute("aria-expanded", "true");
        }
      });
    // Set initial max-height after fonts/layout settle for smooth opening.
    panel && panel.setAttribute("aria-hidden", "true");
  });

  /* ── Toast + copy hash ────────────────────────────────────────────────── */
  const toast = $("#toast");
  let toastTimer = 0;
  function showToast(message) {
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove("show"), 2200);
  }

  const copyHash = $("#copyHash");
  const shaText = $("#shaText");
  copyHash &&
    copyHash.addEventListener("click", async () => {
      const value = shaText ? shaText.textContent.trim() : "";
      if (!value) return;
      try {
        await navigator.clipboard.writeText(value);
        showToast("SHA-256 已复制到剪贴板");
      } catch (e) {
        const ta = document.createElement("textarea");
        ta.value = value;
        ta.style.position = "fixed";
        ta.style.opacity = "0";
        document.body.appendChild(ta);
        ta.select();
        try {
          document.execCommand("copy");
          showToast("SHA-256 已复制到剪贴板");
        } catch (err) {
          showToast("复制失败，请手动选择");
        }
        ta.remove();
      }
    });

  /* ── Small niceties ───────────────────────────────────────────────────── */
  const yearEl = $("#year");
  if (yearEl) yearEl.textContent = String(new Date().getFullYear());

  // Prevent download links from being hijacked by smooth-scroll anchors.
  $$('a[href$=".apk"]').forEach((link) => {
    link.addEventListener("click", () => {
      showToast("开始下载 APK · 3.2 MB");
    });
  });

  // Pause decorative loops when the tab is hidden.
  document.addEventListener("visibilitychange", () => {
    if (document.hidden) cancelAnimationFrame(rafId);
  });
})();
