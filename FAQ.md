# Frequently Ask Questions

## How do I set up WebSMS?

 1. Install WebSMS:
  * Install WebSMS from market (there is a barcode on the main page).
  * Or go to download section and install it from there.
 2. Start WebSMS and open it's preferences via menu.
 3. Set sender and international prefix.
 4. Install Connectors:
  * Click the "more connectors" preferences. It will launch the market search.
  * Or install Connectors from this website.
 5. Restart WebSMS now!
 6. Go to preferences again.
 7. Above the "more connectors" preference you will find the preferences for all Connectors installed. Click them.
 8. For each Connector, you may set:
  * En-/disable the Connector.
  * Set username and password. (for some, the username is set to your sender you set above automatically)
  * Set a alternative sender, which may differ from the one set in main preferences. (This may set your username for some Connectors as well)
 9. Finally you should set the connector you want to use via menu or button (if not hidden). The chosen Connector is displayed in the text field in big grey letters.

## What Connector should I use?

If you own a account for one of the Connectors, use it. You may use your free sms there.

If you don't own an account, the answer differs on your location. At the moment most Connectors are working only for german or austrian people.
Fishtext is specialized on international SMS and should work for most people.

For backup purpose there is a SMS Connector shipped with the main app.
It will send real SMS via your carrier which are billed as normal.

You should read the [ConnectorMatrix](https://github.com/felixb/websms/blob/master/ConnectorMatrix.md) for more details.

## What is the password for sending a SMS?

It is the password you set on the Connectors website while registration.

## What is the website of a Connector?

Please use google. I am pretty sure, google knows the connectors website.

## What is the "prefix"?

You should enter your international country code here (E.g. +1 for US, +49 for Germany).
See the full list [wikipedia](http://en.wikipedia.org/wiki/Country_calling_code).

## I'm living abroad, what prefix should I use?

Please set the prefix of YOUR telephone number used for sender id and login.
The prefix is just needed to strip it from the international formatted number to login to some webservice using the national formatted number.

## How can I send messages directly from the messaging app?

It is not possible with the stock messaging app.
However, try [SMSdroid](https://play.google.com/store/apps/details?id=de.ub0r.android.smsdroid), which is build for this.

## How can I be sure and check, that the SMS is not sent via GSM and is not billed by my provider?

Just turn of the "SMS" Connector. Any other connector will use a webservice to send the messages.

## What have I to put in the field "Default recipient"?

If you do not want to start WebSMS with a fixed recipient, you should leave it blank.

## I've send the sms to the recipient, but the recipient don't receive my sms..why.?

You should not use a connector sending messages without YOUR sender id.
Some connectors do support sending with and without sender id.
Without sender id might be cheaper. But a direct reply is not possible.

## I have issues with/feature requests for any of the apps!

Please use the [issues](https://github.com/felixb/websms/issues) to report issues or feature requests.