# 小宝影院 · Aurora — Landing Page

Static, dependency-free landing page for the XiaobaoTV APK. It is published to
GitHub Pages from the `gh-pages` branch of the main repository:

**https://nestchao.github.io/XiaobaoYingYuan_APK/**

## Structure

```
website/
├── index.html            # one-page site
├── 404.html              # themed not-found page
├── assets/
│   ├── css/style.css     # design system + all animations
│   ├── js/main.js        # theme, aurora canvas, carousels, demos
│   ├── fonts/            # self-hosted Space Grotesk (woff2)
│   └── img/
│       ├── brand/        # icon, favicon, OG image
│       └── screens/      # WebP screenshots (phone + tablet)
├── site.webmanifest
├── robots.txt
├── sitemap.xml
└── .nojekyll
```

## Local preview

```bash
# from the repository root
python -m http.server 8123 --directory website
# open http://127.0.0.1:8123
```

## Deployment

The site is served from an orphan `gh-pages` branch containing only the
contents of this folder. Update it with:

```bash
git subtree push --prefix=website origin gh-pages
```

(or, from a clean checkout: `git push origin $(git subtree split --prefix=website HEAD):gh-pages --force`)

GitHub Pages is configured with source branch `gh-pages`, path `/`.

## Editing notes

- Design tokens live at the top of `assets/css/style.css` (`:root` and
  `[data-theme="light"]`). The aurora canvas reads the `--glow-*` tokens at
  runtime, so theme changes are picked up automatically.
- Screenshots are generated from the emulator QA captures and converted to
  WebP. Keep the 760×1351 (phone) and 1440×810 (tablet) aspect ratios.
- The download button points at the `v1.3.0` GitHub Release asset. Update the
  URL (hero, download section, JSON-LD) when a newer version ships.
- Everything respects `prefers-reduced-motion`.
