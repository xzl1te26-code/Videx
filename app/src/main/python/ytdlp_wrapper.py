import sys
import os
import traceback
import re
import requests
import json
import time
import gc
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

# 🧠 Глобальные переменные для "Тёплого запуска"
YT_DLP_CORE = None
UNSUPPORTED_ERROR = None

def set_custom_core_path(zip_path):
    """Динамически подключает скачанный ZIP-архив yt-dlp в sys.path"""
    try:
        if os.path.exists(zip_path) and zip_path not in sys.path:
            sys.path.insert(0, zip_path)
            return {"status": "success"}
        return {"status": "error", "error_msg": "File not found"}
    except Exception as e:
        return {"status": "error", "error_msg": str(e)}

def pre_load():
    """
    ⚡ Умный предварительный импорт yt-dlp.
    Вызывается один раз при старте приложения для 'прогрева' движка.
    """
    global YT_DLP_CORE, UNSUPPORTED_ERROR
    try:
        if YT_DLP_CORE is None:
            import yt_dlp
            from yt_dlp.utils import UnsupportedError
            YT_DLP_CORE = yt_dlp
            UNSUPPORTED_ERROR = UnsupportedError
            # Делаем фиктивный вызов для инициализации внутренних кэшей yt-dlp
            # (необязательно, но ускоряет первый реальный запрос)
            return {"status": "success", "msg": "Engine warmed up"}
    except Exception as e:
        return {"status": "error", "error_msg": str(e)}
    return {"status": "already_warm"}

def create_optimized_session():
    session = requests.Session()
    retry_strategy = Retry(
        total=2,
        backoff_factor=0.3,
        status_forcelist=[429, 500, 502, 503, 504],
        allowed_methods=["HEAD", "GET", "POST"]
    )
    adapter = HTTPAdapter(
        pool_connections=15,
        pool_maxsize=30,
        max_retries=retry_strategy
    )
    session.mount("https://", adapter)
    session.mount("http://", adapter)
    return session

HTTP_SESSION = create_optimized_session()

def resolve_tiktok_url(url):
    clean_url = url.strip()
    try:
        if any(domain in clean_url for domain in ["vm.tiktok.com", "vt.tiktok.com", "/t/", "/link/"]):
            headers = {
                'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15',
                'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
            }
            r = HTTP_SESSION.get(clean_url, headers=headers, allow_redirects=True, timeout=6)
            clean_url = r.url
    except Exception:
        pass
    return clean_url

def extract_tiktok_id(url):
    match = re.search(r'/(?:video|photo|v)/(\d+)', url)
    if match: return match.group(1)
    match = re.search(r'(\d{15,22})', url)
    if match: return match.group(1)
    return None

def parse_via_tiktok_embed(video_id):
    try:
        embed_url = f"https://www.tiktok.com/embed/v2/{video_id}"
        headers = {
            'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Referer': 'https://www.tiktok.com/'
        }
        res = HTTP_SESSION.get(embed_url, headers=headers, timeout=6)
        if res.status_code == 200:
            html = res.text
            match = re.search(r'<script id="__FRONTITY_CONNECT_STATE__" type="application/json">({.*?})</script>', html)
            if not match:
                match = re.search(r'window\[\'__DEFAULT_DATA__\'\]\s*=\s*({.*?});', html)

            if match:
                data = json.loads(match.group(1))
                root = data.get("source", {}) if "source" in data else data
                for key, val in root.items():
                    if isinstance(val, dict) and "videoData" in val:
                        vd = val["videoData"]
                        item = vd.get("itemInfos", {})
                        author = vd.get("authorInfos", {}).get("nickName") or "TikTok"
                        title = item.get("text") or "TikTok видео"
                        cover = item.get("covers", [None])[0]
                        video_url = item.get("video", {}).get("urls", [None])[0]
                        music = item.get("music", {}).get("playUrl", [None])[0]

                        if video_url:
                            return {
                                "is_photo": False,
                                "title": title,
                                "author": author,
                                "video_url": video_url,
                                "cover": cover or "",
                                "music": music
                            }
    except Exception:
        pass
    return None

