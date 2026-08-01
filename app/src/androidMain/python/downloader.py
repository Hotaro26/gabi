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
        # Determine format string
        try:
            target_h = int(quality.replace('p', ''))
        except:
            target_h = 720
            
        if mode == 'audio':
            fmt_str = 'bestaudio[ext=m4a][protocol^=http]/bestaudio[protocol^=http]/best[protocol^=http]'
        else:
            if quality == 'max':
                fmt_str = 'bestvideo[protocol^=http]+bestaudio[protocol^=http]/best[protocol^=http]'
            else:
                fmt_str = f'bestvideo[height<={target_h}][protocol^=http]+bestaudio[protocol^=http]/best[height<={target_h}][protocol^=http]/best[protocol^=http]'

        ydl_opts = {
            'format': fmt_str,
            'quiet': False, 
            'no_warnings': False,
            'nocheckcertificate': True,
            'user_agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
            'socket_timeout': 60,
            'extract_flat': False,
        }
        
        if cookies_path:
            ydl_opts['cookiefile'] = cookies_path
            
        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url.strip(), download=False)
                video = info['entries'][0] if 'entries' in info and len(info['entries']) > 0 else info
                
                # Determine all available video qualities
                formats = video.get('formats', [])
                available_qualities = []
                max_res = 0
                for fmt in formats:
                    if fmt.get('protocol', '').startswith('http'):
                        h = fmt.get('height')
                        if h and isinstance(h, int):
                            if h > max_res:
                                max_res = h
                            res_str = f"{h}p"
                            if res_str not in available_qualities:
                                available_qualities.append(res_str)
                available_qualities.sort(key=lambda x: int(x.replace('p', '')), reverse=True)
                max_res_str = f"{max_res}p" if max_res > 0 else None

                video_url = None
                audio_url = None
                
                if 'requested_formats' in video:
                    # yt-dlp selected separate video and audio
                    req_formats = video['requested_formats']
                    video_url = req_formats[0].get('url')
                    audio_url = req_formats[1].get('url') if len(req_formats) > 1 else None
                else:
                    # yt-dlp selected a single merged format
                    video_url = video.get('url')
                    
                if not video_url:
                    return json.dumps({'status': 'error', 'message': 'Could not resolve a direct download URL.'})

                return json.dumps({
                    'status': 'success',
                    'url': video_url,
                    'audio_url': audio_url,
                    'title': video.get('title', 'video'),
                    'author': video.get('uploader') or video.get('channel') or 'Unknown',
                    'thumbnail': video.get('thumbnail'),
                    'size': video.get('filesize_approx') or video.get('filesize') or 0,
                    'ext': 'mp3' if mode == 'audio' else video.get('ext', 'mp4'),
                    'max_resolution': max_res_str,
                    'available_qualities': available_qualities
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
            
            if cookies_path:
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
