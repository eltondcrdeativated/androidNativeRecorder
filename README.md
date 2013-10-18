This is an android app that can do stereo recording on Galaxy nexus 4.2.2
===========================================================================================================

Dependency:
1. rooted phone
2. busybox: https://play.google.com/store/apps/details?id=stericson.busybox&hl=en
3. alsa-mixer: https://play.google.com/store/apps/details?id=com.skvalex.alsamixer&hl=en

===========================================================================================================
Usage:

1. for simply stereo audio recording:

start audio recording by clicking 'a' to 'z' buttons, not the ('start-only recording audio') button. 

When finishing recording (recording time is set by program, not user, default is 5 seconds) , a notification will pop up and the result file will be stored at: /sdcard/nativeRecorder/
with the file name same as the button pressed ('a' to 'z')

2. To exit the program:
   click 'exit' button

3. To change the recording length, frequency and wav file endian, change assets/script.jpg in this project:

default command:
/system/xbin/alsa_aplay -C -D hw:0,1 -c 2 -r 48000 -d 5 -f S16_LE

the above command uses hardware 0,1 to record 2 channels (-c), at 48000Hz (-r), 5 seconds (-d), and the output .wav file is in little endian (-f)
Details on formats of command above, refer to:
http://linux.die.net/man/1/aplay

===============================================================================================================
Default Recording Settings (IMPORTANT):
2 channels, 5 seconds, little endian for output .wav file

===============================================================================================================
How it works:
This app essentially issues alsa_amixer commands to bypass android OS and HAL to enable recording directly

===============================================================================================================
Updates:
I'm planning on making it user-friendly (terrible right now).... need some time... We'll see

================================================================================================================
Contact Author:
JJ Wang
canjian.myself@gmail.com
