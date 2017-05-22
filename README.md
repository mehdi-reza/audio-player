# audio-player

1. Update nginx_audio.conf file with correct locations with respect to the folder where you cloned the repo
2. Update src/main/resources/application.properties with correct locations
3. Download nginx (webserver) for windows. This is required to properly handle stream headers.
4. In nginx.conf include nginx_audio.conf in ``http{...}`` block, as following...

``include "C:/Users/Mehdi Raza/aamir/waveform/nginx_audio.conf";``


execute `mvn spring-boot:run` and browser <http://localhost:9991> 

