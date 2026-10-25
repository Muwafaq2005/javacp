# Voice Browser — Command Reference

Everything you can say (or type into the control page's text box) to drive the controlled browser.

## Which mode are you in?

The server picks the first decision engine that answers. The log on the control page shows which one was used.

| Mode | When | How you can talk |
| --- | --- | --- |
| **TypeSafe Jev (cloud)** | `TYPESAFE_API_KEY` set in `.env` | Natural language: rephrasings, corrections ("no, not that one"), and numbered picks all work |
| **Laya (local)** | `laya_server.py` running on `127.0.0.1:8000` | Natural language (accuracy depends on the local model) |
| **Offline heuristic** | Neither of the above (the default) | **Exact phrasings only.** Use the wording in this file |

The phrasings below are written for the **offline heuristic**, so they also work with a model. Commands marked
🧠 need a model (Jev or Laya).

Tips:
- Pause briefly after each command. Commands with free text (searches, typing) wait for ~0.6 s of silence before running.
- Filler like "please", "okay", "um" is fine.

---

## Navigation

| Say | What happens |
| --- | --- |
| `go to wikipedia` · `open youtube` · `visit github` | Opens a known site's home page |
| `go to example dot com` · `open news dot ycombinator dot com` · `visit github.com/anthropics` | Opens a spoken address ("dot" and "slash" are converted) |
| `go back` · `back` · `undo` | Back in history |
| `go forward` · `forward` | Forward in history |
| `reload` · `refresh` | Reloads the page |

Known sites: google, duckduckgo, youtube, wikipedia, github, amazon, reddit. For anything else, say the domain.

> ⚠ Offline, saying a site name anywhere counts as "go to that site": `google cats` opens the Google home page.
> Use `search google for cats` instead. `go to twitter` doesn't work offline; say `go to x dot com`.

## Search

| Say | What happens |
| --- | --- |
| `search for alan turing` · `look up typesafe jev` · `find cheap flights` | Uses the current page's search box if it has one, otherwise DuckDuckGo |
| `search youtube for lofi beats` · `search wikipedia for cats` · `search github for playwright` | That site's own search |

Sites with search: google, duckduckgo, youtube, wikipedia, github, amazon, reddit, x/twitter (🧠), hacker news (🧠).

## Clicking

| Say | What happens |
| --- | --- |
| `click documentation` · `click sign in` · `pick more information` | Clicks the element whose visible text appears in what you said |
| `click the first result` · `open the second link` · `click the 3rd video` · `select the fourth item` | Clicks the Nth item of that kind |
| `one` · `two` · `three` (after numbered badges appear) 🧠 | Picks a numbered candidate when the target was ambiguous |
| `click the other one` · `no, not that one` 🧠 | Corrects the last click |

- Offline, the words must contain the element's **full visible text** (case doesn't matter). If nothing matches, nothing is clicked.
- Ordinals: categories are `video` (the default), `link`, `result`, `button`, `item` / `option`. Numbers 1–10 work as words or digits.
  *Video* ordinals target YouTube results/recommendations.

### Risky clicks need confirmation
Clicks containing words like **buy, purchase, checkout, place order, pay, delete, remove, send, post, publish,
submit, log out, sign out, unsubscribe** pause and show a toast:

| Say | What happens |
| --- | --- |
| `confirm` · `yes` · `yes do it` · `do it` · `go ahead` | Runs the pending action |
| `cancel` · `never mind` · `no` · `don't` | Drops it |

## Typing and forms

| Say | What happens |
| --- | --- |
| `type hello world into the search box` 🧠 | Types the text verbatim into the field |
| `select english from the language dropdown` 🧠 | Picks a dropdown option |
| `clear` · `clear the search box` | Empties the focused field (with a model, the field you name) |
| `clear form` · `reset form` · `empty form` | Empties every input on the page |
| `press enter` · `press escape` · `press space` · `press tab` | Presses that key |

## Scrolling

