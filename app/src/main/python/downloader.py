import yt_dlp
import gallery_dl
import json
import os
import sys
import threading

# Thread lock to prevent concurrent extraction crashes (generator already executing)
extraction_lock = threading.Lock()

def extract_info(url, quality='720', mode='auto', engine='yt-dlp', cookies_path=None):
    if engine == 'gallery-dl':
        return extract_gallery(url, cookies_path)
    else:
        return extract_video(url, quality, mode, cookies_path)

def extract_video(url, quality='720', mode='auto', cookies_path=None):
    with extraction_lock:
        # Robust configuration for direct stream extraction
        ydl_opts = {
            'quiet': False, 
            'no_warnings': False,
            'nocheckcertificate': True,
            'user_agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
            'http_headers': {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
                'Accept-Language': 'en-us,en;q=0.5',
                'Referer': 'https://www.google.com/',
            },
            'socket_timeout': 60,
            'extract_flat': False, # Resolve direct URLs
        }
        
        # Configure format to prefer single-file formats (merged) since we use Ktor for downloading
        if mode == 'audio':
            ydl_opts['format'] = 'bestaudio/best'
        else:
            ydl_opts['format'] = 'bestvideo+bestaudio/best'
            if quality != 'max':
                q_val = quality.replace('p', '') if isinstance(quality, str) else quality
                ydl_opts['format_sort'] = [f'res:{q_val}', 'ext:mp4:m4a']
            else:
                ydl_opts['format_sort'] = ['ext:mp4:m4a']

        if cookies_path and os.path.exists(cookies_path):
            ydl_opts['cookiefile'] = cookies_path

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            try:
                info = ydl.extract_info(url.strip(), download=False)
                
                # Handle playlists or multi-video entries
                video = info['entries'][0] if 'entries' in info and len(info['entries']) > 0 else info
                
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
                    # Fallback to the best resolved format URL
                    formats = [f for f in video['formats'] if f.get('url')]
                    if formats:
                        download_url = formats[-1].get('url') # 'best' is usually at the end

                if not download_url:
                    return json.dumps({'status': 'error', 'message': 'Could not resolve a direct download URL.'})

                return json.dumps({
                    'status': 'success',
                    'url': download_url,
                    'audio_url': audio_url,
                    'title': video.get('title', 'video'),
                    'author': video.get('uploader') or video.get('channel') or 'Unknown',
                    'thumbnail': video.get('thumbnail'),
                    'size': video.get('filesize_approx') or video.get('filesize') or 0,
                    'ext': 'mp3' if mode == 'audio' else video.get('ext', 'mp4')
                })
            except Exception as e:
                return json.dumps({'status': 'error', 'message': f"yt-dlp error: {str(e)}"})

def extract_gallery(url, cookies_path=None):
    with extraction_lock:
        try:
            from gallery_dl import job
            import gallery_dl
            gallery_dl.config.load()
            gallery_dl.config.set(("extractor",), "base-directory", ".")
            if cookies_path and os.path.exists(cookies_path):
                gallery_dl.config.set(("extractor",), "cookies", cookies_path)
            
            # Enable URL resolution (resolve=True) to follow redirects/short URLs
            j = job.DataJob(url, resolve=True)
            j.run()
            
            if j.data_urls:
                # Filter out any non-direct URLs that might still be in the list
                direct_urls = [u for u in j.data_urls if not any(domain in u for domain in ["pinterest.com", "pin.it"])]
                
                if not direct_urls:
                    # If all resolved URLs are still page links, fall back to the first available URL
                    direct_urls = j.data_urls
                
                first_meta = j.data_meta[0] if j.data_meta else {}
                
                return json.dumps({
                    'status': 'success',
                    'urls': direct_urls,
                    'title': first_meta.get('title') or 'Gallery Image',
                    'author': first_meta.get('author') or 'Unknown',
                    'thumbnail': direct_urls[0],
                    'size': 0,
                    'ext': first_meta.get('extension') or 'jpg',
                    'is_gallery': True
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
        # Check if Chaquopy pip internal is available
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

        # Monkey patch pip user_agent to prevent AssetPath crash in Chaquopy
        try:
            import pip._internal.network.session
            pip._internal.network.session.user_agent = lambda *args, **kwargs: "pip/chaquopy"
        except Exception:
            pass

        # Capture output
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

def download_video(url, output_path, cookies_path=None):
    try:
        import yt_dlp
        ydl_opts = {
            'outtmpl': output_path,
            'quiet': True,
            'no_warnings': True
        }
        if cookies_path:
            ydl_opts['cookiefile'] = cookies_path
            
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])
            
        return json.dumps({'status': 'success', 'path': output_path})
    except Exception as e:
        return json.dumps({'status': 'error', 'message': str(e)})
