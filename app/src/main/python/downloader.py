import yt_dlp
import gallery_dl
import json
import os
import sys
import threading

# Thread lock to prevent concurrent extraction crashes (generator already executing)
extraction_lock = threading.Lock()

def parse_custom_headers(custom_headers_json):
    if not custom_headers_json:
        return {}
    try:
        if isinstance(custom_headers_json, dict):
            return custom_headers_json
        headers_data = json.loads(custom_headers_json)
        result = {}
        if isinstance(headers_data, list):
            for item in headers_data:
                if isinstance(item, dict) and item.get('isEnabled', True):
                    k = item.get('key')
                    v = item.get('value')
                    if k and v:
                        result[k.strip()] = v.strip()
        elif isinstance(headers_data, dict):
            result = headers_data
        return result
    except Exception:
        return {}

def is_playlist_url(url):
    if not url:
        return False
    u = url.strip().lower()
    return ('list=' in u or 
            '/playlist' in u or 
            '/sets/' in u or 
            '/album/' in u or 
            '/albums/' in u)

def extract_info(url, quality='720', mode='auto', engine='yt-dlp', cookies_path=None, user_agent=None, custom_headers_json=None):
    if is_playlist_url(url):
        return extract_playlist(url, cookies_path, user_agent, custom_headers_json)
    if engine == 'gallery-dl':
        return extract_gallery(url, cookies_path, custom_headers_json)
    else:
        return extract_video(url, quality, mode, cookies_path, user_agent, custom_headers_json)

def extract_playlist(url, cookies_path=None, user_agent=None, custom_headers_json=None):
    with extraction_lock:
        ydl_opts = {
            'quiet': True,
            'no_warnings': True,
            'nocheckcertificate': True,
            'socket_timeout': 30,
            'extract_flat': 'in_playlist',
            'playlistend': 150,
            'skip_download': True,
            'ignoreerrors': True
        }

        headers = {
            'User-Agent': user_agent or 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language': 'en-us,en;q=0.5',
        }
        custom = parse_custom_headers(custom_headers_json)
        headers.update(custom)
        ydl_opts['http_headers'] = headers

        if cookies_path and os.path.exists(cookies_path):
            ydl_opts['cookiefile'] = cookies_path

        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url.strip(), download=False)
                if not info:
                    return json.dumps({'status': 'error', 'message': 'No playlist information found'})

                raw_entries = info.get('entries')
                entries = []
                if raw_entries is not None:
                    try:
                        entries = [e for e in raw_entries if e]
                    except Exception:
                        entries = []
                
                items = []
                for idx, entry in enumerate(entries):
                    if not entry:
                        continue
                    entry_id = str(entry.get('id') or (idx + 1))
                    entry_url = entry.get('url') or entry.get('webpage_url')
                    # Flat extraction often returns video ID as entry_url or partial path
                    if not entry_url or not str(entry_url).startswith('http'):
                        if 'youtube' in url or 'youtu.be' in url or len(entry_id) == 11:
                            entry_url = f"https://www.youtube.com/watch?v={entry_id}"
                        elif entry_url and not str(entry_url).startswith('http'):
                            entry_url = f"https://www.youtube.com/watch?v={entry_url}"
                        else:
                            entry_url = f"https://www.youtube.com/watch?v={entry_id}"

                    thumb = entry.get('thumbnail')
                    if not thumb and entry.get('thumbnails'):
                        try:
                            thumb = entry['thumbnails'][0].get('url')
                        except Exception:
                            thumb = None
                    if not thumb and ('youtube' in url or 'youtu.be' in url or len(entry_id) == 11):
                        thumb = f"https://i.ytimg.com/vi/{entry_id}/hqdefault.jpg"

                    duration_sec = entry.get('duration')
                    duration_str = None
                    if duration_sec is not None:
                        try:
                            m, s = divmod(int(duration_sec), 60)
                            h, m = divmod(m, 60)
                            duration_str = f"{h}:{m:02d}:{s:02d}" if h else f"{m}:{s:02d}"
                        except Exception:
                            duration_str = str(duration_sec)

                    items.append({
                        'id': entry_id,
                        'url': str(entry_url),
                        'title': entry.get('title') or f"Track {idx + 1}",
                        'author': entry.get('uploader') or entry.get('channel') or info.get('uploader') or 'Unknown',
                        'thumbnail': thumb,
                        'duration': duration_str,
                        'isSelected': True
                    })

                playlist_title = info.get('title') or 'Playlist'
                return json.dumps({
                    'status': 'success',
                    'is_playlist': True,
                    'playlist_id': info.get('id') or 'playlist',
                    'playlist_title': playlist_title,
                    'playlist_items': items,
                    'title': playlist_title,
                    'author': info.get('uploader') or info.get('channel') or 'Various Artists',
                    'thumbnail': items[0].get('thumbnail') if items else None
                })
        except Exception as e:
            return json.dumps({'status': 'error', 'message': f"Playlist extraction failed: {str(e)}"})

