<!DOCTYPE html>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html lang="en">
	<head>
		<link rel="stylesheet" href="/css/font-awesome.min.css">
		<script src="https://code.jquery.com/jquery-3.2.1.min.js" integrity="sha256-hwg4gsxgFZhOsEEamdOYGBf13FyQuiTwlAQgxVSNgt4=" crossorigin="anonymous"></script>
		<script>
			var markerInterval=0, intervalId, duration, playing=false,
			reset=function() {
				resetMarker();		
				duration=document.getElementById('audio').duration;
				calculateMarketInterval();
				updateDuration();
				$("#controls, .marker").show();
				if(playing) {
					playing=false;
					$("#play i").removeClass("fa-pause-circle").addClass("fa-play-circle");
					$("#stop").enable(true);
				}
				$("#stop").enable(false);
			},
			resetMarker=function() {
				$('.marker').css({left:0, width:1});
			},
			toggleControls=function() {
				$('#controls, .marker').toggle();
			},
			updateDuration=function() {
				$('#duration').html(Math.floor(duration)+' seconds');
			},
			calculateMarketInterval=function() {
				markerInterval=(document.getElementById('audio').duration*1000)/640;
				clearInterval(intervalId);
			},
			play=function() {
				if(playing) {				
					document.getElementById('audio').pause();
					playing=false;
					$("#play i").removeClass("fa-pause-circle").addClass("fa-play-circle");
					clearInterval(intervalId);
				} else {
					document.getElementById('audio').play();
					playing=true;
					intervalId=setInterval(function() {
						var width=parseInt($('.marker').css('width'));
						$('.marker').css('width',width+1);
					},markerInterval+1);
					$("#play i").removeClass("fa-play-circle").addClass("fa-pause-circle");//disable();
				}
				$("#stop").enable(true);
			},
			stop=function() {
				if(playing) {
					$("#play i").removeClass("fa-pause-circle").addClass("fa-play-circle");
					resetMarker();
				}
				document.getElementById('audio').pause();
				document.getElementById('audio').currentTime=0;
				playing=false;
				clearInterval(intervalId);
				$("#play").enable(true);
				$("#stop").enable(false);
				resetMarker();
			}

			$.fn.enable=function(flag) {
				if(flag)
					this.removeAttr("disabled");
				else
					this.attr("disabled", "disabled");
			};

			$(function() {				
				document.getElementById('audio').addEventListener('loadedmetadata', reset);

				audio.onended=function(e) {
					clearInterval(intervalId);
					resetMarker();
					$('#tracktime').html("0 / "+duration);
					$("#stop").enable(false);
					$("#play i").removeClass("fa-pause-circle").addClass("fa-play-circle");
					playing=false;
				};
				
				$('.record').on('click', function() {					
					var recordName=$(this).text();
					$.ajax('/api/waveform', {
						method:'GET',
						data:{recordName:recordName},
						success:function(response) {
							$("img#waveform").attr('src', '/tmp/'+response.fileName);
							$('audio').attr({src:'/record/'+recordName});
							document.getElementById('audio').load();
							reset();
							$(".audio-info").html(response.stream);
						}
					});										
				});
				$('#play').on('click', play);
				$('#stop').on('click', stop);
			})		
		</script>
		<style>
			#controls {
				border-top:1px solid gray; 
				padding:4px;
			}
			.records {
				float:left; 
				width:15%;
				max-height:450px;
				/*overflow:scroll;*/
			}
			
			.record {
				padding:5px;
				margin:5px;
				border:1px solid #efefef;
				cursor: pointer;
			}
			
			.player-outer {
				float:right;
				width:84%;				
			}
			
			.player {
				height:120px;
				width:640px;
				margin:auto;							
			}
			
			.waveform {
				position: relative;
				overflow: hidden;
			}
			
			.waveform .marker {
			    display: inline-block;
			    width: 1px;
			    background-color: #b3b1b2;
			    left: 0px;
			    height: 120px;
			    position: absolute;
			}
			img#waveform {
				webkit-filter: grayscale(100%); /* Safari 6.0 - 9.0 */
	    		filter: grayscale(100%);
	    		opacity:.5;
	    	}
	    	
	    	.hidden {
	    		display:none !important;
	    	}
	    	
	    	#tracktime, #duration, .audio-info {
	    		font-size:10px;
	    		font-family:arial;
	    	}
	    	
	    	#duration, .audio-info {
	    		float:right;
	    		padding:5px;
	    		background-color:#efefef;
	    	}
		</style>
	</head>
	<body>
		<div class="container">
			<div class="records">
				<c:forEach items="${records}" var="r">
					<div class="record">${r}</div>
				</c:forEach>
			</div>
			
			<div class="player-outer">
				<div class="player">
					<div class="waveform">
						<span class="marker" style="display:none;"></span>
						<img id="waveform"/>
					</div>
					<audio id="audio" class="hidden"></audio>
					<div id="controls" style="display:none;">
						<button id="play"><i class="fa fa-play-circle" aria-hidden="true"></i></button>
						<button id="stop" disabled="disabled"><i class="fa fa-stop-circle" aria-hidden="true"></i></button>
						&nbsp;<span id="tracktime"></span>
						<span id="duration"></span><span class="audio-info" style="margin-right:100px">176 kb/s 16bit mono</span>
					</div>					
				</div>
			</div>
		</div>
	</body>
</html>