def parse_via_cobalt(url):
    try:
        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
        payload = {
            'url': url,
            'vQuality': 'max',
            'filenamePattern': 'classic'
        }
        res = HTTP_SESSION.post('https://api.cobalt.tools/api/json', json=payload, headers=headers, timeout=6)
        if res.status_code == 200:
            data = res.json()
            status = data.get('status')

            if status == 'picker':
                picker = data.get('picker', [])
                images = [item.get('url') for item in picker if item.get('type') == 'photo']
                audio = data.get('audio')
                if images:
                    return {
                        "is_photo": True,
                        "title": "TikTok Фото-карусель",
                        "author": "TikTok",
                        "images": images,
                        "cover": images[0],
                        "music": audio
                    }

            video_url = data.get('url')
            if video_url:
                return {
                    "is_photo": False,
                    "title": "TikTok видео",
                    "author": "TikTok",
                    "video_url": video_url,
                    "cover": "",
                    "music": data.get('audio')
                }
    except Exception:
        pass
    return None

def parse_via_tikwm(url):
    try:
        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}
        res = HTTP_SESSION.post("https://www.tikwm.com/api/", data={'url': url, 'hd': 1}, headers=headers, timeout=6)
        if res.status_code == 200:
            data = res.json()
            if data.get("code") == 0 and "data" in data:
                d = data["data"]
                images = d.get("images", [])
                title = d.get("title") or "TikTok видео"
                author = d.get("author", {}).get("nickname") or "TikTok"
                cover = d.get("cover") or (images[0] if images else "")

                if images:
                    return {
                        "is_photo": True,
                        "title": title,
                        "author": author,
                        "images": images,
                        "cover": cover,
                        "music": d.get("music")
                    }
                else:
                    return {
                        "is_photo": False,
                        "title": title,
                        "author": author,
                        "video_url": d.get("hdplay") or d.get("play") or d.get("wmplay"),
                        "cover": cover,
                        "music": d.get("music")
                    }
    except Exception:
        pass
    return None

def get_tiktok_meta(url):
    resolved = resolve_tiktok_url(url)
    video_id = extract_tiktok_id(resolved)

    if video_id:
        data = parse_via_tiktok_embed(video_id)
        if data and (data.get("video_url") or data.get("images")):
            return data

    data = parse_via_cobalt(resolved)
    if data and (data.get("video_url") or data.get("images")):
        return data

    data = parse_via_tikwm(resolved)
    if data and (data.get("video_url") or data.get("images")):
        return data

    return None

def apply_impersonate_or_headers(ydl_opts, use_impersonate=False):
    """
    🛡️ БЕЗОПАСНАЯ ПОДМЕНА BROWSER TARGET & HEADERS
    Предотвращает краш 'Impersonate target chrome is not available' на Android NDK / Chaquopy,
    где библиотека curl_cffi не установлена.
    """
    headers = ydl_opts.get('http_headers', {})
    headers.update({
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9,ru;q=0.8',
        'Sec-Ch-Ua': '"Google Chrome";v="131", "Chromium";v="131", "Not_A Brand";v="24"',
        'Sec-Ch-Ua-Mobile': '?0',
        'Sec-Ch-Ua-Platform': '"Windows"',
        'Sec-Fetch-Dest': 'document',
        'Sec-Fetch-Mode': 'navigate',
        'Sec-Fetch-Site': 'none',
        'Sec-Fetch-User': '?1',
        'Upgrade-Insecure-Requests': '1'
    })
    if use_impersonate:
        headers['Referer'] = 'https://www.google.com/'
    ydl_opts['http_headers'] = headers

    if use_impersonate:
        try:
            import yt_dlp.networking.impersonate as imp_mod
            targets = getattr(imp_mod, 'IMPERSONATE_TARGETS', {})
            if 'chrome' in targets or hasattr(imp_mod, 'ImpersonateTarget'):
                ydl_opts['impersonate'] = 'chrome'
        except Exception:
            ydl_opts.pop('impersonate', None)