def extract_video(url, quality='720', mode='auto', cookies_path=None, user_agent=None, custom_headers_json=None):
    with extraction_lock:
        # Robust configuration for direct stream extraction
        ydl_opts = {
            'quiet': False, 
            'no_warnings': False,
            'nocheckcertificate': True,
            'socket_timeout': 45,
            'extract_flat': False,
            'retries': 3
        }
        
        default_headers = {
            'User-Agent': user_agent or 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language': 'en-us,en;q=0.5',
        }
        custom = parse_custom_headers(custom_headers_json)
        default_headers.update(custom)
        ydl_opts['http_headers'] = default_headers
        
        if mode == 'audio':
            ydl_opts['format'] = 'bestaudio/best'
        else:
            # We MUST strictly enforce compatible codecs for Android MediaMuxer!
            # MP4 Muxer strictly requires AVC (H.264) + AAC (M4A).
            # WEBM Muxer strictly requires VP9/VP8 + Opus/Vorbis (WEBM).
            q_val = str(quality).replace('p', '').strip()
            if q_val.isdigit():
                ydl_opts['format'] = (
                    f'bestvideo[vcodec^=avc][protocol^=http][height={q_val}]+bestaudio[ext=m4a][protocol^=http]/'
                    f'bestvideo[vcodec^=vp9][protocol^=http][height={q_val}]+bestaudio[ext=webm][protocol^=http]/'
                    f'bestvideo[vcodec^=avc][protocol^=http][height<={q_val}]+bestaudio[ext=m4a][protocol^=http]/'
                    f'bestvideo[vcodec^=vp9][protocol^=http][height<={q_val}]+bestaudio[ext=webm][protocol^=http]/'
                    f'best[ext=mp4][protocol^=http][height<={q_val}]/'
                    f'best[protocol^=http][height<={q_val}]/'
                    f'best[ext=mp4][protocol^=http]/'
                    f'best[protocol^=http]/'
                    f'bestvideo[vcodec^=avc][height<={q_val}]+bestaudio[ext=m4a]/'
                    f'best[height<={q_val}]/bestvideo+bestaudio/best'
                )
            else:
                ydl_opts['format'] = (
                    'bestvideo[vcodec^=avc][protocol^=http]+bestaudio[ext=m4a][protocol^=http]/'
                    'bestvideo[vcodec^=vp9][protocol^=http]+bestaudio[ext=webm][protocol^=http]/'
                    'best[ext=mp4][protocol^=http]/'
                    'best[protocol^=http]/'
                    'bestvideo[vcodec^=avc]+bestaudio[ext=m4a]/'
                    'bestvideo[vcodec^=vp9]+bestaudio[ext=webm]/'
                    'bestvideo+bestaudio/best'
                )

        if cookies_path and os.path.exists(cookies_path):
            ydl_opts['cookiefile'] = cookies_path

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            try:
                info = ydl.extract_info(url.strip(), download=False)
                if not info:
                    return json.dumps({'status': 'error', 'message': 'Could not extract metadata.'})
                
                # Safely resolve entries if info is a playlist or contains multiple tracks
                raw_entries = info.get('entries')
                entries = []
                if raw_entries is not None:
                    try:
                        entries = [e for e in raw_entries if e]
                    except Exception:
                        entries = []

                is_full_playlist = info.get('_type') == 'playlist' or (
                    len(entries) > 1 and is_playlist_url(url)
                )

                if is_full_playlist:
                    items = []
                    for idx, entry in enumerate(entries):
                        if not entry:
                            continue
                        e_id = str(entry.get('id') or (idx + 1))
                        e_url = entry.get('url') or entry.get('webpage_url')
                        if not e_url or not str(e_url).startswith('http'):
                            if 'youtube' in url or 'youtu.be' in url or len(e_id) == 11:
                                e_url = f"https://www.youtube.com/watch?v={e_id}"
                            elif e_url and not str(e_url).startswith('http'):
                                e_url = f"https://www.youtube.com/watch?v={e_url}"
                            else:
                                e_url = f"https://www.youtube.com/watch?v={e_id}"
                        thumb = entry.get('thumbnail')
                        if not thumb and entry.get('thumbnails'):
                            try:
                                thumb = entry['thumbnails'][0].get('url')
                            except Exception:
                                thumb = None
                        if not thumb and ('youtube' in url or 'youtu.be' in url or len(e_id) == 11):
                            thumb = f"https://i.ytimg.com/vi/{e_id}/hqdefault.jpg"

                        duration_sec = entry.get('duration')
                        duration_str = None
                        if duration_sec is not None:
                            try:
                                m, s = divmod(int(duration_sec), 60)
                                h, m = divmod(m, 60)
                                duration_str = f"{h}:{m:02d}:{s:02d}" if h else f"{m}:{s:02d}"
                            except Exception:
                                duration_str = str(duration_sec)
                        items.append({
                            'id': e_id,
                            'url': str(e_url),
                            'title': entry.get('title') or f"Track {idx + 1}",
                            'author': entry.get('uploader') or entry.get('channel') or info.get('uploader') or 'Unknown',
                            'thumbnail': thumb,
                            'duration': duration_str,
                            'isSelected': True
                        })

                    pl_title = info.get('title') or 'Playlist'
                    return json.dumps({
                        'status': 'success',
                        'is_playlist': True,
                        'playlist_id': info.get('id') or 'playlist',
                        'playlist_title': pl_title,
                        'playlist_items': items,
                        'title': pl_title,
                        'author': info.get('uploader') or info.get('channel') or 'Various Artists',
                        'thumbnail': items[0].get('thumbnail') if items else None
                    })

                # Single video or entry
                video = entries[0] if entries else info
                
                # Find the best direct URL
                download_url = video.get('url')
                audio_url = None
                
                if 'requested_formats' in video:
                    for f in video['requested_formats']:
                        if f.get('vcodec') != 'none' and f.get('acodec') == 'none':
                            download_url = f.get('url')
                        elif f.get('acodec') != 'none' and f.get('vcodec') == 'none':
                            audio_url = f.get('url')
                    if not download_url or not audio_url:
                        for f in video['requested_formats']:
                            if f.get('vcodec') != 'none': download_url = f.get('url')
                            if f.get('acodec') != 'none': audio_url = f.get('url')
                            
                # If both point to the exact same file, it's already merged.
                if download_url and audio_url and download_url == audio_url:
                    audio_url = None

                if not download_url and 'formats' in video:
                    # Fallback to the best resolved direct HTTP format URL (avoid m3u8 if possible)
                    formats = [f for f in video['formats'] if f.get('url')]
                    direct_formats = [f for f in formats if 'm3u8' not in str(f.get('protocol', '')).lower() and '.m3u8' not in str(f.get('url', '')).lower()]
                    pool = direct_formats if direct_formats else formats
                    if pool:
                        download_url = pool[-1].get('url') # 'best' is usually at the end

                # If requested_formats picked an m3u8 playlist, prefer a direct HTTP progressive format if available
                if download_url and ('.m3u8' in str(download_url).lower() or (audio_url and '.m3u8' in str(audio_url).lower())) and 'formats' in video:
                    direct_prog = [
                        f for f in video['formats']
                        if f.get('url') and f.get('vcodec') != 'none' and f.get('acodec') != 'none'
                        and 'm3u8' not in str(f.get('protocol', '')).lower()
                        and '.m3u8' not in str(f.get('url', '')).lower()
                    ]
                    if direct_prog:
                        best_direct = sorted(direct_prog, key=lambda x: x.get('height') or 0)[-1]
                        download_url = best_direct.get('url')
                        audio_url = None
                    else:
                        # Prefer single m3u8 progressive format if available so we don't try to MediaMuxer two m3u8 playlists
                        m3u8_prog = [
                            f for f in video['formats']
                            if f.get('url') and f.get('vcodec') != 'none' and f.get('acodec') != 'none'
                        ]
                        if m3u8_prog:
                            best_m3u8 = sorted(m3u8_prog, key=lambda x: x.get('height') or 0)[-1]
                            download_url = best_m3u8.get('url')
                            audio_url = None

                # Extract available qualities
                qualities = set()
                if 'formats' in video:
                    for f in video['formats']:
                        h = f.get('height')
                        if h and f.get('vcodec') != 'none':
                            qualities.add(f"{h}p")
                sorted_qualities = sorted(list(qualities), key=lambda x: int(x.replace('p', ''))) if qualities else []

                if not download_url:
                    return json.dumps({'status': 'error', 'message': 'Could not resolve a direct download URL.'})

                # Find http_headers for the chosen URL
                target_headers = video.get('http_headers', {})
                if 'formats' in video:
                    for fmt in video['formats']:
                        if fmt.get('url') == download_url and fmt.get('http_headers'):
                            target_headers = fmt.get('http_headers')
                            break

                # Robust metadata extraction across major platforms (YouTube, Instagram, TikTok, Twitter/X)
                raw_title = video.get('title')
                if not raw_title or str(raw_title).strip() == '' or raw_title == 'video':
                    desc = video.get('description')
                    if desc and str(desc).strip():
                        raw_title = str(desc).strip().split('\n')[0][:80]
                    else:
                        raw_title = video.get('id') or 'Media'

                raw_author = (video.get('uploader') or 
                              video.get('uploader_id') or 
                              video.get('channel') or 
                              video.get('creator') or 
                              'Unknown')

                raw_thumb = video.get('thumbnail')
                if not raw_thumb and video.get('thumbnails'):
                    try:
                        raw_thumb = video['thumbnails'][-1].get('url') or video['thumbnails'][0].get('url')
                    except Exception:
                        raw_thumb = None

                return json.dumps({
                    'available_qualities': sorted_qualities,
                    'http_headers': target_headers,
                    'status': 'success',
                    'url': download_url,
                    'audio_url': audio_url,
                    'title': str(raw_title),
                    'author': str(raw_author),
                    'thumbnail': raw_thumb,
                    'size': video.get('filesize_approx') or video.get('filesize') or 0,
                    'ext': 'mp3' if mode == 'audio' else video.get('ext', 'mp4'),
                    'is_playlist': False
                })
            except Exception as e:
                return json.dumps({'status': 'error', 'message': f"yt-dlp error: {str(e)}"})

