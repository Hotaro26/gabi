import yt_dlp
import gallery_dl
import json
import os
import sys

def extract_info(url, quality='720', mode='auto', engine='yt-dlp'):
    if engine == 'gallery-dl':
        return extract_gallery(url)
    else:
        return extract_video(url, quality, mode)

def extract_video(url, quality='720', mode='auto'):
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'nocheckcertificate': True,
        'user_agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        'socket_timeout': 30,
        'extract_flat': True,
    }
    
    # Configure format for yt-dlp
    if mode == 'audio':
        ydl_opts['format'] = 'bestaudio/best'
        ydl_opts['postprocessors'] = [{
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'mp3',
            'preferredquality': '192',
        }]
    else:
        if quality == 'max': ydl_opts['format'] = 'bestvideo+bestaudio/best' 
        else: ydl_opts['format'] = f'bestvideo[height<={quality}]+bestaudio/best[height<={quality}]/best'

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        try:
            info = ydl.extract_info(url, download=False)
            video = info['entries'][0] if 'entries' in info else info
            
            return json.dumps({
                'status': 'success',
                'url': video.get('url'),
                'title': video.get('title', 'video'),
                'author': video.get('uploader') or video.get('author') or 'Unknown',
                'thumbnail': video.get('thumbnail'),
                'size': video.get('filesize_approx') or video.get('filesize') or 0,
                'ext': 'mp3' if mode == 'audio' else video.get('ext', 'mp4')
            })
        except Exception as e:
            return json.dumps({'status': 'error', 'message': str(e)})

def extract_gallery(url):
    try:
        from gallery_dl import job
        import gallery_dl
        gallery_dl.config.load()
        gallery_dl.config.set(("extractor",), "base-directory", ".")
        
        j = job.DataJob(url)
        j.run()
        
        if j.data_urls:
            first_url = j.data_urls[0]
            first_meta = j.data_meta[0] if j.data_meta else {}
            
            return json.dumps({
                'status': 'success',
                'url': first_url,
                'title': first_meta.get('title') or 'Gallery Image',
                'author': first_meta.get('author') or 'Unknown',
                'thumbnail': first_url,
                'size': 0,
                'ext': first_meta.get('extension') or 'jpg',
                'is_gallery': True
            })
        else:
            return json.dumps({'status': 'error', 'message': 'No images found in gallery'})
    except Exception as e:
        return json.dumps({'status': 'error', 'message': str(e)})