# =========================================================
# 🚀 ПАРСИНГ ОДИНОЧНЫХ ВИДЕО И ПЛЕЙЛИСТОВ
# =========================================================
def get_video_info(url, cookie_path="", use_impersonate=False, metadata_cache_path=""):
    url = url.strip()

    # 1. TIKTOK
    if "tiktok.com" in url or "douyin.com" in url:
        data = get_tiktok_meta(url)
        if data:
            title = data.get("title")
            author = data.get("author")
            cover = data.get("cover")

            if data.get("is_photo"):
                images = data.get("images", [])

                # 🧠 Сохраняем паспорт для мгновенного скачивания
                if metadata_cache_path:
                    try:
                        with open(metadata_cache_path, 'w', encoding='utf-8') as f:
                            json.dump(data, f)
                    except: pass

                gc.collect()
                return {
                    "status": "success",
                    "title": f"{title} (Автор: {author}) [{len(images)} фото]",
                    "thumbnail": cover,
                    "is_photo": True,
                    "is_playlist": False,
                    "playlist_entries": "[]",
                    "available_qualities": json.dumps(["original"]),
                    "version": "TikTok Fast Core"
                }
            else:
                # 🧠 Сохраняем паспорт для мгновенного скачивания
                if metadata_cache_path:
                    try:
                        with open(metadata_cache_path, 'w', encoding='utf-8') as f:
                            json.dump(data, f)
                    except: pass

                gc.collect()
                return {
                    "status": "success",
                    "title": f"{title} (Автор: {author})",
                    "thumbnail": cover,
                    "is_photo": False,
                    "is_playlist": False,
                    "playlist_entries": "[]",
                    "available_qualities": json.dumps(["1080", "720"]),
                    "version": "TikTok Fast Core"
                }
        else:
            return {
                "status": "error",
                "error_msg": "TikTok временно ограничил доступ к этому ролику."
            }

    # 2. YT-DLP: ПЛЕЙЛИСТЫ И ОДИНОЧНЫЕ ВИДЕО
    try:
        global YT_DLP_CORE, UNSUPPORTED_ERROR
        if YT_DLP_CORE is None:
            pre_load()

        yt_dlp = YT_DLP_CORE
        UnsupportedError = UNSUPPORTED_ERROR

        ydl_opts = {
            'quiet': True,
            'no_warnings': True,
            'skip_download': True,
            'extract_flat': 'in_playlist',
            'cachedir': True,
            'check_formats': None,
            'no_color': True,
            'socket_timeout': 10,
            'retries': 2,
            'extractor_args': {
                'youtube': {
                    'player_client': ['android', 'ios', 'mweb', 'tv'],
                    'player_skip': ['web'] if use_impersonate else []
                }
            }
        }

        apply_impersonate_or_headers(ydl_opts, use_impersonate)

        if cookie_path and os.path.exists(cookie_path):
            ydl_opts['cookiefile'] = cookie_path

        info = None
        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=False)
        except Exception as imp_err:
            if "Impersonate target" in str(imp_err) or "is not available" in str(imp_err):
                ydl_opts.pop('impersonate', None)
                with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                    info = ydl.extract_info(url, download=False)
            elif isinstance(imp_err, UnsupportedError):
                return {
                    "status": "error",
                    "error_msg": "Этот тип ссылок пока не поддерживается. Попробуйте видео из YouTube, TikTok или Instagram."
                }
            elif "Unsupported URL" in str(imp_err):
                return {
                    "status": "error",
                    "error_msg": "Ссылка не распознана или сайт не поддерживается."
                }
            else:
                raise imp_err

        # 🧠 МГНОВЕННЫЙ СТАРТ: Сохраняем полные метаданные в JSON кэш
        if metadata_cache_path and info:
            try:
                with open(metadata_cache_path, 'w', encoding='utf-8') as f:
                    json.dump(yt_dlp.utils.sanitize_info(info), f)
            except Exception as ce:
                print(f"Metadata cache failed: {ce}")

            # ⭐️ ПРОВЕРКА: ЭТО ПЛЕЙЛИСТ / АЛЬБОМ?
            if info.get('_type') == 'playlist' or 'entries' in info:
                raw_entries = info.get('entries', [])
                entries_list = []

                for entry in raw_entries:
                    if not entry: continue
                    dur = entry.get('duration')
                    dur_str = ""
                    if dur and isinstance(dur, (int, float)):
                        mins = int(dur // 60)
                        secs = int(dur % 60)
                        dur_str = f"{mins:02d}:{secs:02d}"

                    v_url = entry.get('url') or entry.get('webpage_url')
                    if not v_url and entry.get('id'):
                        v_url = f"https://www.youtube.com/watch?v={entry.get('id')}"

                    entries_list.append({
                        "id": str(entry.get('id') or len(entries_list)),
                        "title": entry.get('title') or "Без названия",
                        "url": v_url or url,
                        "thumbnail": entry.get('thumbnail') or (entry.get('thumbnails', [{}])[0].get('url', "")),
                        "duration": dur_str,
                        "author": entry.get('uploader') or entry.get('channel') or info.get('uploader') or ""
                    })

                first_thumb = entries_list[0]["thumbnail"] if entries_list else ""
                playlist_title = info.get('title') or f"Плейлист ({len(entries_list)} видео)"

                return {
                    "status": "success",
                    "title": playlist_title,
                    "thumbnail": first_thumb,
                    "is_photo": False,
                    "is_playlist": True,
                    "playlist_entries": json.dumps(entries_list),
                    "available_qualities": json.dumps(["1080", "720", "480", "360"]),
                    "version": getattr(yt_dlp, '__version__', 'unknown')
                }

            # ОДИНОЧНОЕ ВИДЕО
            title = info.get('title', 'Без названия')
            uploader = info.get('uploader', 'Неизвестный автор')
            thumbnail = info.get('thumbnail', '')
            version = getattr(yt_dlp, '__version__', 'unknown')

            formats = info.get('formats', [])
            heights = set()
            for f in formats:
                h = f.get('height')
                if h and isinstance(h, int) and h >= 144:
                    heights.add(h)

            standard_tiers = [2160, 1440, 1080, 720, 480, 360, 240, 144]
            available_qualities = []
            for tier in standard_tiers:
                if any(h >= tier for h in heights):
                    available_qualities.append(str(tier))

            if not available_qualities:
                available_qualities = ["1080", "720", "480", "360"]

            return {
                "status": "success",
                "title": f"{title} (Автор: {uploader})",
                "thumbnail": thumbnail,
                "is_photo": False,
                "is_playlist": False,
                "playlist_entries": "[]",
                "available_qualities": json.dumps(available_qualities),
                "version": version
            }
    except Exception as e:
        return {
            "status": "error",
            "error_msg": str(e),
            "trace": traceback.format_exc()
        }
    finally:
        gc.collect()

def download_video(url, output_dir, callback=None, is_audio_only=False, quality="best", threads=4, rate_limit=0, throttled_rate=102400, cookie_path="", ffmpeg_path="", custom_filename=None, use_sponsorblock=False, use_impersonate=False, passport_path=""):
    url = url.strip()
    os.makedirs(output_dir, exist_ok=True)
    CHUNK_SIZE = 262144

    def check_cancellation():
        if callback is not None:
            try:
                fn = getattr(callback, 'isCancelled', None)
                if fn is not None and callable(fn) and fn():
                    raise Exception("DOWNLOAD_CANCELLED_BY_USER")
            except Exception as ex:
                if "DOWNLOAD_CANCELLED_BY_USER" in str(ex):
                    raise ex

    try:
        check_cancellation()

        # 🧠 МГНОВЕННЫЙ СТАРТ: Пытаемся взять данные из паспорта
        passport_data = None
        if passport_path and os.path.exists(passport_path):
            try:
                with open(passport_path, 'r', encoding='utf-8') as f:
                    passport_data = json.load(f)
            except: pass

        # 1. TIKTOK
        if "tiktok.com" in url or "douyin.com" in url:
            data = passport_data if (passport_data and (passport_data.get("video_url") or passport_data.get("images"))) else get_tiktok_meta(url)
            if data:
                raw_title = custom_filename or data.get("title") or "tiktok_media"
                # More robust cleaning: remove emojis and non-filesystem chars
                title = re.sub(r'[^\w\s\-\(\)\[\]]', "", raw_title).strip()[:50]
                if not title: title = "tiktok_media"

                # Add unique suffix to prevent overwriting
                unique_suffix = str(int(time.time()))[-4:]
                title = f"{title}_{unique_suffix}"

                if is_audio_only:
                    music_url = data.get("music") or data.get("video_url")
                    if not music_url:
                        return {"status": "error", "error_msg": "Аудиодорожка недоступна"}

                    file_path = os.path.join(output_dir, f"{title}.mp3")
                    r = HTTP_SESSION.get(music_url, timeout=20, stream=True)
                    with open(file_path, 'wb') as f:
                        for chunk in r.iter_content(chunk_size=CHUNK_SIZE):
                            check_cancellation()
                            if chunk: f.write(chunk)

                    if callback:
                        try: callback.onProgressDetailed(100, 0, 0)
                        except Exception: pass

                    return {
                        "status": "success",
                        "file_path": file_path,
                        "all_files": json.dumps([file_path]),
                        "is_photo": False
                    }

                if data.get("is_photo"):
                    images = data.get("images", [])
                    total = len(images)
                    saved_files = []

                    for i, img_url in enumerate(images):
                        check_cancellation()
                        file_path = os.path.join(output_dir, f"{title}_{i+1}.jpg")
                        r = HTTP_SESSION.get(img_url, timeout=20)
                        with open(file_path, 'wb') as f:
                            f.write(r.content)
                        saved_files.append(file_path)
                        if callback and total > 0:
                            try:
                                percent = int(((i + 1) / total) * 100)
                                callback.onProgressDetailed(percent, 0, 0)
                            except Exception: pass

                    return {
                        "status": "success",
                        "file_path": saved_files[0] if saved_files else "",
                        "all_files": json.dumps(saved_files),
                        "is_photo": True
                    }

                video_url = data.get("video_url")
                if video_url:
                    file_path = os.path.join(output_dir, f"{title}.mp4")
                    headers = {
                        'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15',
                        'Referer': 'https://www.tiktok.com/'
                    }
                    r = HTTP_SESSION.get(video_url, headers=headers, stream=True, timeout=30)
                    total_size = int(r.headers.get('content-length', 0))
                    downloaded = 0

                    with open(file_path, 'wb') as f:
                        for chunk in r.iter_content(chunk_size=CHUNK_SIZE):
                            check_cancellation()
                            if chunk:
                                f.write(chunk)
                                downloaded += len(chunk)
                                if callback and total_size > 0:
                                    try:
                                        percent = int((downloaded / total_size) * 100)
                                        callback.onProgressDetailed(percent, 0, 0)
                                    except Exception: pass

                    if callback:
                        try: callback.onProgressDetailed(100, 0, 0)
                        except Exception: pass

                    return {
                        "status": "success",
                        "file_path": file_path,
                        "all_files": json.dumps([file_path]),
                        "is_photo": False
                    }
            else:
                return {
                    "status": "error",
                    "error_msg": "TikTok временно ограничил доступ к этому видео."
                }

        # 2. ДРУГИЕ САЙТЫ (YT-DLP)
        global YT_DLP_CORE
        if YT_DLP_CORE is None:
            pre_load()

        yt_dlp = YT_DLP_CORE
        def progress_hook(d):
            check_cancellation()
            if callback and d.get('status') == 'downloading':
                try:
                    total = d.get('total_bytes') or d.get('total_bytes_estimate') or 0
                    downloaded = d.get('downloaded_bytes', 0)
                    speed = d.get('speed') or 0
                    eta = d.get('eta') or 0
                    percent = int((downloaded / total) * 100) if total > 0 else 0
                    callback.onProgressDetailed(percent, int(speed), int(eta))
                except Exception: pass
            elif callback and d.get('status') == 'finished':
                try: callback.onProgressDetailed(100, 0, 0)
                except Exception: pass

        has_ffmpeg = bool(ffmpeg_path and os.path.exists(ffmpeg_path))

        if is_audio_only:
            format_str = 'bestaudio[ext=m4a]/bestaudio/best'
            format_sort_rules = ['abr', 'ext:m4a:mp3']
        elif quality.startswith("transit_opt"):
            parts = quality.split(":")
            res = parts[1] if len(parts) > 1 else "1080"
            codec = parts[2] if len(parts) > 2 else "h264"
            vcodec_clause = "[vcodec^=avc1]" if codec == "h264" else ("[vcodec^=hev1]" if codec == "h265" else "")
            if has_ffmpeg:
                format_str = f'bestvideo[height<={res}]{vcodec_clause}+bestaudio/bestvideo[height<={res}]+bestaudio/best[height<={res}][ext=mp4]/best[height<={res}]/best'
            else:
                format_str = f'best[height<={res}][ext=mp4]/best[height<={res}]/best'
            format_sort_rules = [f'res:{res}', 'ext:mp4:m4a']
        else:
            if quality in ["2160", "1440", "1080", "720", "480", "360", "240", "144"]:
                h = quality
                if has_ffmpeg:
                    format_str = f'bestvideo[height<={h}]+bestaudio/best[height<={h}]/bestvideo+bestaudio/best'
                else:
                    format_str = f'best[height<={h}]/bestvideo[height<={h}]+bestaudio/best'
                format_sort_rules = [f'res:{h}', 'ext:mp4:m4a']
            else:
                if has_ffmpeg:
                    format_str = 'bestvideo+bestaudio/best'
                else:
                    format_str = 'best/bestvideo+bestaudio'
                format_sort_rules = ['res', 'ext:mp4:m4a']

        # ⭐️ Improved template for uniqueness and safety
        if custom_filename:
            # Clean custom filename just in case
            safe_custom = re.sub(r'[^\w\s\-\(\)\[\]]', "", custom_filename).strip()[:50]
            out_template = os.path.join(output_dir, f"{safe_custom} [%(id)s].%(ext)s")
        else:
            out_template = os.path.join(output_dir, '%(title).100s [%(id)s].%(ext)s')

        ydl_opts = {
            'outtmpl': out_template,
            'format': format_str,
            'format_sort': format_sort_rules,

            # 🚀 ТУРБО-РЕЖИМ: Многопоточность фрагментов (до 16 соединений на 1 файл)
            'concurrent_fragment_downloads': max(1, int(threads * 2)),
            'buffersize': 1024 * 256, # Увеличенный буфер для плавности (256 KB)

            'progress_hooks': [progress_hook],
            'quiet': True,
            'no_warnings': True,
            'nocheckcertificate': True,
            'extractor_args': {
                'youtube': {
                    'player_client': ['android', 'ios', 'mweb', 'tv'],
                    'player_skip': ['web'] if use_impersonate else []
                }
            }
        }

        # ✂️ SPONSORBLOCK: Автоматически вырезаем рекламные интеграции, заставки и самопиар
        if use_sponsorblock:
            ydl_opts['sponsorblock_remove'] = ['sponsor', 'intro', 'outro', 'selfpromo', 'preview', 'filler']

        # 🛡️ АНТИ-БЛОКИРОВКА: Подмена TLS-отпечатка браузера Chrome
        apply_impersonate_or_headers(ydl_opts, use_impersonate)

        # 🧠 АЛГОРИТМ "УМНОЙ СКЛЕЙКИ" (Fast Remuxing)
        if has_ffmpeg:
            ydl_opts['ffmpeg_location'] = ffmpeg_path
            ydl_opts['postprocessor_args'] = {
                'ffmpeg': [
                    '-c', 'copy',          # Копируем потоки без пережатия (МГНОВЕННО)
                    '-map', '0',           # Сохраняем все метаданные
                    '-movflags', 'faststart' # Оптимизируем MP4 для быстрого старта видео
                ]
            }
            ydl_opts['merge_output_format'] = 'mp4/mkv'

        if int(rate_limit) > 0:
            ydl_opts['ratelimit'] = int(rate_limit)
        if int(throttled_rate) > 0:
            ydl_opts['throttled_rate'] = int(throttled_rate)

        if cookie_path and os.path.exists(cookie_path):
            ydl_opts['cookiefile'] = cookie_path

        def run_ytdlp_download():
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = None
                if passport_path and os.path.exists(passport_path):
                    try:
                        with open(passport_path, 'r', encoding='utf-8') as f:
                            info = json.load(f)
                    except: pass

                if info:
                    try:
                        ydl.process_info(info)
                    except Exception as pe:
                        info = ydl.extract_info(url, download=True)
                else:
                    info = ydl.extract_info(url, download=True)

                filename = ydl.prepare_filename(info)

                if has_ffmpeg and not is_audio_only:
                    base_name = os.path.splitext(filename)[0]
                    if os.path.exists(f"{base_name}.mp4"):
                        filename = f"{base_name}.mp4"

                return {
                    "status": "success",
                    "file_path": filename,
                    "all_files": json.dumps([filename]),
                    "is_photo": False
                }

        try:
            return run_ytdlp_download()
        except Exception as imp_err:
            if "Impersonate target" in str(imp_err) or "is not available" in str(imp_err):
                ydl_opts.pop('impersonate', None)
                return run_ytdlp_download()
            else:
                raise imp_err

    except Exception as e:
        err_msg = str(e)
        if "DOWNLOAD_CANCELLED_BY_USER" in err_msg:
            try:
                for f in os.listdir(output_dir):
                    if f.endswith(".part") or f.endswith(".ytdl") or f.endswith(".tmp"):
                        os.remove(os.path.join(output_dir, f))
            except Exception: pass
            return {"status": "cancelled", "error_msg": "Загрузка прервана пользователем"}

        return {
            "status": "error",
            "error_msg": err_msg,
            "trace": traceback.format_exc()
        }
    finally:
        gc.collect()