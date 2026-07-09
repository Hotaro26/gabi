import yt_dlp
import gallery_dl
import json
import os
import sys
import threading

# Thread lock to prevent concurrent extraction crashes (generator already executing)
extraction_lock = threading.Lock()

def extract_info(url, quality='720', mode='auto', engine='yt-dlp'):
    if engine == 'gallery-dl':
        return extract_gallery(url)
    else:
        return extract_video(url, quality, mode)

def extract_video(url, quality='720', mode='auto'):
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
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            try:
                info = ydl.extract_info(url.strip(), download=False)
                
                # Handle playlists or multi-video entries
                video = info['entries'][0] if 'entries' in info and len(info['entries']) > 0 else info
                
                formats = video.get('formats', [])
                
                # 1. Determine all available video qualities and max_resolution
                available_qualities = []
                max_res = 0
                for fmt in formats:
                    h = fmt.get('height')
                    if h and isinstance(h, int):
                        if h > max_res:
                            max_res = h
                        res_str = f"{h}p"
                        if res_str not in available_qualities:
                            available_qualities.append(res_str)
                
                # Sort descending
                available_qualities.sort(key=lambda x: int(x.replace('p', '')), reverse=True)
                max_res_str = f"{max_res}p" if max_res > 0 else None

                video_url = None
                audio_url = None
                
                # 2. Select formats based on mode & requested quality
                if mode == 'audio':
                    audio_fmts = [f for f in formats if f.get('acodec') != 'none' and f.get('vcodec') == 'none' and f.get('url')]
                    if audio_fmts:
                        # Prefer m4a for compatibility
                        m4a_audio = [f for f in audio_fmts if f.get('ext') == 'm4a']
                        if m4a_audio:
                            audio_fmts = m4a_audio
                        audio_fmts.sort(key=lambda x: x.get('abr') or x.get('bitrate') or 0)
                        video_url = audio_fmts[-1].get('url')
                    else:
                        video_url = video.get('url')
                else:
                    video_fmts = [f for f in formats if f.get('vcodec') != 'none' and f.get('url')]
                    if video_fmts:
                        if quality == 'max':
                            video_fmts.sort(key=lambda x: (x.get('height') or 0, x.get('tbr') or x.get('bitrate') or 0))
                            selected_video = video_fmts[-1]
                        else:
                            try:
                                target_h = int(quality.replace('p', ''))
                            except ValueError:
                                target_h = 720
                            
                            matching_fmts = [f for f in video_fmts if f.get('height') and f.get('height') <= target_h]
                            if matching_fmts:
                                matching_fmts.sort(key=lambda x: (x.get('height') or 0, x.get('tbr') or x.get('bitrate') or 0))
                                selected_video = matching_fmts[-1]
                            else:
                                video_fmts.sort(key=lambda x: (x.get('height') or 0, x.get('tbr') or x.get('bitrate') or 0))
                                selected_video = video_fmts[-1]
                        
                        video_url = selected_video.get('url')
                        
                        if selected_video.get('acodec') == 'none':
                            audio_fmts = [f for f in formats if f.get('acodec') != 'none' and f.get('vcodec') == 'none' and f.get('url')]
                            if audio_fmts:
                                m4a_audio = [f for f in audio_fmts if f.get('ext') == 'm4a']
                                if m4a_audio:
                                    audio_fmts = m4a_audio
                                audio_fmts.sort(key=lambda x: x.get('abr') or x.get('bitrate') or 0)
                                audio_url = audio_fmts[-1].get('url')
                    else:
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

def extract_gallery(url):
    with extraction_lock:
        try:
            from gallery_dl import job
            import gallery_dl
            gallery_dl.config.load()
            gallery_dl.config.set(("extractor",), "base-directory", ".")
            
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
