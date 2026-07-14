package io.argus.server;

/** The single-page browser UI served at {@code /}. Plain HTML/CSS/JS, no build step, no framework. */
final class WebUi {

    private WebUi() {
    }

    static final String HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Argus Search</title>
              <style nonce="{{NONCE}}">
                :root { --bg:#0e0f12; --panel:#16181d; --line:#262a31; --bone:#e8e6e1;
                        --dim:#9aa0aa; --ember:#ff6a2b; }
                * { box-sizing:border-box; }
                body { margin:0; background:var(--bg); color:var(--bone);
                       font:15px/1.5 ui-monospace,SFMono-Regular,Menlo,Consolas,monospace; }
                header { padding:22px 20px; border-bottom:1px solid var(--line); font-size:20px;
                         letter-spacing:.02em; }
                header span { color:var(--ember); }
                main { max-width:900px; margin:0 auto; padding:24px 20px; }
                input, button { font:inherit; }
                #q { width:100%; padding:14px 16px; background:var(--panel); color:var(--bone);
                     border:1px solid var(--line); border-radius:10px; outline:none; }
                #q:focus { border-color:var(--ember); }
                .row { display:flex; gap:10px; margin-top:10px; }
                .row input { flex:1; padding:10px 12px; background:var(--panel); color:var(--bone);
                             border:1px solid var(--line); border-radius:8px; }
                .row input#k { flex:0 0 90px; }
                button { padding:10px 18px; background:var(--ember); color:#111; border:none;
                         border-radius:8px; cursor:pointer; font-weight:600; }
                #meta { color:var(--dim); margin:18px 2px 8px; font-size:13px; }
                .hit { background:var(--panel); border:1px solid var(--line); border-radius:10px;
                       padding:14px 16px; margin-bottom:10px; }
                .hit .top { display:flex; justify-content:space-between; margin-bottom:8px; }
                .hit .id { color:var(--dim); }
                .hit .score { color:var(--ember); }
                .f { margin:3px 0; }
                .f span { display:inline-block; min-width:64px; color:var(--dim); }
              </style>
            </head>
            <body>
              <header>Argus <span>search</span></header>
              <main>
                <input id="q" placeholder='Try:  title:distributed AND body:"fault tolerant"' autofocus>
                <div class="row">
                  <input id="field" placeholder="default field (body)">
                  <input id="k" type="number" value="10" min="1">
                  <button onclick="run()">Search</button>
                </div>
                <div id="meta"></div>
                <div id="results"></div>
              </main>
              <script nonce="{{NONCE}}">
                async function run() {
                  const q = document.getElementById('q').value;
                  const field = document.getElementById('field').value;
                  const k = document.getElementById('k').value || 10;
                  const url = `/search?q=${encodeURIComponent(q)}&field=${encodeURIComponent(field)}&k=${encodeURIComponent(k)}`;
                  const t0 = performance.now();
                  let data;
                  try { data = await (await fetch(url)).json(); }
                  catch (err) { document.getElementById('meta').textContent = 'request failed'; return; }
                  const ms = (performance.now() - t0).toFixed(1);
                  document.getElementById('meta').textContent =
                    `${data.total ?? 0} hit(s) - showing ${data.returned ?? 0} - ${ms} ms`;
                  const box = document.getElementById('results');
                  box.innerHTML = '';
                  for (const h of (data.hits || [])) {
                    const div = document.createElement('div');
                    div.className = 'hit';
                    let fields = '';
                    for (const [key, val] of Object.entries(h.fields || {})) {
                      fields += `<div class="f"><span>${esc(key)}</span>${esc(String(val))}</div>`;
                    }
                    div.innerHTML =
                      `<div class="top"><span class="id">#${h.docId}</span>` +
                      `<span class="score">${Number(h.score).toFixed(4)}</span></div>${fields}`;
                    box.appendChild(div);
                  }
                }
                function esc(s) {
                  return s.replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
                }
                document.getElementById('q').addEventListener('keydown', e => { if (e.key === 'Enter') run(); });
              </script>
            </body>
            </html>
            """;

    static String render(String nonce) {
        return HTML.replace("{{NONCE}}", nonce);
    }
}
