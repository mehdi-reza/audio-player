# audio-player

* Update nginx_audio.conf file with correct locations with respect to the folder where you cloned the repo
* Update src/main/resources/application.properties with correct locations
* Download nginx (webserver) for windows. This is required to properly handle stream headers.
* In nginx.conf include nginx_audio.conf in ``http{...}`` block, as following:
 
> ``include "C:/Users/Mehdi Raza/aamir/waveform/nginx_audio.conf";``

*  Start nginx using ``"start nginx"``
*  execute `mvn spring-boot:run` and point browser to <http://localhost:9991> 

Link to [ffmpeg](http://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-20170520-64ea4d1-win64-static.zip)