def extract_gallery(url, cookies_path=None, custom_headers_json=None):
    with extraction_lock:
        try:
            from gallery_dl import job
            import gallery_dl
            gallery_dl.config.load()
            gallery_dl.config.set(("extractor",), "base-directory", ".")
            if cookies_path and os.path.exists(cookies_path):
                gallery_dl.config.set(("extractor",), "cookies", cookies_path)
            
            custom_headers = parse_custom_headers(custom_headers_json)
            if custom_headers:
                gallery_dl.config.set(("extractor",), "headers", custom_headers)

            # Enable URL resolution (resolve=True) to follow redirects/short URLs
            j = job.DataJob(url, resolve=True)
            j.run()
            
            if j.data_urls:
                direct_urls = [u for u in j.data_urls if not any(domain in u for domain in ["pinterest.com", "pin.it"])]
                if not direct_urls:
                    direct_urls = j.data_urls

                # Filter out HLS m3u8 playlists if direct mp4 exists, or fall back to yt-dlp for m3u8-only video pins
                non_m3u8 = [u for u in direct_urls if '.m3u8' not in u.lower().split('?')[0]]
                if not non_m3u8 and any('.m3u8' in u.lower() for u in direct_urls):
                    return json.dumps({'status': 'error', 'message': 'HLS stream detected; falling back to video extractor'})
                if non_m3u8:
                    direct_urls = non_m3u8

                first_meta = j.data_meta[0] if j.data_meta else {}
                first_path = direct_urls[0].lower().split('?')[0]
                detected_ext = first_meta.get('extension')
                if not detected_ext:
                    for candidate_ext in ('mp4', 'webm', 'mov', 'gif', 'png', 'webp', 'jpg', 'jpeg'):
                        if first_path.endswith('.' + candidate_ext):
                            detected_ext = candidate_ext
                            break
                if not detected_ext:
                    detected_ext = 'jpg'

                is_video_file = detected_ext in ('mp4', 'webm', 'mov') and len(direct_urls) == 1

                return json.dumps({
                    'status': 'success',
                    'urls': direct_urls if not is_video_file else None,
                    'url': direct_urls[0] if is_video_file else None,
                    'title': first_meta.get('title') or ('Pinterest Video' if is_video_file else 'Gallery Image'),
                    'author': first_meta.get('author') or first_meta.get('uploader') or 'Unknown',
                    'thumbnail': direct_urls[0] if not is_video_file else first_meta.get('thumbnail'),
                    'size': 0,
                    'ext': detected_ext,
                    'is_gallery': not is_video_file,
                    'is_playlist': False
                })
            else:
                return json.dumps({'status': 'error', 'message': 'No images found in gallery'})
        except Exception as e:
            return json.dumps({'status': 'error', 'message': str(e)})