| Say | What happens |
| --- | --- |
| `scroll down` | Down one screen |
| `scroll up` | Up one screen |
| `scroll to top` · `scroll top` | Jumps to the top |
| `scroll to bottom` · `scroll bottom` | Jumps to the bottom |
| `scroll down a bit` · `scroll to the end` 🧠 | Small / full scroll (offline, every scroll is one screen) |

## Tabs

| Say | What happens |
| --- | --- |
| `new tab` · `open new tab` | Opens an empty tab |
| `close tab` · `close current tab` | Closes the current tab |
| `next tab` · `switch tab` | Next tab |
| `switch to previous tab` | Previous tab |
| `switch to first tab` | First tab |

> ⚠ Offline, a bare `previous tab` goes to the *next* tab; say `switch to previous tab`.
> `open X in new tab` only opens an empty tab.

## Page tools

| Say | What happens |
| --- | --- |
| `zoom in` · `zoom out` | 125% / 85% |
| `zoom to 150%` · `zoom 90%` | That exact zoom level |
| `zoom reset` · `zoom normal` | Back to 100% |
| `read page` · `read article` · `read content` | Puts the first ~300 characters of the main text in the log (it is not spoken aloud) |

> ⚠ `reset zoom` (that word order) zooms *in* offline; say `zoom reset`.

## Video and audio (any page with a `<video>`/`<audio>`)

| Say | What happens |
| --- | --- |
| `play` · `pause` · `resume` · `stop` | Toggles play/pause |
| `volume up` · `volume down` · `turn volume down` | ±15% |
| `skip 10 seconds` · `skip forward 30 seconds` · `fast forward` | Jumps ahead (default 10 s) |
| `skip back 10 seconds` · `seek backward 30` · `rewind` | Jumps back (default 10 s) |
| `speed 1.5x` · `speed 2x` · `speed 0.5` | Sets the playback rate |
| `play faster` · `play slower` · `normal speed` | 1.5× / 0.75× / 1× |

> ⚠ Known issues offline:
> - `mute`, `unmute` and `fullscreen` also just toggle play/pause.
> - `rewind 30 seconds` jumps **forward**; say `skip back 30 seconds`.
> - `double speed` and `speed up` set 1×; say `speed 2x` or `play faster`.
> - Any sentence with "speed", "rate", "faster" or "slower" counts as a speed command, e.g. `search for speed test`.

## YouTube

| Say | What happens |
| --- | --- |
| `turn on captions` · `toggle subtitles` | Toggles CC |
| `toggle theater mode` | Theater mode |
| `open miniplayer` | Miniplayer |
| `go to next chapter` · `go to previous chapter` | Chapter navigation |
| `show comments` | Scrolls to the comments |
| `like` | Likes the video |
| `subscribe` | Subscribes to the channel |

> ⚠ Offline, start these with a leading word as shown. A bare `theater mode`, `next chapter` or `like video` toggles **captions** instead.
> Any sentence containing "comments", "captions", "subtitles" or "subscribe" counts as a YouTube command, even `search for subtitles`.

---

## Control page (http://localhost:8787)

| Control | What it does |
| --- | --- |
| **Start mic** | Starts speech recognition (Chrome/Edge only) |
| **Text box + Enter** | Same as speaking the command |
| **Undo** | Goes back one page |
| **Re-snapshot** | Re-reads the page's elements (use after the page changes by itself) |

## Launch options

```bash
./run.sh                                                    # headed Chromium, starts on example.com
./run.sh -Dspring-boot.run.jvmArguments="-Dheadless=true"   # no browser window
./run.sh -Dspring-boot.run.jvmArguments="-DstartUrl=https://www.youtube.com/"
./run.sh -Dspring-boot.run.jvmArguments="-Dcdp=http://127.0.0.1:9222"   # drive your own Chrome (started with --remote-debugging-port=9222)
./run.sh -Dspring-boot.run.jvmArguments="-Dserver.port=9000"
```

The server only listens on `127.0.0.1`. To use it from another device, add
`-Dserver.address=0.0.0.0 -Dvoicebrowser.allowed-origins=http://<your-ip>:8787`, and only on a trusted network.
