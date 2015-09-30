Exhentai Downloader

Usage:

Edit "ex/conf.txt" first:

ipb_member_id		-	Cookie information, login then check browser cookies.
ipb_pass_hash		-	^
LOGGING_ENABLED		-	When 1, a log file is made detailing program state.
THREAD_DELAY		-	Delay between image downloads.
THREAD_DELAY_R		-	Upper limit to randomised delay added to ^.
THREAD_DELAY_H		-	Same as image delay, but for HTML requests.
THREAD_DELAY_H_R	-	Upper limit to randomised delay added to ^.	
TRANSFER_THRESHOLD_TIME	-	Specifies minimum time in seconds to check downloaded bytes.
TRANSFER_THRESHOLD_BYTES-	If bytes transferred < threshold over above time, download fails.
USER_AGENT		-	Used to spoof browser activity.

Adjust thread delay values to avoid bot detection, higher is safer but slower.

After this, edit "ex/list.txt":

Each line should contain a gallery URL, ie:

http://exhentai.org/g/123456/abcdefghij/

After this, run "ex.jar" and it'll start processing the list.
Alternatively, fire up the jar and it'll create an empty list if it doesn't already exist.