def get_versions():
    try:
        yt_dlp_version = None
        gallery_dl_version = None
        import os
        import sys
        
        try:
            for p in sys.path:
                if 'python_packages' in p:
                    v_file = os.path.join(p, 'yt_dlp', 'version.py')
                    if os.path.exists(v_file):
                        with open(v_file, 'r') as f:
                            for line in f:
                                if '__version__' in line:
                                    yt_dlp_version = line.split('=')[1].strip().strip("'").strip('"')
                                    break
                    g_file = os.path.join(p, 'gallery_dl', 'version.py')
                    if os.path.exists(g_file):
                        with open(g_file, 'r') as f:
                            for line in f:
                                if line.startswith('__version__'):
                                    gallery_dl_version = line.split('=')[1].strip().strip("'").strip('"')
                                    break
        except Exception:
            pass

        if not yt_dlp_version:
            import yt_dlp.version
            yt_dlp_version = yt_dlp.version.__version__
            
        if not gallery_dl_version:
            import gallery_dl
            gallery_dl_version = gallery_dl.__version__

        return json.dumps({
            'status': 'success',
            'yt_dlp': yt_dlp_version,
            'gallery_dl': gallery_dl_version
        })
    except Exception as e:
        return json.dumps({'status': 'error', 'message': str(e)})

def update_extractors(target_path):
    import io
    import sys
    try:
        try:
            from pip._internal.cli.main import main as pip_main
        except ImportError:
            try:
                from pip._internal import main as pip_main
            except ImportError:
                import pip
                pip_main = getattr(pip, "main", None)

        if not pip_main:
            return json.dumps({'status': 'error', 'message': 'Pip is not available in this environment'})

        try:
            import pip._internal.network.session
            pip._internal.network.session.user_agent = lambda *args, **kwargs: "pip/chaquopy"
        except Exception:
            pass

        old_stdout = sys.stdout
        old_stderr = sys.stderr
        new_out = io.StringIO()
        sys.stdout = new_out
        sys.stderr = new_out

        try:
            exit_code = pip_main(["install", "--upgrade", "--target", target_path, "yt-dlp", "gallery-dl"])
        except SystemExit as e:
            exit_code = e.code
        finally:
            sys.stdout = old_stdout
            sys.stderr = old_stderr
        
        output = new_out.getvalue()
        if exit_code == 0 or exit_code is None:
            return json.dumps({'status': 'success', 'message': output})
        else:
            return json.dumps({'status': 'error', 'message': f'Pip failed with code {exit_code}. Output: {output}'})
    except Exception as e:
        return json.dumps({'status': 'error', 'message': str(e)})

def download_video(url, output_path, cookies_path=None, custom_headers_json=None):
    try:
        import yt_dlp
        ydl_opts = {
            'outtmpl': output_path,
            'quiet': True,
            'no_warnings': True,
            'nocheckcertificate': True,
            'hls_prefer_native': True
        }
        if cookies_path:
            ydl_opts['cookiefile'] = cookies_path
        
        custom_headers = parse_custom_headers(custom_headers_json)
        if custom_headers:
            ydl_opts['http_headers'] = custom_headers
            
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])
            
        return json.dumps({'status': 'success', 'path': output_path})
    except Exception as e:
        return json.dumps({'status': 'error', 'message': str(e)})
